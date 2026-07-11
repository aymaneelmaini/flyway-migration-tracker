package com.aymaneelmaini.flywaytracker.utils

import java.io.File

class TerminalClient(private val workingDir: String) {

    fun exec(vararg args: String): List<String> {
        if (workingDir.isBlank()) return emptyList()
        return try {
            val process = ProcessBuilder(args.toList())
                .directory(File(workingDir))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            output.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
