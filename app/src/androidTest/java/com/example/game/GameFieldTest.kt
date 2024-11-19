package com.example.game

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.File

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
}
