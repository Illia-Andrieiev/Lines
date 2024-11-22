package com.example.game

import android.content.Context
import java.io.FileOutputStream
import java.io.FileInputStream
class FileWriter {

    // Function to write data to a file
    fun writeToFile(context: Context, fileName: String, data: String) {
        val fileOutputStream: FileOutputStream
        try {
            // Open file output stream in private mode
            fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            // Write data to the file
            fileOutputStream.write(data.toByteArray())
            // Close the file output stream
            fileOutputStream.close()
        } catch (e: Exception) {
            // Print stack trace in case of an exception
            e.printStackTrace()
        }
    }

    // Function to read data from a file
    fun readFromFile(context: Context, fileName: String): String {
        val fileInputStream: FileInputStream
        return try {
            // Open file input stream
            fileInputStream = context.openFileInput(fileName)
            // Create a buffered reader to read the file
            val inputStreamReader = fileInputStream.bufferedReader()
            // Use a StringBuilder to accumulate the file contents
            val stringBuilder = StringBuilder()
            // Read each line and append to the StringBuilder
            inputStreamReader.forEachLine { stringBuilder.append(it) }
            // Close the file input stream
            fileInputStream.close()
            // Return the accumulated string
            stringBuilder.toString()
        } catch (e: Exception) {
            // Print stack trace in case of an exception
            e.printStackTrace()
            ""
        }
    }
}
