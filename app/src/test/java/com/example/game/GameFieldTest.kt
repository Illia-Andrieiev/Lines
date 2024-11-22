package com.example.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameFieldTest {

    private lateinit var gameField: GameField

    @Before
    fun setUp() {
        gameField = GameField(9)
    }

    @Test
    fun testSetPointAndCheckScore_AddsScoreForNewLine() {
        gameField.setPoint(0, 0, 'r')
        gameField.setPoint(0, 1, 'r')
        gameField.setPoint(0, 2, 'r')
        gameField.setPoint(0, 3, 'r')

        val score = gameField.setPointAndCheckScore(0, 4, 'r')

        assertEquals(10, score)
        assertEquals(10, gameField.score)
    }

    @Test
    fun testSetPointAndCheckScore_NoScoreForSingleBall() {
        val score = gameField.setPointAndCheckScore(4, 4, 'b')

        assertEquals(0, score)
        assertEquals(0, gameField.score)
    }

    @Test
    fun testSetPointAndCheckScore_AddsScoreForDiagonalLine() {
        gameField.setPoint(0, 0, 'g')
        gameField.setPoint(1, 1, 'g')
        gameField.setPoint(2, 2, 'g')
        gameField.setPoint(3, 3, 'g')

        val score = gameField.setPointAndCheckScore(4, 4, 'g')

        // О10 * 2^0 = 10
        assertEquals(10, score)
        assertEquals(10, gameField.score)
    }

    @Test
    fun testSetPointAndCheckScore_AddsScoreForVerticalLine() {
        gameField.setPoint(0, 0, 'y')
        gameField.setPoint(1, 0, 'y')
        gameField.setPoint(2, 0, 'y')
        gameField.setPoint(3, 0, 'y')

        val score = gameField.setPointAndCheckScore(4, 0, 'y')

        // 0 * 2^0 = 10
        assertEquals(10, score)
        assertEquals(10, gameField.score)
    }

    @Test
    fun testSetPointAndCheckScore_MultipleLines() {
        gameField.setPoint(0, 0, 'm')
        gameField.setPoint(0, 1, 'm')
        gameField.setPoint(0, 2, 'm')
        gameField.setPoint(0, 3, 'm')
        gameField.setPoint(1, 0, 'm')
        gameField.setPoint(2, 0, 'm')
        gameField.setPoint(3, 0, 'm')

        val score1 = gameField.setPointAndCheckScore(0, 4, 'm')
        val score2 = gameField.setPointAndCheckScore(4, 0, 'm')

        assertEquals(10, score1)
        assertEquals(0, score2) // because ball(0,0) line must delete
        assertEquals(10, gameField.score)
    }
}
