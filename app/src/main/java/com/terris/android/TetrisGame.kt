package com.terris.android

import java.util.Random

/** Rules and state for a small, dependency-free Tetris engine. */
class TetrisGame {
    companion object {
        const val BOARD_WIDTH = 10
        const val BOARD_HEIGHT = 20
        const val PIECE_COUNT = 7
    }

    data class StepResult(
        val pieceLocked: Boolean = false,
        val clearedRows: List<Int> = emptyList()
    )

    val board: Array<IntArray> = Array(BOARD_HEIGHT) { IntArray(BOARD_WIDTH) }

    var activeType: Int = 0
        private set
    var activeRotation: Int = 0
        private set
    var activeX: Int = 0
        private set
    var activeY: Int = 0
        private set
    var nextPieceType: Int = 0
        private set
    var score: Int = 0
        private set
    var lines: Int = 0
        private set
    var level: Int = 1
        private set
    var isPaused: Boolean = false
        private set
    var isGameOver: Boolean = false
        private set
    var isClearing: Boolean = false
        private set
    var clearingRows: List<Int> = emptyList()
        private set

    private val random = Random()
    private val bag = mutableListOf<Int>()

    init {
        reset()
    }

    fun reset() {
        board.forEach { row -> row.fill(0) }
        score = 0
        lines = 0
        level = 1
        isPaused = false
        isGameOver = false
        isClearing = false
        clearingRows = emptyList()
        bag.clear()
        activeType = drawPiece()
        nextPieceType = drawPiece()
        activeRotation = 0
        activeX = spawnX(activeType)
        activeY = -1
    }

    fun togglePause() {
        if (!isGameOver && !isClearing) isPaused = !isPaused
    }

    fun pauseForLifecycle() {
        if (!isGameOver) isPaused = true
    }

    fun moveLeft(): Boolean = moveHorizontal(-1)

    fun moveRight(): Boolean = moveHorizontal(1)

    fun softDrop(): StepResult {
        if (!canPlay()) return StepResult()
        return if (canPlace(activeType, activeRotation, activeX, activeY + 1)) {
            activeY += 1
            score += 1
            StepResult()
        } else {
            lockPiece()
        }
    }

    fun hardDrop(): StepResult {
        if (!canPlay()) return StepResult()
        var distance = 0
        while (canPlace(activeType, activeRotation, activeX, activeY + 1)) {
            activeY += 1
            distance += 1
        }
        score += distance * 2
        return lockPiece()
    }

    fun rotate(): Boolean {
        if (!canPlay()) return false
        val candidate = (activeRotation + 1) % 4
        val kicks = intArrayOf(0, -1, 1, -2, 2)
        for (kick in kicks) {
            if (canPlace(activeType, candidate, activeX + kick, activeY)) {
                activeX += kick
                activeRotation = candidate
                return true
            }
        }
        return false
    }

    /** Advances the automatic gravity step. */
    fun tick(): StepResult {
        if (!canPlay()) return StepResult()
        return if (!canPlace(activeType, activeRotation, activeX, activeY + 1)) {
            lockPiece()
        } else {
            activeY += 1
            StepResult()
        }
    }

    fun completeLineClear() {
        if (!isClearing) return
        val cleared = clearingRows.size
        collapseClearingRows()
        lines += cleared
        level = lines / 10 + 1
        score += when (cleared) {
            1 -> 100 * level
            2 -> 300 * level
            3 -> 500 * level
            else -> 800 * level
        }
        isClearing = false
        clearingRows = emptyList()
        spawnNextPiece()
    }

    fun ghostY(): Int {
        var ghost = activeY
        while (canPlace(activeType, activeRotation, activeX, ghost + 1)) {
            ghost += 1
        }
        return ghost
    }

    fun currentCells(): IntArray = cellsFor(activeType, activeRotation)

    fun nextCells(): IntArray = cellsFor(nextPieceType, 0)

    fun dropDelayMillis(): Long = (850L - ((level - 1) * 65L)).coerceAtLeast(90L)

    private fun moveHorizontal(delta: Int): Boolean {
        if (!canPlay()) return false
        if (canPlace(activeType, activeRotation, activeX + delta, activeY)) {
            activeX += delta
            return true
        }
        return false
    }

    private fun canPlay(): Boolean = !isPaused && !isGameOver && !isClearing

    private fun canPlace(type: Int, rotation: Int, x: Int, y: Int): Boolean {
        val cells = cellsFor(type, rotation)
        var index = 0
        while (index < cells.size) {
            val boardX = x + cells[index]
            val boardY = y + cells[index + 1]
            if (boardX !in 0 until BOARD_WIDTH || boardY >= BOARD_HEIGHT) return false
            if (boardY >= 0 && board[boardY][boardX] != 0) return false
            index += 2
        }
        return true
    }

    private fun lockPiece(): StepResult {
        val cells = currentCells()
        var index = 0
        while (index < cells.size) {
            val boardX = activeX + cells[index]
            val boardY = activeY + cells[index + 1]
            if (boardY < 0) {
                isGameOver = true
            } else {
                board[boardY][boardX] = activeType + 1
            }
            index += 2
        }

        if (isGameOver) return StepResult(pieceLocked = true)

        val fullRows = findFullRows()
        if (fullRows.isNotEmpty()) {
            isClearing = true
            clearingRows = fullRows
            return StepResult(pieceLocked = true, clearedRows = fullRows)
        }

        spawnNextPiece()
        return StepResult(pieceLocked = true)
    }

    private fun findFullRows(): List<Int> {
        val rows = mutableListOf<Int>()
        for (row in 0 until BOARD_HEIGHT) {
            if (board[row].all { it != 0 }) rows.add(row)
        }
        return rows
    }

    private fun collapseClearingRows() {
        val full = clearingRows.toSet()
        var writeRow = BOARD_HEIGHT - 1
        for (readRow in BOARD_HEIGHT - 1 downTo 0) {
            if (readRow in full) continue
            if (writeRow != readRow) board[writeRow] = board[readRow].copyOf()
            writeRow -= 1
        }
        while (writeRow >= 0) {
            board[writeRow] = IntArray(BOARD_WIDTH)
            writeRow -= 1
        }
    }

    private fun spawnNextPiece() {
        activeType = nextPieceType
        nextPieceType = drawPiece()
        activeRotation = 0
        activeX = spawnX(activeType)
        activeY = -1
        if (!canPlace(activeType, activeRotation, activeX, activeY)) {
            isGameOver = true
        }
    }

    private fun spawnX(type: Int): Int = when (type) {
        0 -> 3
        1 -> 3
        else -> 3
    }

    private fun drawPiece(): Int {
        if (bag.isEmpty()) refillBag()
        return bag.removeAt(bag.lastIndex)
    }

    private fun refillBag() {
        bag.clear()
        for (piece in 0 until PIECE_COUNT) bag.add(piece)
        for (index in bag.lastIndex downTo 1) {
            val swapIndex = random.nextInt(index + 1)
            val value = bag[index]
            bag[index] = bag[swapIndex]
            bag[swapIndex] = value
        }
    }

    private fun cellsFor(type: Int, rotation: Int): IntArray = Shapes.PIECES[type][rotation % 4]

    private object Shapes {
        // Each pair in a rotation is an x/y coordinate inside a 4x4 piece box.
        val PIECES = arrayOf(
            arrayOf(
                intArrayOf(0, 1, 1, 1, 2, 1, 3, 1),
                intArrayOf(2, 0, 2, 1, 2, 2, 2, 3),
                intArrayOf(0, 2, 1, 2, 2, 2, 3, 2),
                intArrayOf(1, 0, 1, 1, 1, 2, 1, 3)
            ),
            arrayOf(
                intArrayOf(1, 0, 2, 0, 1, 1, 2, 1),
                intArrayOf(1, 0, 2, 0, 1, 1, 2, 1),
                intArrayOf(1, 0, 2, 0, 1, 1, 2, 1),
                intArrayOf(1, 0, 2, 0, 1, 1, 2, 1)
            ),
            arrayOf(
                intArrayOf(1, 0, 0, 1, 1, 1, 2, 1),
                intArrayOf(1, 0, 1, 1, 2, 1, 1, 2),
                intArrayOf(0, 1, 1, 1, 2, 1, 1, 2),
                intArrayOf(1, 0, 0, 1, 1, 1, 1, 2)
            ),
            arrayOf(
                intArrayOf(0, 0, 0, 1, 1, 1, 2, 1),
                intArrayOf(1, 0, 2, 0, 1, 1, 1, 2),
                intArrayOf(0, 1, 1, 1, 2, 1, 2, 2),
                intArrayOf(1, 0, 1, 1, 0, 2, 1, 2)
            ),
            arrayOf(
                intArrayOf(2, 0, 0, 1, 1, 1, 2, 1),
                intArrayOf(1, 0, 1, 1, 1, 2, 2, 2),
                intArrayOf(0, 1, 1, 1, 2, 1, 0, 2),
                intArrayOf(0, 0, 1, 0, 1, 1, 1, 2)
            ),
            arrayOf(
                intArrayOf(1, 0, 2, 0, 0, 1, 1, 1),
                intArrayOf(1, 0, 1, 1, 2, 1, 2, 2),
                intArrayOf(1, 1, 2, 1, 0, 2, 1, 2),
                intArrayOf(0, 0, 0, 1, 1, 1, 1, 2)
            ),
            arrayOf(
                intArrayOf(0, 0, 1, 0, 1, 1, 2, 1),
                intArrayOf(2, 0, 1, 1, 2, 1, 1, 2),
                intArrayOf(0, 1, 1, 1, 1, 2, 2, 2),
                intArrayOf(1, 0, 0, 1, 1, 1, 0, 2)
            )
        )
    }
}
