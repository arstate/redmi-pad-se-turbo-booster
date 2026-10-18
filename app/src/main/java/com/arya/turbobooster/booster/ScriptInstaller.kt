package com.arya.turbobooster.booster

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ScriptInstaller {
    private const val TAG = "ScriptInstaller"

    /**
     * Extracts all shell scripts bundled inside APK assets/scripts/
     * into internal app storage (/data/user/0/com.arya.turbobooster/files/scripts/)
     * and mirrors to /sdcard/TurboBoosterScripts/ for user convenience.
     */
    fun extractAllScripts(context: Context): List<File> {
        val extractedFiles = mutableListOf<File>()
        try {
            val scriptsDir = File(context.filesDir, "scripts").apply { mkdirs() }
            val assetFiles = context.assets.list("scripts") ?: emptyArray()

            for (fileName in assetFiles) {
                val destFile = File(scriptsDir, fileName)
                context.assets.open("scripts/$fileName").use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.setExecutable(true, false)
                destFile.setReadable(true, false)
                extractedFiles.add(destFile)
            }

            // Mirror to SDCard folder for quick access
            try {
                val sdcardMirror = File("/sdcard/TurboBoosterScripts").apply { mkdirs() }
                for (f in extractedFiles) {
                    val mirror = File(sdcardMirror, f.name)
                    f.copyTo(mirror, overwrite = true)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Mirror to /sdcard skipped: ${t.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting scripts from assets: ${e.message}", e)
        }
        return extractedFiles
    }

    /**
     * Executes an embedded script via Shizuku shell.
     */
    suspend fun executeBundledScript(
        context: Context,
        scriptName: String,
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val scriptContent = try {
            val file = File(File(context.filesDir, "scripts"), scriptName)
            if (file.exists()) {
                file.readText()
            } else {
                context.assets.open("scripts/$scriptName").bufferedReader().use { it.readText() }
            }
        } catch (t: Throwable) {
            val ts = timeFormat.format(java.util.Date())
            onLog?.invoke(LogEntry(ts, "Run Script: $scriptName", "FAILED: ${t.message}", false))
            return false
        }

        val base64 = android.util.Base64.encodeToString(scriptContent.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val cmd = "echo '$base64' | base64 -d > /data/local/tmp/$scriptName && chmod 755 /data/local/tmp/$scriptName && sh /data/local/tmp/$scriptName"
        val ts = timeFormat.format(java.util.Date())
        val res = ShellExecutor.execute(cmd, requirePrivileged = true)
        val out = if (res.stdout.isNotBlank()) res.stdout else res.stderr
        val entry = LogEntry(
            timestamp = ts,
            command = "sh $scriptName",
            output = out.ifBlank { if (res.isSuccess) "OK" else "Code ${res.exitCode}" },
            isSuccess = res.isSuccess
        )
        onLog?.invoke(entry)
        return res.isSuccess
    }
}
