package com.example.game

import android.content.Context
import kotlin.math.pow
import kotlin.random.Random

data class Coordinates(val x:Int, val y:Int)
data class Ball(val x:Int, val y:Int, val color:Char)

class GameField(private val size: Int) {
    // our game field
    private var field: Array<CharArray> = Array(size) { CharArray(size) }
    private var emptyPoints: MutableList<Coordinates> =
        MutableList(size * size) { index ->
            Coordinates(index % size, index / size)
        }
    private var score = 0

    init {
        for (i in 0 until size) {
            for (j in 0 until size) {
                field[i][j] = '0' // by default all points are empty
            }
        }
    }

    fun getScore():Int{
        return score
    }
    fun getAmountOfEmptyPoints():Int{
        return emptyPoints.size
    }
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
    fun getSize():Int{
        return size
    }


    // return color of ball at selected point
    fun getPoint(x: Int, y: Int): Char {
        if(x >= size || y >= size)
            return '0'
        return field[y][x]
    }

    // change state of current point and manage emptyPoints
    fun setPoint(x: Int, y: Int, color: Char) {
        val currentColor = field[y][x]
        val coordinates = Coordinates(y, x)

        if (currentColor == '0' && color != '0') {
            emptyPoints.remove(coordinates)
        }

        if (currentColor != '0' && color == '0' && !emptyPoints.contains(coordinates)) {
            emptyPoints.add(coordinates)
        }

        field[y][x] = color
    }

    private fun isPathExist(fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        // check out of bounds
        if (fromX < 0 || fromX >= size || fromY < 0 || fromY >= size ||
            toX < 0 || toX >= size || toY < 0 || toY >= size) {
            return false
        }

        // is end point empty
        if (field[toY][toX] != '0') {
            return false
        }

        // visited points
        val visited = Array(size) { BooleanArray(size) }

        // support func for DFS
        fun dfs(x: Int, y: Int): Boolean {
            // if reach destination
            if (x == toX && y == toY) {
                return true
            }

            // mark current as viewed
            visited[y][x] = true

            // directions of movements
            val directions = arrayOf(
                intArrayOf(-1, 0),
                intArrayOf(1, 0),
                intArrayOf(0, -1),
                intArrayOf(0, 1)
            )

            // step for all directions
            for (dir in directions) {
                val newX = x + dir[1]
                val newY = y + dir[0]

                // Check out of bounds and if point aleady visited
                if (newX in 0 until size && newY in 0 until size &&
                    field[newY][newX] == '0' && !visited[newY][newX]) {
                    if (dfs(newX, newY)) {
                        return true
                    }
                }
            }
            return false
        }
        return dfs(fromX, fromY)
    }

    // move ball, if it exit and path exist from one point, to another.
    fun moveBall(fromX:Int,fromY:Int,toX:Int,toY:Int):Int{
        if(fromX<0 || fromX >=size||fromY<0||fromY>=size||
            toX<0||toX>=size||toY<0||toY>=size){
            return -1
        }
        if(field[fromY][fromX] != '0' && isPathExist(fromX,fromY,toX,toY)) {
            setPoint(toX, toY, field[fromY][fromX])
            setPoint(fromX, fromY,'0')
            val curScore = movementScore(toX, toY)
            score += curScore
            return curScore
        }
        return -1
    }
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

            // Check in another direction
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

            if (lineCoordinates.size > maxLineLen) {
                maxLineLen = lineCoordinates.size
                maxLineCoordinates = lineCoordinates
            }
        }

        if (maxLineLen >= 5) {
            for (coordinate in maxLineCoordinates) {
                setPoint(coordinate.x, coordinate.y, '0')
            }
            return 10 * 2.0.pow((maxLineLen - 5)).toInt()
        }

        return 0
    }

    fun writeToFile(context: Context, fileName: String) {
        val fileWriter = FileWriter()
        val stringBuilder = StringBuilder()

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
    fun readFromFile(context: Context, fileName: String) {
        val fileWriter = FileWriter()
        val data = fileWriter.readFromFile(context, fileName)
        if (data.isNotEmpty()) {
            val entries = data.split(";")
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
    private fun getRandomColor():Char{
        val randomIndex = Random.nextInt(7)
        when(randomIndex){
            0 -> return 'r'
            1 -> return 'b'
            2 -> return 'B'
            3 -> return 'g'
            4 -> return 'y'
            5 -> return 'c'
            6 -> return 'm'
        }
        return '0'
    }
    fun setPointAndCheckScore(x:Int, y:Int,color:Char):Int{
        setPoint(x,y,color)
        return movementScore(x,y)
    }
    fun getRandomBall(): Ball? {
        if (emptyPoints.isEmpty())
            return null
        val randomIndex = Random.nextInt(emptyPoints.size)
        val newBallCoordinates = emptyPoints[randomIndex]
        return Ball(newBallCoordinates.y, newBallCoordinates.x, getRandomColor())
    }
    fun clear(){
        field = Array(size) { CharArray(size) }
        emptyPoints = MutableList(size * size) { index ->
                Coordinates(index % size, index / size)
            }
        score = 0
        for (i in 0 until size) {
            for (j in 0 until size) {
                field[i][j] = '0' // by default all points are empty
            }
        }
    }
}

