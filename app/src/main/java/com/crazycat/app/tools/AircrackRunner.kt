package com.crazycat.app.tools

import android.content.Context
import java.io.File

object AircrackRunner {

    private fun prepareBinaryAndLibs(context: Context): File {
        val outDir = context.filesDir
        
        // Lista de todos os arquivos que estão na pasta assets/binaries/
        // Inclui o executável e as 7 bibliotecas .so
        val filesToExtract = arrayOf(
            "aircrack_arm64", "libsqlite3.so", "libnl-3.so", 
            "libnl-genl-3.so", "libssl.so.3", "libcrypto.so.3", 
            "libz.so.1", "libc++_shared.so"
        )

        for (fileName in filesToExtract) {
            val outFile = File(outDir, fileName)
            if (!outFile.exists()) {
                context.assets.open("binaries/$fileName").use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            // Garante permissão de execução para o binário principal
            if (fileName == "aircrack_arm64") {
                outFile.setExecutable(true)
            }
        }
        
        return File(outDir, "aircrack_arm64")
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
                val bin = prepareBinaryAndLibs(context)
                val wordlist = prepareWordlist(context)
                
                // O segredo aqui: Passar o LD_LIBRARY_PATH apontando para a pasta onde extraímos os .so
                val env = mapOf("LD_LIBRARY_PATH" to context.filesDir.absolutePath)
                
                val processBuilder = ProcessBuilder(
                    bin.absolutePath, 
                    pcapFile.absolutePath, 
                    "-w", 
                    wordlist.absolutePath
                )
                
                processBuilder.environment()["LD_LIBRARY_PATH"] = context.filesDir.absolutePath
                
                val process = processBuilder.start()
                
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
