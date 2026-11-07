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

        var criticalSucceeded = true
        var allSucceeded = true

        suspend fun runStep(cmd: String, desc: String? = null, isCritical: Boolean = false) {
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
            if (!res.isSuccess) {
                allSucceeded = false
                if (isCritical) criticalSucceeded = false
            }
        }

        // 1. Lock CPU frequencies to max (Fixed Performance Mode)
        runStep("cmd power set-fixed-performance-mode-enabled true", "Lock CPU Clocks (2.4GHz Gold / 1.9GHz Silver)", isCritical = true)

        // 2. Bypass thermal throttling
        runStep("cmd thermalservice override-status 0", "Override Thermal Throttling (Cooling Status 0)", isCritical = true)

        // 3. Fast SQLite In-Memory Caching (bypasses eMMC bottleneck)
        runStep("setprop debug.sqlite.journalmode MEMORY", "SQLite Journal Mode -> MEMORY", isCritical = true)
        runStep("setprop debug.sqlite.syncmode OFF", "SQLite Sync Mode -> OFF", isCritical = true)

        // 4. Remove GPU Gaussian blur
        runStep("settings put global disable_window_blurs 1", "Disable GPU Window Blurs", isCritical = true)

        // 5. Lock 90Hz Display Refresh Rate
        runStep("settings put secure refresh_rate_mode 2", "Refresh Rate Mode -> 2 (Forced High)", isCritical = true)
        runStep("settings put system peak_refresh_rate 90.0", "Peak Refresh Rate -> 90.0 Hz", isCritical = true)

        // 6. Memory Factor and Activity Manager optimizations
        runStep("cmd activity memory-factor set NORMAL", "Activity Memory Factor -> NORMAL", isCritical = true)
        runStep("cmd device_config put activity_manager max_cached_processes 16", "Max Cached Processes -> 16", isCritical = true)
        runStep("cmd device_config put activity_manager_native_boot modern_queue_enabled true", "Modern Broadcast Queue -> ENABLED", isCritical = true)

        // 7. MIUI / HyperOS Downscaling Subsystem
        runStep("cmd miui.downscale disable-downscale false && cmd miui.downscale set-debug-mode true", "Enable HyperOS Downscale Subsystem")

        // 8. 0.7x native downscale for heavy games (Excluding MLBB to prevent Unity Surface Crash)
        for ((pkg, name) in TARGET_GAMES) {
            if (pkg == "com.mobile.legends") {
                runStep("cmd game reset com.mobile.legends", "Reset Game Scaling (Mobile Legends)")
                runStep("cmd appops set com.mobile.legends READ_EXTERNAL_STORAGE allow", "ML Storage Read -> ALLOW")
                runStep("cmd appops set com.mobile.legends WRITE_EXTERNAL_STORAGE allow", "ML Storage Write -> ALLOW")
                runStep("cmd appops set com.mobile.legends MANAGE_EXTERNAL_STORAGE allow", "ML Storage Manage -> ALLOW")
                continue
            }
            runStep("cmd game set --downscale 0.7 $pkg", "Game Downscale 0.7x ($name)")
            runStep("cmd miui.downscale add-debug-app $pkg 0.7", "HyperOS Downscale 0.7x ($name)")
        }

        // Anti-Crash: Ensure ANGLE driver is deleted (Adreno 610 crashes Unity engine under ANGLE)
        runStep("settings delete global angle_gl_driver_selection_values", "Delete Global ANGLE Driver")
        runStep("settings delete global angle_gl_driver_selection_pkgs", "Delete ANGLE Driver Pkgs")
        runStep("settings delete global updatable_driver_all_apps", "Delete Updatable Driver Override")

        // 9. Chrome Anti-Reload Flags and 0.85x downscale
        val chromeFlags = "_ --disable-low-end-device-mode --purge-max-password-store=0 --max-active-webgl-contexts=16 --enable-features=ParallelDownloading,SmoothScrolling --disable-features=AudioProcessHighPriority,CalculateNativeWinOcclusion,SpareRendererForSitePerProcess"
        runStep("echo '$chromeFlags' > /data/local/tmp/chrome-command-line && chmod 644 /data/local/tmp/chrome-command-line", "Inject Chrome Anti-Reload Flags")
        runStep("cmd game set --downscale 0.85 com.android.chrome", "Chrome Resolution Scaling -> 0.85x")

        // 10. SurfaceFlinger Realtime Binder Thread & ADPF CPU Hint
        runStep("setprop debug.sf.set_binder_thread_rt 1", "SurfaceFlinger RT Binder Thread -> 1", isCritical = true)
        runStep("setprop debug.sf.enable_adpf_cpu_hint 1", "SurfaceFlinger ADPF CPU Hint -> 1", isCritical = true)

        // 11. Zero-Delay Touch & Tablet Gesture Responsiveness
        runStep("settings put secure long_press_timeout 250", "Touch Long-Press -> 250ms (Zero Lag)")
        runStep("settings put secure tap_timeout 100", "Touch Tap Timeout -> 100ms")
        runStep("settings put secure multi_press_timeout 150", "Touch Multi-Press -> 150ms")
        runStep("settings put system pointer_speed 3", "Touch Pointer Speed -> 3")
        runStep("cmd device_config put input_native_boot velocitytracker_strategy impulse", "Input VelocityTracker -> Impulse")
        runStep("settings put secure assistant_on_nav_gesture 0", "Disable Nav Corner Assistant Interception")
        runStep("settings put secure search_press_hold_nav_handle 0", "Disable Nav Handle Hold Interception")
        runStep("setprop vendor.perf.gestureflingboost.enable true", "Enable Gesture Fling Boost")
        runStep("setprop persist.sys.turbosched.touch_boost true", "Enable TurboSched Touch Boost")

        // 12. HyperOS Flagship Unf*cker & MQSAS Backdoor (from Enki013/hyperosunfcker)
        runStep("settings put system deviceLevelList \"v:1,c:3,g:1,m:2\"", "Flagship Tier Spoof (c:3 High Perf, g:1 GPU Saver, m:2 RAM)", isCritical = true)
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.cpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: CPU Computility -> Level 6")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.computility.gpulevel 6\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: GPU Computility -> Level 6")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.advanced_visual_release 3\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: Advanced Visual -> Level 3")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.support_view_smoothcorner true\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: View Smoothcorner -> TRUE")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.support_window_smoothcorner true\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: Window Smoothcorner -> TRUE")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.mi_shadow_supported true\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: Native Shadow -> TRUE")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.miui.blur.enable false\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: MIUI Blur -> FALSE")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.sf.disable_blurs 1\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: SurfaceFlinger Blurs -> DISABLED")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.background_blur_supported false\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: Background Blur -> FALSE")
        runStep("service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"persist.sys.miui_blur_enabled false\" s16 \"/storage/emulated/0/log.txt\" i32 600", "MQSAS Backdoor: MIUI Blur Enabled -> FALSE")

        // 13. Disable Desktop & Tablet Dock Blur (Saves 100% Fillrate on Swipe)
        runStep("settings put global enable_miui_desktop_blur 0", "Desktop Gaussian Blur -> 0")
        runStep("settings put system miui_home_blur_level 0", "Home Blur Level -> 0")
        runStep("settings put system config_icon_animation 0", "Disable Heavy Icon Re-render Animation")
        runStep("settings put system enable_dock_blur 0", "Tablet Floating Dock Blur -> 0")
        runStep("settings put system show_dock_divider 0", "Tablet Dock Divider -> 0")

        // 14. PowerKeeper & Joyose Lockdown (Bypass Throttling & Allow Stable 90Hz)
        runStep("cmd appops set com.miui.powerkeeper WRITE_SETTINGS ignore", "PowerKeeper Write Settings -> IGNORE")
        runStep("cmd appops set com.miui.powerkeeper GET_USAGE_STATS ignore", "PowerKeeper Usage Stats -> IGNORE")
        runStep("cmd appops set com.miui.powerkeeper RUN_IN_BACKGROUND ignore", "PowerKeeper Background -> IGNORE")
        runStep("cmd appops set com.xiaomi.joyose TOAST_WINDOW ignore", "Joyose Cloud Telemetry -> IGNORE")
        runStep("am force-stop com.xiaomi.joyose", "Force Stop Joyose Background Loop")

        // 15. Golden Ratio Animation Scale (0.35x - Anti NPE Crash + Ultra Fast)
        runStep("settings put system miui_home_animation_rate 2", "Launcher Physics Curve -> 2 (Fast & Smooth)")
        runStep("settings put global window_animation_scale 0.35", "Window Animation Scale -> 0.35x")
        runStep("settings put global transition_animation_scale 0.35", "Transition Animation Scale -> 0.35x")
        runStep("settings put global animator_duration_scale 0.35", "Animator Duration Scale -> 0.35x")
        runStep("settings put secure linkage_state 1", "Recents Linkage State -> 1")
        runStep("settings put global task_stack_view_layout_style 2", "Task Stack View -> 2 (Smooth Recents)")

        // 16. RAM 4GB Defense: Disable Virtual RAM (Zero eMMC Thrashing)
        runStep("settings put global isExternalRamOn 0", "Virtual RAM (eMMC) -> OFF")
        runStep("settings put global ram_expand_size 0", "RAM Expand Size -> 0 MB")
        runStep("settings put global miui_memory_extension 0", "MIUI Memory Extension -> 0")

        // 17. Multi-Window & Freeform Engine Unlock
        runStep("settings put global enable_freeform_support 1", "AOSP Freeform Multi-Window -> ENABLED")
        runStep("settings put global force_resizable_activities 1", "Force Resizable Activities -> ENABLED")
        runStep("settings put global enable_non_resizable_multi_window 1", "Non-resizable Multi-Window -> ENABLED")

        // 18. Suppress Heavy Background Leeches & Xiaomi Trackers
        runStep("cmd appops set com.google.android.as RUN_IN_BACKGROUND ignore", "Google AS Background -> IGNORE")
        runStep("pm suspend --user 0 com.google.android.as", "Suspend Google AS")
        runStep("cmd appops set com.milink.service RUN_IN_BACKGROUND ignore", "MiLink Service Background -> IGNORE")
        runStep("cmd appops set com.xiaomi.aicr RUN_IN_BACKGROUND ignore", "Xiaomi AICR Background -> IGNORE")
        runStep("cmd appops set com.miui.msa.global RUN_IN_BACKGROUND ignore", "MSA Global Ads -> IGNORE")
        runStep("cmd appops set com.miui.analytics RUN_IN_BACKGROUND ignore", "MIUI Analytics -> IGNORE")
        runStep("cmd appops set com.xiaomi.metok-sdk RUN_IN_BACKGROUND ignore", "Xiaomi Metok SDK -> IGNORE")
        runStep("cmd appops set com.miui.daemon RUN_IN_BACKGROUND ignore", "MIUI Daemon -> IGNORE")

        // 19. SurfaceFlinger Buffer Latch & Dirty Regions (4PDA / Coolapk 90Hz Stutter Fix)
        runStep("setprop debug.sf.latch_unsignaled 1", "SurfaceFlinger Latch Unsignaled -> 1")
        runStep("setprop debug.sf.enable_hwc_vds 1", "SurfaceFlinger HWC VDS -> 1")
        runStep("setprop debug.sf.enable_advanced_sf_phase_offset 1", "SurfaceFlinger Advanced Phase Offset -> 1")
        runStep("setprop debug.hwui.render_dirty_regions false", "HWUI Render Dirty Regions -> FALSE")

        // 20. Android Phantom Process Killer Bypass & eMMC Logging Lockdown
        runStep("cmd device_config put activity_manager max_phantom_processes 2147483647", "Max Phantom Processes -> UNLIMITED")
        runStep("settings put global settings_enable_monitor_phantom_procs false", "Monitor Phantom Procs -> FALSE")
        runStep("settings put system send_miui_log 0", "MIUI Log Sending -> 0")
        runStep("settings put system upload_log 0", "MIUI Log Upload -> 0")
        runStep("settings put system anonymous_data 0", "Anonymous Data -> 0")
        runStep("settings put secure miui_log_enable 0", "Secure MIUI Log -> 0")
        runStep("settings put secure track_service 0", "Track Service -> 0")
        runStep("settings put global miui_log_enable 0", "Global MIUI Log -> 0")

        // 21. AOT DEXOPT Speed Compilation (SystemUI, Launcher, Settings)
        runStep("cmd package compile -m speed -f com.miui.home", "AOT Speed Compile -> HyperOS Launcher")
        runStep("cmd package compile -m speed -f com.android.systemui", "AOT Speed Compile -> SystemUI")
        runStep("cmd package compile -m speed -f com.android.settings", "AOT Speed Compile -> Settings")

        // 20. Extract & Deploy Bundled APK Shell Scripts to Internal Storage & SDCard
        try {
            ScriptInstaller.extractAllScripts(context)
        } catch (_: Throwable) {}

        // 21. Quad Speaker 3D Surround & Dolby Atmos Bitstream Unlock
        AudioBoosterManager.applyAudioHardwareBoost(onLog)
        AudioBoosterManager.initAudioEffects(context)

        // 22. Execute Master 100 & Flagship Unfcker Shell Scripts
        try {
            ScriptInstaller.executeBundledScript(context, "boost_100.sh", onLog)
            ScriptInstaller.executeBundledScript(context, "boost_flagship_unfcker.sh", onLog)
        } catch (t: Throwable) {
            Log.e(TAG, "Error executing bundled scripts: ${t.message}")
        }

        if (criticalSucceeded) {
            setTurboActive(context, true)
        }
        return criticalSucceeded
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
        runStep("settings put system pointer_speed 0", "Reset Pointer Speed")
        runStep("cmd device_config delete input_native_boot velocitytracker_strategy", "Reset VelocityTracker")

        // Reset HyperOS Unfcker & Flagship Spoofing
        runStep("settings put system deviceLevelList \"v:1,c:1,g:1,m:1\"", "Reset Device Level to Stock Tier 1")
        runStep("settings put system miui_home_animation_rate 1", "Reset Launcher Animation Rate")
        runStep("settings put global enable_miui_desktop_blur 1", "Restore Desktop Blur")
        runStep("settings delete system enable_dock_blur", "Reset Dock Blur")
        runStep("settings delete system show_dock_divider", "Reset Dock Divider")
        runStep("cmd appops set com.miui.powerkeeper WRITE_SETTINGS allow", "Restore PowerKeeper Permissions")
        runStep("pm unsuspend --user 0 com.google.android.as", "Unsuspend Google AS")

        // Reset Audio
        AudioBoosterManager.resetAudioHardwareStock(onLog)
        AudioBoosterManager.release()

        setTurboActive(context, false)
        return allSucceeded
    }
}
