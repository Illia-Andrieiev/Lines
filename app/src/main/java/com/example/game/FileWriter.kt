package com.example.game

import android.content.Context
import java.io.FileOutputStream
import java.io.FileInputStream

class FileWriter{
    fun writeToFile(context: Context, fileName: String, data: String) {
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
            fileOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readFromFile(context: Context, fileName: String): String {
        val fileInputStream: FileInputStream
        return try {
            fileInputStream = context.openFileInput(fileName)
            val inputStreamReader = fileInputStream.bufferedReader()
            val stringBuilder = StringBuilder()
            inputStreamReader.forEachLine { stringBuilder.append(it) }
            fileInputStream.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}

