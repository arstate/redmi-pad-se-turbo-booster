package com.arya.turbobooster.booster

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.lang.reflect.Method

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val isSuccess: Boolean
)

object ShellExecutor {
    private const val TAG = "ShellExecutor"

    private val newProcessMethod: Method? by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply {
                isAccessible = true
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to resolve Shizuku.newProcess via reflection", t)
            null
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (t: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (t: Throwable) {
            false
        }
    }

    suspend fun execute(command: String, requirePrivileged: Boolean = true): CommandResult = withContext(Dispatchers.IO) {
        if (isShizukuAvailable() && hasShizukuPermission() && newProcessMethod != null) {
            executeViaShizuku(command)
        } else if (!requirePrivileged) {
            executeViaLocalShell(command)
        } else {
            CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = "Privilege error: Shizuku UID 2000 service inactive or unpermitted",
                isSuccess = false
            )
        }
    }

    private suspend fun executeViaShizuku(command: String): CommandResult {
        return try {
            val method = newProcessMethod ?: throw IllegalStateException("Shizuku newProcess method unavailable")
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            readProcessOutput(process, command)
        } catch (t: Throwable) {
            Log.e(TAG, "Shizuku execution failed: ${t.message}", t)
            CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = "Shizuku error: ${t.localizedMessage ?: t.javaClass.simpleName}",
                isSuccess = false
            )
        }
    }

    private suspend fun executeViaLocalShell(command: String): CommandResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            readProcessOutput(process, command)
        } catch (t: Throwable) {
            Log.e(TAG, "Local shell execution failed: ${t.message}", t)
            CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = "Shell error: ${t.localizedMessage ?: t.javaClass.simpleName}",
                isSuccess = false
            )
        }
    }

    private suspend fun readProcessOutput(process: Process, command: String, timeoutMs: Long = 8000L): CommandResult = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val stdoutDeferred = async {
                    try {
                        process.inputStream.bufferedReader().use(BufferedReader::readText).trim()
                    } catch (_: Throwable) { "" }
                }
                val stderrDeferred = async {
                    try {
                        process.errorStream.bufferedReader().use(BufferedReader::readText).trim()
                    } catch (_: Throwable) { "" }
                }

                withTimeout(timeoutMs) {
                    val exitCode = process.waitFor()
                    val stdout = stdoutDeferred.await()
                    val stderr = stderrDeferred.await()
                    CommandResult(
                        command = command,
                        exitCode = exitCode,
                        stdout = stdout,
                        stderr = stderr,
                        isSuccess = (exitCode == 0)
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            try { process.destroy() } catch (_: Throwable) {}
            CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = "Command timed out after ${timeoutMs}ms",
                isSuccess = false
            )
        } catch (t: Throwable) {
            CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = t.localizedMessage ?: "Unknown error",
                isSuccess = false
            )
        }
    }
}
