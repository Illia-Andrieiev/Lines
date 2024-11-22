package com.example.game

import android.content.Context
import android.util.Log
import kotlin.math.pow
import kotlin.random.Random

data class Coordinates(val x: Int, val y: Int)
data class Ball(val x: Int, val y: Int, val color: Char)

class GameField(private val size: Int) {
    // Our game field represented as a 2D array of characters
    private var field: Array<CharArray> = Array(size) { CharArray(size) }
    // List of empty points on the field
    private var emptyPoints: MutableList<Coordinates> =
        MutableList(size * size) { index ->
            Coordinates(index % size, index / size)
        }
    var score = 0

    init {
        // Initialize the field with '0' to represent empty points
        for (i in 0 until size) {
            for (j in 0 until size) {
                field[i][j] = '0' // By default, all points are empty
            }
        }
    }

    // Get the number of empty points
    fun getAmountOfEmptyPoints(): Int {
        return emptyPoints.size
    }

    // Create a copy of the current GameField
    fun copy(): GameField {
        val newGameField = GameField(this.size)
        for (i in 0 until size) {
            for (j in 0 until size) {
                newGameField.field[i][j] = this.field[i][j]
            }
        }
        newGameField.emptyPoints.clear()
        newGameField.emptyPoints.addAll(this.emptyPoints)
        return newGameField
    }

    // Get the size of the field
    fun getSize(): Int {
        return size
    }

    // Return the color of the ball at the selected point
    fun getPoint(x: Int, y: Int): Char {
        if (x >= size || y >= size)
            return '0'
        return field[y][x]
    }

    // Change the state of the current point and manage emptyPoints list
    fun setPoint(x: Int, y: Int, color: Char) {
        val currentColor = field[y][x]
        val coordinates = Coordinates(y, x)

        // If the point was empty and now is not, remove it from emptyPoints
        if (currentColor == '0' && color != '0') {
            emptyPoints.remove(coordinates)
        }

        // If the point was not empty and now is, add it to emptyPoints
        if (currentColor != '0' && color == '0' && !emptyPoints.contains(coordinates)) {
            emptyPoints.add(coordinates)
        }

        field[y][x] = color
    }

    // Check if there is a path from one point to another using Depth-First Search (DFS)
    private fun isPathExist(fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        // Check out of bounds
        if (fromX < 0 || fromX >= size || fromY < 0 || fromY >= size ||
            toX < 0 || toX >= size || toY < 0 || toY >= size) {
            return false
        }

        // Ensure the end point is empty
        if (field[toY][toX] != '0') {
            return false
        }

        // Array to keep track of visited points
        val visited = Array(size) { BooleanArray(size) }

        // Support function for DFS
        fun dfs(x: Int, y: Int): Boolean {
            // If destination is reached
            if (x == toX && y == toY) {
                return true
            }

            // Mark current point as visited
            visited[y][x] = true

            // Possible directions of movement: up, down, left, right
            val directions = arrayOf(
                intArrayOf(-1, 0),
                intArrayOf(1, 0),
                intArrayOf(0, -1),
                intArrayOf(0, 1)
            )

            // Attempt to move in each direction
            for (dir in directions) {
                val newX = x + dir[1]
                val newY = y + dir[0]

                // Check out of bounds and if the point has been visited
                if (newX in 0 until size && newY in 0 until size &&
                    field[newY][newX] == '0' && !visited[newY][newX]) {
                    if (dfs(newX, newY)) {
                        return true
                    }
                }
            }
            return false
        }

        // Start DFS from the initial point
        return dfs(fromX, fromY)
    }

    // Move a ball if it exists and there is a path from one point to another
    fun moveBall(fromX: Int, fromY: Int, toX: Int, toY: Int): Int {
        if (fromX < 0 || fromX >= size || fromY < 0 || fromY >= size ||
            toX < 0 || toX >= size || toY < 0 || toY >= size) {
            return -1
        }

        if (field[fromY][fromX] != '0' && isPathExist(fromX, fromY, toX, toY)) {
            setPoint(toX, toY, field[fromY][fromX])
            setPoint(fromX, fromY, '0')
            val curScore = movementScore(toX, toY)
            score += curScore
            return curScore
        }
        return -1
    }

    // Calculate score based on the movement of a ball
    private fun movementScore(x: Int, y: Int): Int {
        val targetChar = field[y][x]
        if (targetChar == '0') return 0

        // Directions: horizontal, vertical and two diagonals
        val directions = arrayOf(
            intArrayOf(1, 0), intArrayOf(0, 1), // horizontal, vertical
            intArrayOf(1, 1), intArrayOf(1, -1) // two diagonals
        )

        var maxLineLen = 0
        var maxLineCoordinates: List<Coordinates> = emptyList()

        // Iterate over each direction
        for (dir in directions) {
            val lineCoordinates = mutableListOf<Coordinates>()
            lineCoordinates.add(Coordinates(x, y))

            // Check in one direction
            var i = 1
            while (true) {
                val newX = x + i * dir[0]
                val newY = y + i * dir[1]
                if (newX in 0 until size && newY in 0 until size && field[newY][newX] == targetChar) {
                    lineCoordinates.add(Coordinates(newX, newY))
                    i++
                } else {
                    break
                }
            }

            // Check in the opposite direction
            i = 1
            while (true) {
                val newX = x - i * dir[0]
                val newY = y - i * dir[1]
                if (newX in 0 until size && newY in 0 until size && field[newY][newX] == targetChar) {
                    lineCoordinates.add(Coordinates(newX, newY))
                    i++
                } else {
                    break
                }
            }

            // Update maximum line length and coordinates
            if (lineCoordinates.size > maxLineLen) {
                maxLineLen = lineCoordinates.size
                maxLineCoordinates = lineCoordinates
            }
        }

        // If a line of 5 or more is formed, clear the line and calculate score
        if (maxLineLen >= 5) {
            for (coordinate in maxLineCoordinates) {
                setPoint(coordinate.x, coordinate.y, '0')
            }
            return 10 * 2.0.pow((maxLineLen - 5)).toInt()
        }

        return 0
    }

    // Write the current state of the game to a file
    fun writeToFile(context: Context, fileName: String) {
        val fileWriter = FileWriter()
        val stringBuilder = StringBuilder()
        Log.d("writer", "$score")
        stringBuilder.append("$score")
        stringBuilder.append('|')
        for (y in 0 until size) {
            for (x in 0 until size) {
                stringBuilder.append("$x,$y,${field[y][x]}")
                if (x != size - 1 || y != size - 1) {
                    stringBuilder.append(";")
                }
            }
        }
        val data = stringBuilder.toString()
        fileWriter.writeToFile(context, fileName, data)
    }

    // Read the state of the game from a file
    fun readFromFile(context: Context, fileName: String) {
        val fileWriter = FileWriter()
        val data = fileWriter.readFromFile(context, fileName)
        if (data.isNotEmpty()) {
            val mainEntries = data.split("|")
            score = mainEntries[0].toInt()
            Log.d("reader", "$score")
            val entries = mainEntries[1].split(";")
            for (entry in entries) {
                val parts = entry.split(",")
                if (parts.size == 3) {
                    val x = parts[0].toInt()
                    val y = parts[1].toInt()
                    val value = parts[2].first()
                    if (x in 0 until size && y in 0 until size) {
                        setPoint(x,y,value)
                    }
                }
            }
        }
    }
    // Function to get a random color character for the ball
    private fun getRandomColor(): Char {
        val randomIndex = Random.nextInt(7)
        return when (randomIndex) {
            0 -> 'r'  // Red
            1 -> 'b'  // Black
            2 -> 'B'  // Blue
            3 -> 'g'  // Green
            4 -> 'y'  // Yellow
            5 -> 'c'  // Cyan
            6 -> 'm'  // Magenta
            else -> '0'  // Default to '0' if no match
        }
    }

    // Function to set the color of a point and check for score updates
    fun setPointAndCheckScore(x: Int, y: Int, color: Char): Int {
        setPoint(x, y, color)
        val moveScore = movementScore(x, y)
        if (moveScore > 0) {
            score += moveScore
        }
        return moveScore
    }

    // Function to get a random ball with coordinates and color. Do not remove coordinates from empty points
    fun getRandomBall(): Ball? {
        if (emptyPoints.isEmpty()) {
            return null
        }
        val randomIndex = Random.nextInt(emptyPoints.size)
        val newBallCoordinates = emptyPoints[randomIndex]
        return Ball(newBallCoordinates.y, newBallCoordinates.x, getRandomColor())
    }

    // Function to clear the game field and reset the score
    fun clear() {
        field = Array(size) { CharArray(size) }
        emptyPoints = MutableList(size * size) { index ->
            Coordinates(index % size, index / size)
        }
        score = 0
        for (i in 0 until size) {
            for (j in 0 until size) {
                field[i][j] = '0'  // By default, all points are empty
            }
        }
    }

}

