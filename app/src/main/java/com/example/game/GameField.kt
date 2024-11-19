package com.example.game

import android.content.Context
import kotlin.random.Random

data class Coordinates(val x:Int, val y:Int)
class GameField(private val size: Int) {
    // our game field
    private val field: Array<CharArray> = Array(size) { CharArray(size) }
    private val emptyPoints: MutableList<Coordinates> =
        MutableList(size * size) { index ->
            Coordinates(index % size, index / size)
        }

    fun getSize():Int{
        return size
    }
    init {
        for (i in 0 until size) {
            for (j in 0 until size) {
                field[i][j] = '0' // by default all points are empty
            }
        }
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
    fun moveBall(fromX:Int,fromY:Int,toX:Int,toY:Int){
        if(fromX<0 || fromX >=size||fromY<0||fromY>=size||
            toX<0||toX>=size||toY<0||toY>=size){
            return
        }
        if(field[fromY][fromX] != '0' && isPathExist(fromX,fromY,toX,toY)) {
            setPoint(toX, toY, field[fromY][fromX])
            setPoint(fromX, fromY,'0')
        }
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
    fun getRandomCoordinate(): Coordinates? {
        if (emptyPoints.isEmpty())
            return null
        val randomIndex = Random.nextInt(emptyPoints.size)
        return emptyPoints.removeAt(randomIndex)
    }
}
