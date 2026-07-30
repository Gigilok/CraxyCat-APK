package com.crazycat.app.tools

import android.content.Context
import java.io.File

object AircrackRunner {

    private fun prepareBinary(context: Context): File {
        val outFile = File(context.filesDir, "aircrack-ng")
        if (!outFile.exists()) {
            context.assets.open("binaries/aircrack_arm64").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile.setExecutable(true)
            outFile.setReadable(true)
        }
        return outFile
    }

    private fun prepareWordlist(context: Context): File {
        val outFile = File(context.filesDir, "wordlist.txt")
        if (!outFile.exists()) {
            context.assets.open("wordlists/default.txt").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return outFile
    }

    fun crackHandshake(context: Context, pcapFile: File, onResult: (String?) -> Unit) {
        Thread {
            try {
                val bin = prepareBinary(context)
                val wordlist = prepareWordlist(context)
                
                val process = Runtime.getRuntime().exec(
                    arrayOf(bin.absolutePath, pcapFile.absolutePath, "-w", wordlist.absolutePath)
                )
                
                val reader = process.inputStream.bufferedReader()
                var line: String?
                var foundKey: String? = null
                
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("KEY FOUND!")) {
                        foundKey = line.substringAfter("[").substringBefore("]").trim()
                        process.destroy()
                        break
                    }
                }
                onResult(foundKey)
            } catch (e: Exception) {
                onResult(null)
            }
        }.start()
    }
}
