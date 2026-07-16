package com.terris.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GameView(context: Context) : View(context) {
    private data class Geometry(
        val boardLeft: Float,
        val boardTop: Float,
        val cell: Float,
        val boardWidth: Float,
        val boardHeight: Float,
        val sideLeft: Float,
        val sideRight: Float,
        val controls: Array<RectF>,
        val pauseButton: RectF
    )

    private val game = TetrisGame()
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (!game.isPaused && !game.isGameOver) {
                game.tick()
                invalidate()
            }
            handler.postDelayed(this, if (game.isPaused || game.isGameOver) 240L else game.dropDelayMillis())
        }
    }

    private val density = resources.displayMetrics.density
    private val background = Color.rgb(16, 19, 24)
    private val surface = Color.rgb(23, 27, 34)
    private val surfaceLighter = Color.rgb(32, 38, 49)
    private val lineColor = Color.rgb(48, 56, 70)
    private val primaryText = Color.rgb(244, 247, 251)
    private val secondaryText = Color.rgb(143, 154, 172)
    private val accent = Color.rgb(100, 216, 203)
    private val pieceColors = intArrayOf(
        Color.rgb(84, 216, 230),
        Color.rgb(247, 201, 90),
        Color.rgb(192, 139, 255),
        Color.rgb(97, 133, 255),
        Color.rgb(255, 159, 90),
        Color.rgb(105, 217, 138),
        Color.rgb(242, 107, 122)
    )

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        color = primaryText
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        color = secondaryText
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        color = primaryText
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }

    private var downX = 0f
    private var downY = 0f

    init {
        isFocusable = true
        setBackgroundColor(background)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    fun pauseForLifecycle() {
        game.pauseForLifecycle()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val geometry = geometry()
        canvas.drawColor(background)
        drawTopBar(canvas, geometry)
        drawSidePanel(canvas, geometry)
        drawBoard(canvas, geometry)
        drawControls(canvas, geometry)
        if (game.isPaused || game.isGameOver) drawOverlay(canvas, geometry)
    }

    private fun geometry(): Geometry {
        val portrait = height > width
        val topBarHeight = dp(if (portrait) 64f else 58f)
        val controlHeight = dp(if (portrait) 136f else 96f)
        val controlTop = height - controlHeight
        val boardTop = topBarHeight + dp(8f)
        val maxBoardWidth = width * if (portrait) 0.62f else 0.52f
        val availableBoardHeight = (controlTop - boardTop - dp(12f)).coerceAtLeast(dp(80f))
        val cell = min(maxBoardWidth / TetrisGame.BOARD_WIDTH, availableBoardHeight / TetrisGame.BOARD_HEIGHT)
            .coerceAtLeast(dp(8f))
        val boardWidth = cell * TetrisGame.BOARD_WIDTH
        val boardHeight = cell * TetrisGame.BOARD_HEIGHT
        val boardLeft = if (portrait) width * 0.08f else width * 0.10f
        val sideLeft = boardLeft + boardWidth + dp(18f)
        val sideRight = (width - dp(14f)).coerceAtLeast(sideLeft + dp(36f))

        val gap = dp(8f)
        val maxButtonSize = dp(58f)
        val availableWidth = width - dp(24f)
        val buttonSize = min(maxButtonSize, (availableWidth - gap * 4f) / 5f)
        val totalWidth = buttonSize * 5f + gap * 4f
        val firstButton = (width - totalWidth) / 2f
        val buttonTop = controlTop + (controlHeight - buttonSize) / 2f
        val controls = Array(5) { index ->
            val left = firstButton + index * (buttonSize + gap)
            RectF(left, buttonTop, left + buttonSize, buttonTop + buttonSize)
        }
        val pauseButton = RectF(width - dp(52f), dp(12f), width - dp(14f), dp(48f))

        return Geometry(
            boardLeft = boardLeft,
            boardTop = boardTop,
            cell = cell,
            boardWidth = boardWidth,
            boardHeight = boardHeight,
            sideLeft = sideLeft,
            sideRight = sideRight,
            controls = controls,
            pauseButton = pauseButton
        )
    }

    private fun drawTopBar(canvas: Canvas, geometry: Geometry) {
        titlePaint.textSize = dp(24f)
        titlePaint.color = primaryText
        canvas.drawText("TERRIS", dp(16f), dp(36f), titlePaint)

        val portrait = height > width
        if (portrait) {
            drawTopMetric(canvas, "SCORE", game.score.toString(), width * 0.42f, dp(18f))
            drawTopMetric(canvas, "LV", game.level.toString(), width * 0.69f, dp(18f))
            drawTopMetric(canvas, "LINES", game.lines.toString(), width * 0.81f, dp(18f))
        } else {
            drawTopMetric(canvas, "SCORE", game.score.toString(), width * 0.40f, dp(16f))
            drawTopMetric(canvas, "LEVEL", game.level.toString(), width * 0.60f, dp(16f))
            drawTopMetric(canvas, "LINES", game.lines.toString(), width * 0.75f, dp(16f))
        }

        fillPaint.color = surfaceLighter
        canvas.drawRoundRect(geometry.pauseButton, dp(10f), dp(10f), fillPaint)
        strokePaint.color = lineColor
        strokePaint.strokeWidth = dp(1f)
        canvas.drawRoundRect(geometry.pauseButton, dp(10f), dp(10f), strokePaint)
        drawPauseIcon(canvas, geometry.pauseButton, game.isPaused)
    }

    private fun drawTopMetric(canvas: Canvas, label: String, value: String, x: Float, y: Float) {
        labelPaint.textSize = dp(8f)
        labelPaint.color = secondaryText
        canvas.drawText(label, x, y, labelPaint)
        valuePaint.textSize = dp(14f)
        valuePaint.color = primaryText
        canvas.drawText(value, x, y + dp(18f), valuePaint)
    }

    private fun drawSidePanel(canvas: Canvas, geometry: Geometry) {
        val panelWidth = geometry.sideRight - geometry.sideLeft
        if (panelWidth < dp(44f)) return

        val nextBottom = geometry.boardTop + dp(if (height > width) 118f else 136f)
        val nextPanel = RectF(geometry.sideLeft, geometry.boardTop, geometry.sideRight, nextBottom)
        fillPaint.color = surface
        canvas.drawRoundRect(nextPanel, dp(8f), dp(8f), fillPaint)
        strokePaint.color = lineColor
        strokePaint.strokeWidth = dp(1f)
        canvas.drawRoundRect(nextPanel, dp(8f), dp(8f), strokePaint)

        labelPaint.textSize = dp(9f)
        labelPaint.color = secondaryText
        canvas.drawText("NEXT", nextPanel.left + dp(10f), nextPanel.top + dp(20f), labelPaint)
        drawNextPiece(canvas, nextPanel)

        val metricTop = nextPanel.bottom + dp(18f)
        drawSideMetric(canvas, "SCORE", game.score.toString(), metricTop, geometry)
        drawSideMetric(canvas, "LEVEL", game.level.toString(), metricTop + dp(60f), geometry)
        drawSideMetric(canvas, "LINES", game.lines.toString(), metricTop + dp(120f), geometry)
    }

    private fun drawSideMetric(canvas: Canvas, label: String, value: String, top: Float, geometry: Geometry) {
        labelPaint.textSize = dp(8f)
        labelPaint.color = secondaryText
        canvas.drawText(label, geometry.sideLeft, top, labelPaint)
        valuePaint.textSize = dp(20f)
        valuePaint.color = primaryText
        canvas.drawText(value, geometry.sideLeft, top + dp(23f), valuePaint)
    }

    private fun drawNextPiece(canvas: Canvas, panel: RectF) {
        val cells = game.nextCells()
        val previewCell = min(dp(16f), (panel.width() - dp(20f)) / 4f)
        var minX = 4
        var maxX = 0
        var minY = 4
        var maxY = 0
        var index = 0
        while (index < cells.size) {
            minX = min(minX, cells[index])
            maxX = max(maxX, cells[index])
            minY = min(minY, cells[index + 1])
            maxY = max(maxY, cells[index + 1])
            index += 2
        }
        val pieceWidth = (maxX - minX + 1) * previewCell
        val pieceHeight = (maxY - minY + 1) * previewCell
        val startX = panel.centerX() - pieceWidth / 2f
        val startY = panel.centerY() + dp(8f) - pieceHeight / 2f
        index = 0
        while (index < cells.size) {
            val x = startX + (cells[index] - minX) * previewCell
            val y = startY + (cells[index + 1] - minY) * previewCell
            drawBlock(canvas, x, y, previewCell, game.nextPieceType + 1)
            index += 2
        }
    }

    private fun drawBoard(canvas: Canvas, geometry: Geometry) {
        val boardRect = RectF(
            geometry.boardLeft,
            geometry.boardTop,
            geometry.boardLeft + geometry.boardWidth,
            geometry.boardTop + geometry.boardHeight
        )
        fillPaint.color = surface
        canvas.drawRoundRect(boardRect, dp(8f), dp(8f), fillPaint)
        strokePaint.color = lineColor
        strokePaint.strokeWidth = dp(1f)
        canvas.drawRoundRect(boardRect, dp(8f), dp(8f), strokePaint)

        strokePaint.color = Color.argb(100, 48, 56, 70)
        strokePaint.strokeWidth = dp(0.7f)
        for (column in 1 until TetrisGame.BOARD_WIDTH) {
            val x = geometry.boardLeft + column * geometry.cell
            canvas.drawLine(x, geometry.boardTop, x, geometry.boardTop + geometry.boardHeight, strokePaint)
        }
        for (row in 1 until TetrisGame.BOARD_HEIGHT) {
            val y = geometry.boardTop + row * geometry.cell
            canvas.drawLine(geometry.boardLeft, y, geometry.boardLeft + geometry.boardWidth, y, strokePaint)
        }

        for (row in 0 until TetrisGame.BOARD_HEIGHT) {
            for (column in 0 until TetrisGame.BOARD_WIDTH) {
                val colorIndex = game.board[row][column]
                if (colorIndex != 0) {
                    drawBlock(
                        canvas,
                        geometry.boardLeft + column * geometry.cell,
                        geometry.boardTop + row * geometry.cell,
                        geometry.cell,
                        colorIndex
                    )
                }
            }
        }

        if (!game.isGameOver) {
            val ghostY = game.ghostY()
            drawGhost(canvas, geometry, ghostY)
            drawActive(canvas, geometry)
        }
    }

    private fun drawGhost(canvas: Canvas, geometry: Geometry, ghostY: Int) {
        val cells = game.currentCells()
        var index = 0
        while (index < cells.size) {
            val cellX = game.activeX + cells[index]
            val cellY = ghostY + cells[index + 1]
            if (cellY >= 0) {
                val left = geometry.boardLeft + cellX * geometry.cell + geometry.cell * 0.16f
                val top = geometry.boardTop + cellY * geometry.cell + geometry.cell * 0.16f
                val right = geometry.boardLeft + (cellX + 1) * geometry.cell - geometry.cell * 0.16f
                val bottom = geometry.boardTop + (cellY + 1) * geometry.cell - geometry.cell * 0.16f
                linePaint.color = Color.argb(115, Color.red(pieceColors[game.activeType]), Color.green(pieceColors[game.activeType]), Color.blue(pieceColors[game.activeType]))
                linePaint.strokeWidth = max(dp(1f), geometry.cell * 0.08f)
                canvas.drawRoundRect(RectF(left, top, right, bottom), geometry.cell * 0.12f, geometry.cell * 0.12f, linePaint)
            }
            index += 2
        }
    }

    private fun drawActive(canvas: Canvas, geometry: Geometry) {
        val cells = game.currentCells()
        var index = 0
        while (index < cells.size) {
            val cellX = game.activeX + cells[index]
            val cellY = game.activeY + cells[index + 1]
            if (cellY >= 0) {
                drawBlock(
                    canvas,
                    geometry.boardLeft + cellX * geometry.cell,
                    geometry.boardTop + cellY * geometry.cell,
                    geometry.cell,
                    game.activeType + 1
                )
            }
            index += 2
        }
    }

    private fun drawBlock(canvas: Canvas, left: Float, top: Float, size: Float, colorIndex: Int) {
        val inset = size * 0.08f
        val rect = RectF(left + inset, top + inset, left + size - inset, top + size - inset)
        val color = pieceColors[(colorIndex - 1).coerceIn(0, pieceColors.lastIndex)]
        fillPaint.color = color
        fillPaint.alpha = 255
        canvas.drawRoundRect(rect, size * 0.14f, size * 0.14f, fillPaint)
        linePaint.color = Color.argb(90, 255, 255, 255)
        linePaint.strokeWidth = max(dp(1f), size * 0.045f)
        canvas.drawLine(rect.left + size * 0.10f, rect.top + size * 0.12f, rect.right - size * 0.12f, rect.top + size * 0.12f, linePaint)
        canvas.drawLine(rect.left + size * 0.12f, rect.top + size * 0.10f, rect.left + size * 0.12f, rect.bottom - size * 0.12f, linePaint)
    }

    private fun drawControls(canvas: Canvas, geometry: Geometry) {
        geometry.controls.forEach { button ->
            fillPaint.color = surface
            canvas.drawRoundRect(button, dp(14f), dp(14f), fillPaint)
            strokePaint.color = lineColor
            strokePaint.strokeWidth = dp(1f)
            canvas.drawRoundRect(button, dp(14f), dp(14f), strokePaint)
        }
        drawArrow(canvas, geometry.controls[0], Direction.LEFT)
        drawArrow(canvas, geometry.controls[1], Direction.DOWN)
        drawHardDrop(canvas, geometry.controls[2])
        drawArrow(canvas, geometry.controls[3], Direction.RIGHT)
        drawRotate(canvas, geometry.controls[4])
    }

    private enum class Direction { LEFT, DOWN, RIGHT }

    private fun drawArrow(canvas: Canvas, button: RectF, direction: Direction) {
        val centerX = button.centerX()
        val centerY = button.centerY()
        val distance = button.width() * 0.19f
        val head = button.width() * 0.11f
        linePaint.color = primaryText
        linePaint.strokeWidth = dp(2.5f)
        val path = Path()
        when (direction) {
            Direction.LEFT -> {
                path.moveTo(centerX + distance, centerY)
                path.lineTo(centerX - distance, centerY)
                path.moveTo(centerX - distance, centerY)
                path.lineTo(centerX - distance + head, centerY - head)
                path.moveTo(centerX - distance, centerY)
                path.lineTo(centerX - distance + head, centerY + head)
            }
            Direction.RIGHT -> {
                path.moveTo(centerX - distance, centerY)
                path.lineTo(centerX + distance, centerY)
                path.moveTo(centerX + distance, centerY)
                path.lineTo(centerX + distance - head, centerY - head)
                path.moveTo(centerX + distance, centerY)
                path.lineTo(centerX + distance - head, centerY + head)
            }
            Direction.DOWN -> {
                path.moveTo(centerX, centerY - distance)
                path.lineTo(centerX, centerY + distance)
                path.moveTo(centerX, centerY + distance)
                path.lineTo(centerX - head, centerY + distance - head)
                path.moveTo(centerX, centerY + distance)
                path.lineTo(centerX + head, centerY + distance - head)
            }
        }
        canvas.drawPath(path, linePaint)
    }

    private fun drawHardDrop(canvas: Canvas, button: RectF) {
        drawArrow(canvas, button, Direction.DOWN)
        linePaint.strokeWidth = dp(2f)
        val y = button.centerY() + button.height() * 0.29f
        canvas.drawLine(button.left + button.width() * 0.30f, y, button.right - button.width() * 0.30f, y, linePaint)
    }

    private fun drawRotate(canvas: Canvas, button: RectF) {
        val inset = button.width() * 0.27f
        val arc = RectF(button.left + inset, button.top + inset, button.right - inset, button.bottom - inset)
        linePaint.color = primaryText
        linePaint.strokeWidth = dp(2.4f)
        canvas.drawArc(arc, 35f, 275f, false, linePaint)
        val tipX = arc.right - button.width() * 0.01f
        val tipY = arc.top + button.height() * 0.16f
        val path = Path()
        path.moveTo(tipX, tipY)
        path.lineTo(tipX - button.width() * 0.16f, tipY - button.height() * 0.02f)
        path.lineTo(tipX - button.width() * 0.03f, tipY + button.height() * 0.14f)
        canvas.drawPath(path, linePaint)
    }

    private fun drawPauseIcon(canvas: Canvas, button: RectF, paused: Boolean) {
        linePaint.color = primaryText
        linePaint.strokeWidth = dp(2.5f)
        if (paused) {
            val path = Path()
            path.moveTo(button.centerX() - dp(5f), button.centerY() - dp(8f))
            path.lineTo(button.centerX() + dp(7f), button.centerY())
            path.lineTo(button.centerX() - dp(5f), button.centerY() + dp(8f))
            path.close()
            fillPaint.color = primaryText
            canvas.drawPath(path, fillPaint)
        } else {
            canvas.drawLine(button.centerX() - dp(5f), button.centerY() - dp(7f), button.centerX() - dp(5f), button.centerY() + dp(7f), linePaint)
            canvas.drawLine(button.centerX() + dp(5f), button.centerY() - dp(7f), button.centerX() + dp(5f), button.centerY() + dp(7f), linePaint)
        }
    }

    private fun drawOverlay(canvas: Canvas, geometry: Geometry) {
        fillPaint.color = Color.argb(170, 16, 19, 24)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

        val centerX = geometry.boardLeft + geometry.boardWidth / 2f
        val centerY = geometry.boardTop + geometry.boardHeight / 2f
        val panelWidth = min(geometry.boardWidth + dp(30f), width - dp(36f))
        val panel = RectF(centerX - panelWidth / 2f, centerY - dp(70f), centerX + panelWidth / 2f, centerY + dp(70f))
        fillPaint.color = surfaceLighter
        canvas.drawRoundRect(panel, dp(12f), dp(12f), fillPaint)
        strokePaint.color = lineColor
        strokePaint.strokeWidth = dp(1f)
        canvas.drawRoundRect(panel, dp(12f), dp(12f), strokePaint)

        titlePaint.textSize = dp(22f)
        titlePaint.color = if (game.isGameOver) primaryText else accent
        val title = if (game.isGameOver) "GAME OVER" else "PAUSED"
        drawCenteredText(canvas, title, centerX, centerY - dp(15f), titlePaint)
        labelPaint.textSize = dp(10f)
        labelPaint.color = secondaryText
        val subtitle = if (game.isGameOver) "TAP TO RESTART" else "USE PAUSE TO RESUME"
        drawCenteredText(canvas, subtitle, centerX, centerY + dp(18f), labelPaint)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baseline: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, centerX, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val geometry = geometry()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                if (game.isGameOver) {
                    game.reset()
                    invalidate()
                    return true
                }
                if (geometry.pauseButton.contains(event.x, event.y)) {
                    game.togglePause()
                    invalidate()
                    return true
                }
                if (game.isPaused) return true

                val buttonIndex = geometry.controls.indexOfFirst { it.contains(event.x, event.y) }
                if (buttonIndex >= 0) {
                    when (buttonIndex) {
                        0 -> game.moveLeft()
                        1 -> game.softDrop()
                        2 -> game.hardDrop()
                        3 -> game.moveRight()
                        4 -> game.rotate()
                    }
                    invalidate()
                    return true
                }

                val dx = event.x - downX
                val dy = event.y - downY
                if (abs(dx) > abs(dy) && abs(dx) > dp(18f)) {
                    if (dx < 0) game.moveLeft() else game.moveRight()
                } else if (dy > dp(24f)) {
                    game.hardDrop()
                } else {
                    game.rotate()
                }
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (game.isGameOver) {
            game.reset()
            invalidate()
            return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> game.moveLeft()
            KeyEvent.KEYCODE_DPAD_RIGHT -> game.moveRight()
            KeyEvent.KEYCODE_DPAD_DOWN -> game.softDrop()
            KeyEvent.KEYCODE_DPAD_UP -> game.rotate()
            KeyEvent.KEYCODE_SPACE -> game.hardDrop()
            KeyEvent.KEYCODE_P -> game.togglePause()
            else -> return super.onKeyDown(keyCode, event)
        }
        invalidate()
        return true
    }

    private fun dp(value: Float): Float = value * density
}
