package com.example.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.math.pow

class GameFieldTest {

    @Test
    fun testWriteAndReadGameField() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileName = "gameFieldTest.txt"
        val gameField = GameField(5)

        gameField.setPoint(0, 0, 'A')
        gameField.setPoint(1, 1, 'B')
        gameField.setPoint(2, 2, 'C')

        gameField.writeToFile(context, fileName)

        val newGameField = GameField(5)
        newGameField.readFromFile(context, fileName)

        assertEquals('A', newGameField.getPoint(0, 0))
        assertEquals('B', newGameField.getPoint(1, 1))
        assertEquals('C', newGameField.getPoint(2, 2))

        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    @Test
    fun testMoveBall() {
        val gameField = GameField(9)

        // Filling a field to create almost a vertical line
        gameField.setPoint(0, 0, 'X')
        gameField.setPoint(1, 0, 'X')
        gameField.setPoint(2, 0, 'X')
        gameField.setPoint(3, 0, 'X')
        gameField.setPoint(5, 0, 'X')
        // Trying to move the ball to an already occupied space
        val invalidScore = gameField.moveBall(0, 0, 5, 0)
        assertEquals(-1, invalidScore)

        // Move the ball, which should complete the line of length 5 and remove it
        var score = gameField.moveBall(5, 0, 4, 0)

        // Check results
        assertEquals(10 * 2.0.pow(0).toInt(), score) // 10 * 2^(5-5) = 10
        assertEquals('0', gameField.getPoint(0, 0))
        assertEquals('0', gameField.getPoint(1, 0))
        assertEquals('0', gameField.getPoint(2, 0))
        assertEquals('0', gameField.getPoint(3, 0))
        assertEquals('0', gameField.getPoint(4, 0))
        // Check if can move ball on diagonals
        gameField.setPoint(2, 5, 'X')
        gameField.setPoint(1, 0, 'X')
        gameField.setPoint(0, 1, 'X')
        score = gameField.moveBall(2, 5, 0, 0)
        assertEquals(-1, score)
    }

}
