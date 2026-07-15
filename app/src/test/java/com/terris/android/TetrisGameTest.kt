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
}
