package com.arya.turbobooster.booster

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

data class LogEntry(
    val timestamp: String,
    val command: String,
    val output: String,
    val isSuccess: Boolean
)

object TurboBoosterManager {
    private const val TAG = "TurboBoosterManager"
    private const val PREFS_NAME = "turbo_booster_prefs"
    private const val KEY_IS_TURBO_ACTIVE = "is_turbo_active"
    private const val KEY_AUTO_BOOT = "auto_boot"

    val TARGET_GAMES = listOf(
        "com.tencent.ig" to "PUBG Mobile",
        "com.mobile.legends" to "Mobile Legends",
        "com.roblox.client" to "Roblox",
        "com.mojang.minecraftpe" to "Minecraft",
        "com.lemon.lvoverseas" to "CapCut Global",
        "com.lemon.ccpadpro" to "CapCut Pad Pro"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isTurboActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_TURBO_ACTIVE, false)
    }

    fun setTurboActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_TURBO_ACTIVE, active).apply()
    }

    fun isAutoBootEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_BOOT, false)
    }

    fun setAutoBootEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BOOT, enabled).apply()
    }

    suspend fun applySuperPerformanceMode(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())

        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            val ts = timeFormat.format(java.util.Date())
            val msg = if (!ShellExecutor.isShizukuAvailable()) "Shizuku service offline" else "Shizuku not authorized"
            onLog(LogEntry(ts, "Pre-flight Security Check", "FAILED: $msg (UID 2000 required)", false))
            return false
        }

        var allSucceeded = true

        suspend fun runStep(cmd: String, desc: String? = null) {
            val ts = timeFormat.format(java.util.Date())
            val res = ShellExecutor.execute(cmd, requirePrivileged = true)
            val out = if (res.stdout.isNotBlank()) res.stdout else res.stderr
            val entry = LogEntry(
                timestamp = ts,
                command = desc ?: cmd,
                output = out.ifBlank { if (res.isSuccess) "OK" else "Code ${res.exitCode}" },
                isSuccess = res.isSuccess
            )
            onLog(entry)
            if (!res.isSuccess) allSucceeded = false
        }

        // 1. Lock CPU frequencies to max (Fixed Performance Mode)
        runStep("cmd power set-fixed-performance-mode-enabled true", "Lock CPU Clocks (2.4GHz Gold / 1.9GHz Silver)")

        // 2. Bypass thermal throttling
        runStep("cmd thermalservice override-status 0", "Override Thermal Throttling (Cooling Status 0)")

        // 3. Fast SQLite In-Memory Caching (bypasses eMMC bottleneck)
        runStep("setprop debug.sqlite.journalmode MEMORY", "SQLite Journal Mode -> MEMORY")
        runStep("setprop debug.sqlite.syncmode OFF", "SQLite Sync Mode -> OFF")

        // 4. Remove GPU Gaussian blur
        runStep("settings put global disable_window_blurs 1", "Disable GPU Window Blurs")

        // 5. Lock 90Hz Display Refresh Rate
        runStep("settings put secure refresh_rate_mode 2", "Refresh Rate Mode -> 2 (Forced High)")
        runStep("settings put system peak_refresh_rate 90.0", "Peak Refresh Rate -> 90.0 Hz")

        // 6. Memory Factor and Activity Manager optimizations
        runStep("cmd activity memory-factor set NORMAL", "Activity Memory Factor -> NORMAL")
        runStep("cmd device_config put activity_manager max_cached_processes 16", "Max Cached Processes -> 16")
        runStep("cmd device_config put activity_manager_native_boot modern_queue_enabled true", "Modern Broadcast Queue -> ENABLED")

        // 7. MIUI / HyperOS Downscaling Subsystem
        runStep("cmd miui.downscale disable-downscale false && cmd miui.downscale set-debug-mode true", "Enable HyperOS Downscale Subsystem")

        // 8. 0.7x native downscale for heavy games
        for ((pkg, name) in TARGET_GAMES) {
            runStep("cmd game set --downscale 0.7 $pkg", "Game Downscale 0.7x ($name)")
            runStep("cmd miui.downscale add-debug-app $pkg 0.7", "HyperOS Downscale 0.7x ($name)")
        }

        // 9. Chrome Anti-Reload Flags and 0.85x downscale
        val chromeFlags = "_ --disable-low-end-device-mode --purge-max-password-store=0 --max-active-webgl-contexts=16 --enable-features=ParallelDownloading,SmoothScrolling --disable-features=AudioProcessHighPriority,CalculateNativeWinOcclusion,SpareRendererForSitePerProcess"
        runStep("echo '$chromeFlags' > /data/local/tmp/chrome-command-line && chmod 644 /data/local/tmp/chrome-command-line", "Inject Chrome Anti-Reload Flags")
        runStep("cmd game set --downscale 0.85 com.android.chrome", "Chrome Resolution Scaling -> 0.85x")

        // 10. SurfaceFlinger Realtime Binder Thread & ADPF CPU Hint
        runStep("setprop debug.sf.set_binder_thread_rt 1", "SurfaceFlinger RT Binder Thread -> 1")
        runStep("setprop debug.sf.enable_adpf_cpu_hint 1", "SurfaceFlinger ADPF CPU Hint -> 1")

        // 11. Ergonomic Touch Delays (Fix Accidental Long-Press & Menu Popups)
        runStep("settings put secure long_press_timeout 400", "Touch Long-Press Delay -> 400ms (Normal)")
        runStep("settings put secure multi_press_timeout 300", "Touch Multi-Press Delay -> 300ms (Normal)")

        // 12. Quad Speaker 3D Surround & Dolby Atmos Bitstream Unlock
        AudioBoosterManager.applyAudioHardwareBoost(onLog)
        AudioBoosterManager.initAudioEffects(context)

        if (allSucceeded) {
            setTurboActive(context, true)
        }
        return allSucceeded
    }

    suspend fun applyStockMode(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())

        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            val ts = timeFormat.format(java.util.Date())
            val msg = if (!ShellExecutor.isShizukuAvailable()) "Shizuku service offline" else "Shizuku not authorized"
            onLog(LogEntry(ts, "Pre-flight Security Check", "FAILED: $msg (UID 2000 required)", false))
            return false
        }

        var allSucceeded = true

        suspend fun runStep(cmd: String, desc: String? = null) {
            val ts = timeFormat.format(java.util.Date())
            val res = ShellExecutor.execute(cmd, requirePrivileged = true)
            val out = if (res.stdout.isNotBlank()) res.stdout else res.stderr
            val entry = LogEntry(
                timestamp = ts,
                command = desc ?: cmd,
                output = out.ifBlank { if (res.isSuccess) "OK" else "Code ${res.exitCode}" },
                isSuccess = res.isSuccess
            )
            onLog(entry)
            if (!res.isSuccess) allSucceeded = false
        }

        runStep("cmd power set-fixed-performance-mode-enabled false", "Restore Adaptive CPU Clocks")
        runStep("cmd thermalservice reset", "Reset Thermal Status to Hardware Sensor")
        runStep("settings put global disable_window_blurs 0", "Restore Window Blurs")
        runStep("settings put global window_animation_scale 1.0", "Reset Animation Scale")
        runStep("settings put secure refresh_rate_mode 0", "Reset Refresh Rate Mode (Dynamic 60-90Hz)")
        runStep("settings put system peak_refresh_rate 90.0", "Restore Stock Peak Refresh Rate")
        runStep("cmd miui.downscale disable-downscale true", "Disable HyperOS Downscale")
        runStep("cmd miui.downscale clear-debug-apps", "Clear HyperOS Debug Downscale Apps")
        runStep("cmd miui.downscale set-debug-mode false", "Disable HyperOS Downscale Debug Mode")
        runStep("cmd device_config delete activity_manager max_cached_processes", "Reset Max Cached Processes")
        runStep("cmd device_config delete activity_manager_native_boot modern_queue_enabled", "Reset Modern Queue Config")
        runStep("cmd activity memory-factor reset", "Reset Memory Factor")

        for ((pkg, name) in TARGET_GAMES) {
            runStep("cmd game reset $pkg", "Reset Game Scaling ($name)")
        }

        runStep("cmd game reset com.android.chrome", "Reset Chrome Scaling")
        runStep("rm -f /data/local/tmp/chrome-command-line", "Remove Chrome Command Line Flags")

        runStep("setprop debug.sqlite.journalmode \"\"", "Reset SQLite Journal Mode")
        runStep("setprop debug.sqlite.syncmode \"\"", "Reset SQLite Sync Mode")
        runStep("setprop debug.sf.set_binder_thread_rt 0", "Reset SurfaceFlinger RT Binder")
        runStep("setprop debug.sf.enable_adpf_cpu_hint 0", "Reset SurfaceFlinger ADPF Hint")
        runStep("settings put secure long_press_timeout 400", "Reset Long-Press Delay (400ms Normal)")
        runStep("settings put secure multi_press_timeout 300", "Reset Multi-Press Delay (300ms Normal)")

        // Reset Audio
        AudioBoosterManager.resetAudioHardwareStock(onLog)
        AudioBoosterManager.release()

        setTurboActive(context, false)
        return allSucceeded
    }
}
