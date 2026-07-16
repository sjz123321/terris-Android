package com.terris.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrisGameTest {
    @Test
    fun startsWithEmptyBoardAndLevelOne() {
        val game = TetrisGame()

        assertEquals(0, game.score)
        assertEquals(0, game.lines)
        assertEquals(1, game.level)
        assertFalse(game.isGameOver)
        assertTrue(game.board.all { row -> row.all { it == 0 } })
    }

    @Test
    fun horizontalMovementStopsAtBoardEdge() {
        val game = TetrisGame()

        repeat(20) { game.moveLeft() }
        val leftX = game.activeX
        repeat(20) { game.moveRight() }
        val rightX = game.activeX

        assertTrue(leftX >= 0)
        assertTrue(rightX < TetrisGame.BOARD_WIDTH)
    }

    @Test
    fun hardDropLocksCurrentPieceAndContinuesGame() {
        val game = TetrisGame()
        val firstType = game.activeType

        game.hardDrop()

        assertTrue(game.board.any { row -> row.any { it == firstType + 1 } })
        assertFalse(game.isPaused)
    }

    @Test
    fun pausePreventsMovementUntilResumed() {
        val game = TetrisGame()
        val originalX = game.activeX

        game.togglePause()
        game.moveLeft()
        assertEquals(originalX, game.activeX)

        game.togglePause()
        game.moveLeft()
        assertEquals(originalX - 1, game.activeX)
    }

    @Test
    fun tPieceSecondRotationPointsDown() {
        val game = TetrisGame()
        // Force active piece to T (type index 2) via reflection of private state.
        val typeField = TetrisGame::class.java.getDeclaredField("activeType")
        typeField.isAccessible = true
        typeField.setInt(game, 2)
        val rotField = TetrisGame::class.java.getDeclaredField("activeRotation")
        rotField.isAccessible = true
        rotField.setInt(game, 0)
        val xField = TetrisGame::class.java.getDeclaredField("activeX")
        xField.isAccessible = true
        xField.setInt(game, 3)
        val yField = TetrisGame::class.java.getDeclaredField("activeY")
        yField.isAccessible = true
        yField.setInt(game, 2)

        fun stemDirection(cells: IntArray): Int {
            val ys = listOf(cells[1], cells[3], cells[5], cells[7])
            val minY = ys.minOrNull()!!
            val maxY = ys.maxOrNull()!!
            return when {
                ys.count { it == minY } == 1 && ys.count { it == maxY } == 3 -> -1
                ys.count { it == minY } == 3 && ys.count { it == maxY } == 1 -> 1
                else -> 0
            }
        }

        assertEquals(-1, stemDirection(game.currentCells()))
        assertTrue(game.rotate())
        assertTrue(game.rotate())
        assertEquals(2, game.activeRotation)
        assertEquals(1, stemDirection(game.currentCells()))
    }
}
