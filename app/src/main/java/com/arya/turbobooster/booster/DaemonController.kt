package com.arya.turbobooster.booster

import android.content.Context
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

object DaemonController {
    private const val TAG = "DaemonController"
    private const val SOCKET_NAME = "turbodaemon_ipc"
    private const val PREFS_DAEMON = "turbodaemon_prefs"
    private const val KEY_AUTH_TOKEN = "daemon_auth_token"

    private var cachedToken: String? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        getAuthToken(context)
    }

    fun getAuthToken(context: Context? = null): String {
        cachedToken?.let { return it }
        val ctx = context ?: appContext
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREFS_DAEMON, Context.MODE_PRIVATE)
            var token = prefs.getString(KEY_AUTH_TOKEN, null)
            if (token == null) {
                token = UUID.randomUUID().toString()
                prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
            }
            cachedToken = token
            return token
        }
        return ""
    }

    suspend fun isDaemonAlive(context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val resp = sendCommand(token, "PING")
        resp?.startsWith("PONG") == true
    }

    suspend fun getDaemonStatus(context: Context? = null): String? = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        sendCommand(token, "GET_STATUS") ?: sendCommand(token, "PING")
    }

    suspend fun setProfile(profile: String, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val resp = sendCommand(token, "SET_PROFILE $profile")
        resp?.startsWith("OK") == true
    }

    suspend fun triggerReclaim(context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val resp = sendCommand(token, "TRIGGER_RECLAIM")
        resp?.startsWith("OK") == true
    }

    suspend fun stopDaemon(context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val resp = sendCommand(token, "STOP")
        resp?.startsWith("OK") == true
    }

    suspend fun addProtectedApp(pkg: String, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val resp = sendCommand(token, "ADD_PROTECTED_APP $pkg")
        resp?.startsWith("OK") == true
    }

    suspend fun removeProtectedApp(pkg: String, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val resp = sendCommand(token, "REMOVE_PROTECTED_APP $pkg")
        resp?.startsWith("OK") == true
    }

    suspend fun syncProtectedApps(context: Context): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken(context)
        val protectedApps = TurboBoosterManager.getProtectedApps(context)
        var allOk = true
        for (pkg in protectedApps) {
            val resp = sendCommand(token, "ADD_PROTECTED_APP $pkg")
            if (resp?.startsWith("OK") != true) {
                allOk = false
            }
        }
        allOk
    }

    suspend fun ensureDaemonRunning(context: Context): Boolean = withContext(Dispatchers.IO) {
        init(context)

        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            Log.w(TAG, "Cannot start daemon: Shizuku not ready")
            return@withContext false
        }

        val token = getAuthToken(context)
        if (isDaemonAlive(context)) {
            Log.d(TAG, "Daemon is already running and responsive")
            syncProtectedApps(context)
            return@withContext true
        }

        val nativeDir = context.applicationInfo.nativeLibraryDir
        var primaryPath = "$nativeDir/libturbodaemon.so"
        val fallbackPath = "/data/local/tmp/libturbodaemon.so"

        // Strategy A: Try launching from nativeLibraryDir first
        var started = launchAndVerify(primaryPath, token, context)

        // Strategy B: If primary launch blocked by SELinux, copy to /data/local/tmp and launch
        if (!started) {
            Log.w(TAG, "Primary launch failed or blocked by SELinux, falling back to $fallbackPath")
            ShellExecutor.execute("cp \"$primaryPath\" \"$fallbackPath\" && chmod 755 \"$fallbackPath\"", requirePrivileged = true)
            started = launchAndVerify(fallbackPath, token, context)
        }

        if (started) {
            Log.d(TAG, "TurboDaemon successfully running, syncing protected apps list")
            syncProtectedApps(context)
        } else {
            Log.e(TAG, "Failed to start TurboDaemon via all strategies")
        }

        started
    }

    private suspend fun launchAndVerify(exePath: String, token: String, context: Context): Boolean {
        return try {
            val cmd = arrayOf(exePath, "--token", token, "--socket", SOCKET_NAME, "--daemon")
            ShellExecutor.newProcess(cmd, null, null)

            var attempts = 0
            while (attempts < 15) {
                delay(100)
                if (isDaemonAlive(context)) {
                    return true
                }
                attempts++
            }
            false
        } catch (t: Throwable) {
            Log.e(TAG, "Exception launching daemon at $exePath: ${t.message}", t)
            false
        }
    }

    private fun sendCommand(token: String, cmd: String): String? {
        if (token.isEmpty()) return null
        var socket: LocalSocket? = null
        return try {
            socket = LocalSocket()
            socket.connect(LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT))
            socket.soTimeout = 2000

            val writer = BufferedWriter(OutputStreamWriter(socket.outputStream))
            val reader = BufferedReader(InputStreamReader(socket.inputStream))

            // 1. Authenticate with cryptographic token
            writer.write("AUTH $token\n")
            writer.flush()
            val authResp = reader.readLine()
            if (authResp != "OK") {
                Log.w(TAG, "Daemon authentication failed: $authResp")
                return null
            }

            // 2. Send authenticated command
            writer.write("$cmd\n")
            writer.flush()
            reader.readLine()
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Throwable) {}
        }
    }
}
