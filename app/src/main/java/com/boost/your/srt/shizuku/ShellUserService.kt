package com.boost.your.srt.shizuku

import kotlin.system.exitProcess

/**
 * Executes shell commands inside the Shizuku-privileged process.
 * Instantiated by Shizuku (no-arg constructor) after ShizukuHelper binds it.
 */
class ShellUserService() : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): Array<String> {
        return try {
            val process = ProcessBuilder("sh", "-c", command).start()
            var err = ""
            val errReader = Thread { err = process.errorStream.bufferedReader().readText() }
            errReader.start()
            val out = process.inputStream.bufferedReader().readText()
            errReader.join()
            val code = process.waitFor()
            arrayOf(code.toString(), out.trim(), err.trim())
        } catch (e: Exception) {
            arrayOf("-1", "", e.message ?: e.javaClass.simpleName)
        }
    }
}
