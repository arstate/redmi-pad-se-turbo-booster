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

enum class BoosterProfile {
    STOCK,
    FLAGSHIP_BALANCED,
    EXTREME_GAMING_RENDER,
    DEBLOAT_RESTORE
}

object TurboBoosterManager {
    private const val TAG = "TurboBoosterManager"
    private const val PREFS_NAME = "turbo_booster_prefs"
    private const val KEY_ACTIVE_PROFILE = "active_profile"
    private const val KEY_IS_TURBO_ACTIVE = "is_turbo_active"
    private const val KEY_AUTO_BOOT = "auto_boot"
    private const val KEY_PROTECTED_APPS = "protected_apps_set"

    // HARDCODED RED LIST: NEVER uninstall, disable, or crash these core framework packages
    val RED_LIST = setOf(
        "com.miui.securitycenter",
        "com.miui.securitycore",
        "com.miui.securityadd",
        "com.xiaomi.finddevice",
        "com.lbe.security.miui",
        "com.miui.home",
        "com.android.systemui",
        "com.android.settings",
        "com.miui.powerkeeper",
        "com.google.android.gms",
        "com.android.vending",
        "com.android.webview",
        "com.google.android.webview"
    )

    // GREEN LIST DEBLOAT: Safe to uninstall + crash undead -800 persistent daemons
    val GREEN_LIST_GOOGLE = listOf(
        "com.google.android.as",
        "com.google.android.as.oss",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.wellbeing",
        "com.google.android.projection.gearhead",
        "com.google.mainline.telemetry",
        "com.google.android.feedback",
        "com.google.android.apps.tachyon",
        "com.google.android.videos",
        "com.google.android.apps.messaging",
        "com.google.android.marvin.talkback"
    )

    val GREEN_LIST_XIAOMI = listOf(
        "com.mi.globalminusscreen",
        "com.miui.hybrid",
        "com.miui.msa.global",
        "com.miui.analytics",
        "com.miui.daemon",
        "com.miui.bugreport",
        "com.xiaomi.mipicks",
        "com.xiaomi.aicr",
        "com.xiaomi.aiasst.vision",
        "com.miui.yellowpage",
        "com.xiaomi.glgm",
        "com.xiaomi.payment",
        "com.miui.aod",
        "com.milink.service",
        "com.mi.globalbrowser",
        "com.miui.player",
        "com.miui.videoplayer",
        "com.miui.weather2",
        "com.miui.phrase",
        "com.facebook.system",
        "com.facebook.appmanager",
        "com.facebook.services"
    )

    val GREEN_LIST_OPTIONAL_CLOUD = listOf(
        "com.miui.cloudservice",
        "com.miui.cloudbackup",
        "com.xiaomi.micloud.sdk",
        "com.miui.mishare.connectivity",
        "com.android.updater"
    )

    val ALL_GREEN_LIST = GREEN_LIST_GOOGLE + GREEN_LIST_XIAOMI + GREEN_LIST_OPTIONAL_CLOUD

    // Default Protected Apps (Excluded from heavy caging & suspension to protect notifications/alarms/VPNs)
    val DEFAULT_PROTECTED_APPS = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.discord",
        "com.tailscale.ipn",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "free.vpn.unblock.proxy.vpnmaster",
        "com.vpn.free.hotspot.secure.vpnify"
    )

    // Target Games and Creators
    val TARGET_GAMES = listOf(
        "com.tencent.ig" to "PUBG Mobile",
        "com.mobile.legends" to "Mobile Legends",
        "com.roblox.client" to "Roblox",
        "com.mojang.minecraftpe" to "Minecraft",
        "com.lemon.lvoverseas" to "CapCut Global",
        "com.lemon.ccpadpro" to "CapCut Pad Pro"
    )

    // Non-essential tracing/logging daemons safe to stop via MQSAS Call 21
    val TRACING_DAEMONS = listOf(
        "heapprofd",
        "incidentd",
        "mobile_log_d",
        "statsd",
        "traced",
        "traced_perf",
        "traced_probes",
        "traced_relay",
        "vendor_tcpdump"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getActiveProfile(context: Context): BoosterProfile {
        val name = getPrefs(context).getString(KEY_ACTIVE_PROFILE, BoosterProfile.STOCK.name)
        return try {
            BoosterProfile.valueOf(name ?: BoosterProfile.STOCK.name)
        } catch (_: Throwable) {
            BoosterProfile.STOCK
        }
    }

    fun setActiveProfile(context: Context, profile: BoosterProfile) {
        getPrefs(context).edit().putString(KEY_ACTIVE_PROFILE, profile.name).apply()
        setTurboActive(context, profile != BoosterProfile.STOCK)
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

    fun getProtectedApps(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_PROTECTED_APPS, DEFAULT_PROTECTED_APPS) ?: DEFAULT_PROTECTED_APPS
    }

    fun setProtectedApps(context: Context, apps: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_PROTECTED_APPS, apps).apply()
    }

    fun addProtectedApp(context: Context, pkg: String) {
        val current = getProtectedApps(context).toMutableSet()
        current.add(pkg)
        setProtectedApps(context, current)
    }

    fun removeProtectedApp(context: Context, pkg: String) {
        val current = getProtectedApps(context).toMutableSet()
        current.remove(pkg)
        setProtectedApps(context, current)
    }

    // Helper step execution with logging
    private suspend fun runStep(
        cmd: String,
        desc: String? = null,
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val ts = timeFormat.format(java.util.Date())
        val res = ShellExecutor.execute(cmd, requirePrivileged = true)
        val out = if (res.stdout.isNotBlank()) res.stdout else res.stderr
        val entry = LogEntry(
            timestamp = ts,
            command = desc ?: cmd,
            output = out.ifBlank { if (res.isSuccess) "OK" else "Code ${res.exitCode}" },
            isSuccess = res.isSuccess
        )
        onLog?.invoke(entry)
        return res.isSuccess
    }

    // MQSAS Call 21 Persistent Property Injection with Direct Fallback
    suspend fun mqsasSetProp(
        key: String,
        value: String,
        desc: String? = null,
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        val cmd = "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"setprop\" i32 1 s16 \"$key $value\" s16 \"/dev/null\" i32 600"
        val ok = runStep(cmd, desc ?: "MQSAS 21: $key -> $value", onLog)
        // Verify property or fallback
        val verifyRes = ShellExecutor.execute("getprop $key", requirePrivileged = true)
        if (verifyRes.stdout.trim() != value) {
            runStep("setprop $key \"$value\"", "Fallback direct setprop $key -> $value", onLog)
        }
        return ok
    }

    // MQSAS Call 21 Stop Overhead Daemon
    suspend fun mqsasStopDaemon(
        daemon: String,
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        val cmd = "service call miui.mqsas.IMQSNative 21 i32 1 s16 \"stop\" i32 1 s16 \"$daemon\" s16 \"/dev/null\" i32 600"
        return runStep(cmd, "MQSAS 21: Stop $daemon", onLog)
    }

    /**
     * Master State Machine Profile Dispatcher
     */
    suspend fun applyProfile(
        context: Context,
        profile: BoosterProfile,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        val ok = when (profile) {
            BoosterProfile.STOCK -> applyStockMode(context, onLog)
            BoosterProfile.FLAGSHIP_BALANCED -> applyFlagshipBalancedMode(context, onLog)
            BoosterProfile.EXTREME_GAMING_RENDER -> applyExtremeGamingRenderMode(context, onLog)
            BoosterProfile.DEBLOAT_RESTORE -> restoreDebloatedPackages(context, onLog)
        }
        if (ok) {
            setActiveProfile(context, profile)
        }
        return ok
    }

    // Compatibility alias for existing UI button
    suspend fun applySuperPerformanceMode(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        return applyFlagshipBalancedMode(context, onLog)
    }

    /**
     * LAYER 1 TO 5: FLAGSHIP_BALANCED PROFILE
     * SM6225 2.4GHz Gold Cores, 90Hz, 205dpi, Cgroup v2 Freezer (16 cached),
     * LMKD Heaviest Task, 0.60x Animation Scale, SQLite WAL/NORMAL, Zero-Blur.
     */
    suspend fun applyFlagshipBalancedMode(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog(LogEntry("", "Pre-flight Security Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        var allOk = true

        // --- LAYER 1: MQSAS CALL 21 SM6225 & HYPEROS 2.0 PROPERTIES ---
        // Gold Core Affinity & Touch/Render Pipeline
        mqsasSetProp("persist.sys.miui.sf_cores", "4-7", "SurfaceFlinger Gold Cores -> 4-7", onLog)
        mqsasSetProp("persist.sys.miui_animator_sched.bigcores", "4-7", "Animator Big Cores -> 4-7", onLog)
        mqsasSetProp("persist.sys.miui_animator_sched.big_prime_cores", "4-7", "Animator Big/Prime -> 4-7", onLog)
        mqsasSetProp("persist.sys.miui_animator_sched.sched_threads", "3", "Animator Sched Threads -> 3", onLog)
        mqsasSetProp("persist.vendor.display.miui.composer_boost", "4-7", "Display Composer Boost -> 4-7", onLog)
        mqsasSetProp("persist.sys.miui.sf_fps", "90", "HyperOS SurfaceFlinger FPS -> 90", onLog)
        mqsasSetProp("persist.sys.enable_inputopts", "true", "Input Options -> TRUE", onLog)
        mqsasSetProp("persist.sys.enable_pinfile", "true", "Pinfile Acceleration -> TRUE", onLog)
        mqsasSetProp("persist.sys.first.frame.accelerates", "true", "First Frame Accelerate -> TRUE", onLog)
        mqsasSetProp("persist.sys.raise_region_sampling_prio", "true", "Sampling Priority Boost -> TRUE", onLog)
        mqsasSetProp("persist.sys.turbosched.touch_boost", "true", "TurboSched Touch Boost -> TRUE", onLog)
        mqsasSetProp("persist.sys.dynamicbuffer.max_adjust_num", "1", "Dynamic Buffer Adjust -> 1", onLog)
        mqsasSetProp("persist.sys.freeform_window", "true", "Freeform Window Subsystem -> TRUE", onLog)

        // HyperOS Memory Engine (SPTM / MMS / SPC / FBO)
        mqsasSetProp("persist.miui.extm.enable", "0", "Disable Virtual RAM eMMC Extension -> 0", onLog)
        mqsasSetProp("persist.sys.miui_sptm.enable", "true", "SPTM Engine -> TRUE", onLog)
        mqsasSetProp("persist.sys.miui_sptm.enable_pl_type", "2", "SPTM Pipeline Type -> 2", onLog)
        mqsasSetProp("persist.sys.miui_sptm_new.enable", "true", "SPTM New Subsystem -> TRUE", onLog)
        mqsasSetProp("persist.sys.miui_startup_mode.enable", "true", "HyperOS Fast Startup -> TRUE", onLog)
        mqsasSetProp("persist.sys.gc_booster.enable", "1", "GC Booster -> 1", onLog)
        mqsasSetProp("persist.sys.miui_modify_heap_config.enable", "true", "Heap Config Modifier -> TRUE", onLog)
        mqsasSetProp("persist.sys.fboservice.ctrl", "true", "FBO Service Control -> TRUE", onLog)
        mqsasSetProp("persist.sys.mms.compact_enable", "true", "MMS In-RAM Compact -> TRUE", onLog)
        mqsasSetProp("persist.sys.mms.write_lmkd", "true", "MMS LMKD Synchronization -> TRUE", onLog)
        mqsasSetProp("persist.sys.mms.kill_fas_cached_idle", "true", "MMS Kill FAS Idle -> TRUE", onLog)
        mqsasSetProp("persist.sys.mms.bg_apps_limit", "24", "MMS Background Apps Limit -> 24", onLog)
        mqsasSetProp("persist.sys.spc.cpulimit.enabled", "true", "SPC CPU Limit -> TRUE", onLog)
        mqsasSetProp("persist.sys.smartpower.intercept.enable", "true", "SmartPower Intercept -> TRUE", onLog)
        mqsasSetProp("persist.sys.miui.adj_update_foreground_state.enable.delayMs", "3000", "ADJ FG Update Delay -> 3000ms", onLog)

        // Stop Overhead Tracing/Logging Daemons
        for (daemon in TRACING_DAEMONS) {
            mqsasStopDaemon(daemon, onLog)
        }
        // Safe log trimming (NEVER stop logd!)
        runStep("logcat -G 64K && logcat -c", "Trim Logcat Buffer to 64K", onLog)
        runStep("cmd looper_stats disable", "Disable Looper Stats Overhead", onLog)

        // --- LAYER 2: ANDROID 15 DEVICE_CONFIG & LMKD TUNING ---
        runStep("device_config set_sync_disabled_for_tests persistent", "Lock device_config Against GMS Overwrite", onLog)
        runStep("device_config put activity_manager_native_boot use_freezer true", "Cgroup v2 Cached Freezer -> TRUE", onLog)
        runStep("device_config put activity_manager_native_boot freeze_debounce_timeout 3000", "Freeze Debounce -> 3000ms", onLog)
        runStep("settings put global cached_apps_freezer enabled", "Global Cached Apps Freezer -> ENABLED", onLog)
        runStep("device_config put activity_manager max_cached_processes 16", "Max Cached Processes -> 16 (4GB Sweet Spot)", onLog)
        runStep("device_config put activity_manager max_phantom_processes 128", "Max Phantom Processes -> 128", onLog)
        runStep("device_config put activity_manager use_compaction true", "Android 15 In-RAM Compaction -> TRUE", onLog)
        runStep("device_config put activity_manager compact_throttle_1 45000", "Compact Throttle 1 -> 45000ms", onLog)
        runStep("device_config put activity_manager compact_throttle_2 45000", "Compact Throttle 2 -> 45000ms", onLog)
        runStep("device_config put activity_manager compact_full_rss_throttle_kb 16000", "Compact Full RSS Throttle -> 16MB", onLog)
        runStep("device_config put activity_manager compact_full_delta_rss_throttle_kb 8000", "Compact Delta RSS -> 8MB", onLog)

        // LMKD Heaviest-Task & Anti-Stall
        runStep("device_config put lmkd_native kill_heaviest_task true", "LMKD: Kill Heaviest Task First -> TRUE", onLog)
        runStep("device_config put lmkd_native psi_partial_stall_ms 60", "LMKD PSI Partial Stall -> 60ms", onLog)
        runStep("device_config put lmkd_native psi_complete_stall_ms 450", "LMKD PSI Complete Stall -> 450ms", onLog)
        runStep("device_config put lmkd_native thrashing_limit 50", "LMKD Thrashing Limit -> 50", onLog)
        runStep("device_config put lmkd_native thrashing_limit_decay 50", "LMKD Thrashing Decay -> 50", onLog)
        runStep("device_config put lmkd_native swap_free_low_percentage 15", "LMKD Swap Free Low -> 15%", onLog)

        // ART Runtime & Overhead Suppression
        runStep("device_config put runtime_native_boot disable_lock_profiling true", "Disable Lock Profiling -> TRUE", onLog)
        runStep("device_config put runtime_native_boot iorap_perfetto_enable false", "Disable IORap Perfetto -> FALSE", onLog)
        runStep("device_config put runtime_native_boot iorap_readahead_enable false", "Disable IORap Readahead -> FALSE", onLog)
        runStep("device_config put runtime_native metrics.write-to-statsd false", "Disable Runtime StatsD Metrics", onLog)
        runStep("device_config put runtime_native use_app_image_startup_cache true", "Use App Image Startup Cache -> TRUE", onLog)
        runStep("device_config put runtime_native usap_pool_enabled false", "USAP Pool -> FALSE (Save ~60MB RAM)", onLog)

        // Telemetry & AdServices Kill-Switches
        runStep("device_config put window_manager enable_cross_window_blurs false", "Disable Cross-Window Blurs", onLog)
        runStep("device_config put adservices global_kill_switch true", "AdServices Global Kill-Switch -> TRUE", onLog)
        runStep("device_config put adservices adservice_enabled false", "AdService -> FALSE", onLog)
        runStep("device_config put adservices fledge_background_fetch_enabled false", "Fledge Background Fetch -> FALSE", onLog)
        runStep("device_config put on_device_personalization global_kill_switch true", "ODP Global Kill-Switch -> TRUE", onLog)
        runStep("device_config put on_device_personalization federated_compute_kill_switch true", "ODP Federated Compute -> FALSE", onLog)

        // SQLite Robust WAL Mode (FIX Corruption Risk: NO MEMORY/OFF)
        runStep("setprop debug.sqlite.journalmode WAL", "SQLite Journal Mode -> WAL (Safe & Fast)", onLog)
        runStep("setprop debug.sqlite.wal.syncmode NORMAL", "SQLite WAL Sync Mode -> NORMAL", onLog)

        // --- LAYER 4: HYPEROS APPOPS & BACKGROUND CAGING ---
        runStep("cmd appops set com.miui.powerkeeper WRITE_SETTINGS ignore", "PowerKeeper Write Settings -> IGNORE", onLog)
        runStep("cmd appops set com.miui.powerkeeper RUN_ANY_IN_BACKGROUND ignore", "PowerKeeper Run Any Background -> IGNORE", onLog)

        // GMS & Play Store Sensor Caging (Keep foreground intact, kill background idle drain)
        runStep("cmd appops set com.google.android.gms ACTIVITY_RECOGNITION ignore", "GMS Activity Recognition -> IGNORE", onLog)
        runStep("cmd appops set com.google.android.gms BODY_SENSORS ignore", "GMS Body Sensors -> IGNORE", onLog)
        runStep("cmd appops set com.google.android.gms COARSE_LOCATION ignore", "GMS Coarse Location -> IGNORE", onLog)
        runStep("cmd appops set com.android.vending RUN_IN_BACKGROUND ignore", "Play Store Background -> IGNORE", onLog)
        runStep("cmd appops set com.android.vending RUN_ANY_IN_BACKGROUND ignore", "Play Store Run Any BG -> IGNORE", onLog)
        runStep("cmd appops set com.android.vending 10008 ignore", "Play Store Autostart (10008) -> IGNORE", onLog)
        runStep("cmd appops set com.android.vending 10053 ignore 2>/dev/null || true", "Play Store Service Link -> IGNORE", onLog)
        runStep("am set-standby-bucket com.android.vending rare", "Play Store Standby Bucket -> RARE", onLog)

        // Cage Heavy Social/Shopping Apps (excluding user's protected apps)
        cageHeavyBackgroundApps(context, onLog)

        // --- LAYER 5: DISPLAY 90Hz, FSG, INPUT & PINNER (DPI PRESERVED) ---
        // Never override tablet DPI automatically (user preference preserved)

        runStep("settings put system peak_refresh_rate 90.0", "Peak Refresh Rate -> 90.0 Hz", onLog)
        runStep("settings put system min_refresh_rate 90.0", "Min Refresh Rate -> 90.0 Hz", onLog)
        runStep("settings put secure user_refresh_rate 90", "User Refresh Rate -> 90", onLog)
        runStep("settings put secure refresh_rate_mode 2", "Refresh Rate Mode -> 2", onLog)

        runStep("settings put global disable_window_blurs 1", "Disable GPU Window Blurs", onLog)
        runStep("settings put system enable_miui_desktop_blur 0", "Disable Desktop Blur", onLog)
        runStep("settings put system miui_home_blur_level 0", "Home Blur Level -> 0", onLog)
        runStep("settings put system enable_dock_blur 0", "Dock Blur -> 0", onLog)

        // Flagship vs Blur Fix: c:3 without Adreno 610 blur choke g:3
        runStep("settings put system deviceLevelList \"v:1,c:3,g:1,m:2\"", "Device Level -> v:1,c:3,g:1,m:2 (Flagship c:3, GPU Saver g:1)", onLog)

        runStep("settings put global force_fsg_nav_bar 1", "FSG Navigation -> ENABLED", onLog)
        runStep("settings put system force_fsg_nav_bar 1", "System FSG Navigation -> ENABLED", onLog)
        runStep("settings put global hide_gesture_line 1", "Hide Gesture Indicator Line", onLog)

        // Flagship Balanced Animation Scale (0.60x - Smooth & Fast)
        runStep("settings put system miui_home_animation_rate 2", "MIUI Home Animation Rate -> 2", onLog)
        runStep("settings put global window_animation_scale 0.60", "Window Animation Scale -> 0.60x", onLog)
        runStep("settings put global transition_animation_scale 0.60", "Transition Animation Scale -> 0.60x", onLog)
        runStep("settings put global animator_duration_scale 0.60", "Animator Duration Scale -> 0.60x", onLog)

        // Input Responsiveness
        runStep("settings put secure long_press_timeout 200", "Long Press Timeout -> 200ms", onLog)
        runStep("settings put secure multi_press_timeout 150", "Multi Press Timeout -> 150ms", onLog)
        runStep("settings put system touch_response_speed 1", "Touch Response Speed -> 1", onLog)
        runStep("settings put system touch_gesture_speed 2", "Touch Gesture Speed -> 2", onLog)
        runStep("settings put secure assistant_on_nav_gesture 0", "Disable Corner Assistant Intercept", onLog)
        runStep("settings put secure search_press_hold_nav_handle 0", "Disable Nav Handle Hold Intercept", onLog)
        runStep("settings put global wifi_scan_always_enabled 0", "Disable Wi-Fi Background Polling", onLog)
        runStep("settings put global ble_scan_always_enabled 0", "Disable BLE Background Polling", onLog)

        // Freeform Multi-Window & CapCut Guard
        runStep("settings put global enable_freeform_support 1", "Enable Freeform Multi-Window", onLog)
        runStep("settings put global force_resizable_activities 1", "Force Resizable Activities", onLog)
        runStep("settings put global enable_non_resizable_multi_window 1", "Non-resizable Multi-Window", onLog)
        for (cPkg in listOf("com.lemon.lvoverseas", "com.lemon.ccpadpro")) {
            runStep("cmd game reset $cPkg", "Reset CapCut Scaling ($cPkg)", onLog)
            runStep("cmd appops set $cPkg READ_EXTERNAL_STORAGE allow", "CapCut Read Storage -> ALLOW", onLog)
            runStep("cmd appops set $cPkg WRITE_EXTERNAL_STORAGE allow", "CapCut Write Storage -> ALLOW", onLog)
            runStep("cmd appops set $cPkg MANAGE_EXTERNAL_STORAGE allow", "CapCut Manage Storage -> ALLOW", onLog)
            runStep("cmd appops set $cPkg READ_MEDIA_IMAGES allow", "CapCut Images -> ALLOW", onLog)
            runStep("cmd appops set $cPkg READ_MEDIA_VIDEO allow", "CapCut Video -> ALLOW", onLog)
            runStep("cmd appops set $cPkg READ_MEDIA_AUDIO allow", "CapCut Audio -> ALLOW", onLog)
            runStep("cmd appops set $cPkg NO_ISOLATED_STORAGE allow", "CapCut No Isolated Storage -> ALLOW", onLog)
            runStep("cmd appops set $cPkg RUN_IN_BACKGROUND allow", "CapCut Background -> ALLOW", onLog)
            runStep("dumpsys deviceidle whitelist +$cPkg", "CapCut DeviceIdle Whitelist", onLog)
        }

        // Core UI ART AOT Compilation & Trimming
        runStep("cmd package compile -m speed -f com.android.systemui", "AOT Speed Compile SystemUI", onLog)
        runStep("cmd package compile -m speed -f com.miui.home", "AOT Speed Compile MIUI Home", onLog)
        runStep("cmd package compile -m speed -f com.google.android.inputmethod.latin 2>/dev/null || true", "AOT Speed Gboard", onLog)
        runStep("cmd pinner repin 2>/dev/null || true", "Pinner Repin Core Buffers", onLog)
        runStep("sm fstrim", "Trim eMMC 5.1 Flash Storage", onLog)

        // Start / Wake Native Rust Daemon
        DaemonController.ensureDaemonRunning(context)

        return allOk
    }

    /**
     * EXTREME_GAMING_RENDER PROFILE
     * Thermal override 0, 0.25x instant animation, Joyose freeze, game downscaling & isolation.
     */
    suspend fun applyExtremeGamingRenderMode(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        // First apply balanced foundations
        applyFlagshipBalancedMode(context, onLog)

        onLog(LogEntry("", "--- EXTREME GAMING & RENDER MODE ENGAGED ---", "Thermal limit override 0 + Ultra Snappy 0.25x", true))

        // Thermal Override 0 (Opt-in extreme mode only, with warning)
        runStep("cmd thermalservice override-status 0", "Override Thermal Throttling Status -> 0 (COOL)", onLog)

        // Disable Joyose Throttler
        runStep("pm disable-user --user 0 com.xiaomi.joyose 2>/dev/null || true", "Disable Joyose Game Throttler", onLog)
        runStep("cmd activity crash --user 0 com.xiaomi.joyose 2>/dev/null || true", "Reap Undead Joyose Process", onLog)

        // Ultra Snappy Animation Scale (0.25x)
        runStep("settings put global window_animation_scale 0.25", "Window Animation Scale -> 0.25x", onLog)
        runStep("settings put global transition_animation_scale 0.25", "Transition Animation Scale -> 0.25x", onLog)
        runStep("settings put global animator_duration_scale 0.25", "Animator Duration Scale -> 0.25x", onLog)

        // Apply Game Resolution Downscaling (0.80x) & 90FPS overlay
        for ((pkg, name) in TARGET_GAMES) {
            if (pkg == "com.lemon.lvoverseas" || pkg == "com.lemon.ccpadpro" || pkg == "com.mobile.legends") {
                continue // Protect video editor and MLBB Unity engine from EGL crash
            }
            runStep("cmd game set --downscale 0.8 $pkg", "Downscale 0.8x ($name)", onLog)
            runStep("cmd miui.downscale add-debug-app $pkg 0.8 2>/dev/null || true", "HyperOS Downscale 0.8x ($name)", onLog)
            runStep("device_config put game_overlay $pkg mode=2,downscaleFactor=0.80,fps=90 2>/dev/null || true", "Game Overlay 90FPS ($name)", onLog)
            runStep("cmd tare set-vip 0 $pkg true 2>/dev/null || true", "TARE Set VIP True ($name)", onLog)
        }

        // Notify Daemon of Extreme Profile
        DaemonController.setProfile("EXTREME_GAMING_RENDER", context)

        return true
    }

    /**
     * STOCK PROFILE
     * Restores device to clean factory defaults.
     */
    suspend fun applyStockMode(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog(LogEntry("", "Pre-flight Security Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        var allOk = true

        // Reset Thermal
        runStep("cmd thermalservice reset", "Reset Thermal Status to Hardware Sensors", onLog)

        // Enable Joyose
        runStep("pm enable com.xiaomi.joyose 2>/dev/null || true", "Re-enable Joyose", onLog)

        // Reset Display & Animations
        runStep("settings put global disable_window_blurs 0", "Restore Window Blurs", onLog)
        runStep("settings put system enable_miui_desktop_blur 1", "Restore Desktop Blur", onLog)
        runStep("settings put system deviceLevelList \"v:1,c:1,g:1,m:1\"", "Reset Device Level -> Tier 1 Stock", onLog)
        runStep("settings put global window_animation_scale 1.0", "Reset Window Animation Scale -> 1.0x", onLog)
        runStep("settings put global transition_animation_scale 1.0", "Reset Transition Animation Scale -> 1.0x", onLog)
        runStep("settings put global animator_duration_scale 1.0", "Reset Animator Duration Scale -> 1.0x", onLog)
        runStep("settings put system miui_home_animation_rate 1", "Reset Home Animation Rate -> 1", onLog)
        runStep("settings put secure refresh_rate_mode 0", "Reset Refresh Rate Mode -> Auto/Dynamic", onLog)
        // wm density left untouched to preserve user preference

        // Reset SQLite
        runStep("setprop debug.sqlite.journalmode \"\"", "Reset SQLite Journal Mode", onLog)
        runStep("setprop debug.sqlite.wal.syncmode \"\"", "Reset SQLite WAL Sync Mode", onLog)

        // Reset Device Config
        runStep("device_config delete activity_manager max_cached_processes", "Reset Max Cached Processes", onLog)
        runStep("device_config delete activity_manager use_compaction", "Reset Compaction Config", onLog)
        runStep("device_config delete activity_manager_native_boot use_freezer", "Reset Freezer Config", onLog)
        runStep("device_config delete lmkd_native kill_heaviest_task", "Reset LMKD Config", onLog)

        // Reset Games
        for ((pkg, name) in TARGET_GAMES) {
            runStep("cmd game reset $pkg 2>/dev/null || true", "Reset Scaling ($name)", onLog)
            runStep("cmd tare set-vip 0 $pkg false 2>/dev/null || true", "Reset TARE VIP ($name)", onLog)
        }

        // Restore PowerKeeper
        runStep("cmd appops set com.miui.powerkeeper WRITE_SETTINGS allow", "Restore PowerKeeper Settings", onLog)
        runStep("cmd appops set com.miui.powerkeeper RUN_ANY_IN_BACKGROUND allow", "Restore PowerKeeper Background", onLog)

        // Restore GMS / Vending
        runStep("cmd appops set com.google.android.gms ACTIVITY_RECOGNITION allow", "Restore GMS Activity Recognition", onLog)
        runStep("cmd appops set com.android.vending RUN_IN_BACKGROUND allow", "Restore Play Store Background", onLog)
        runStep("am set-standby-bucket com.android.vending active", "Restore Play Store Standby Bucket -> ACTIVE", onLog)

        // Stop Daemon
        DaemonController.stopDaemon(context)

        setTurboActive(context, false)
        return allOk
    }

    /**
     * LAYER 3: SURGICAL DEBLOAT & UNDEAD -800 CRASH-PURGE
     */
    suspend fun executeDebloat(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog(LogEntry("", "Security Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        onLog(LogEntry("", ">>> SURGICAL DEBLOAT & UNDEAD CRASH-PURGE INITIATED <<<", "Reaping idle RAM hogs", true))

        var count = 0
        for (pkg in ALL_GREEN_LIST) {
            // Anti-Bootloop Rule: NEVER touch packages in RED_LIST
            if (RED_LIST.contains(pkg)) {
                onLog(LogEntry("", "SECURITY BLOCKED: $pkg", "Package is protected by Hardcoded Red-List", false))
                continue
            }

            // 1. Uninstall for user 0
            val uninst = ShellExecutor.execute("pm uninstall -k --user 0 $pkg", requirePrivileged = true)
            // 2. Crash undead persistent -800 process from memory immediately
            ShellExecutor.execute("cmd activity crash --user 0 $pkg", requirePrivileged = true)
            ShellExecutor.execute("cmd appops set $pkg RUN_IN_BACKGROUND ignore 2>/dev/null || true", requirePrivileged = true)
            ShellExecutor.execute("am force-stop $pkg 2>/dev/null || true", requirePrivileged = true)

            if (uninst.isSuccess) {
                count++
                onLog(LogEntry("", "Debloated: $pkg", "Uninstalled & Undead Process Reaped from RAM", true))
            }
        }

        onLog(LogEntry("", "Debloat Complete", "Purged $count bloat packages from user 0", true))
        return true
    }

    /**
     * 1-TAP RESTORE ALL DEBLOATED PACKAGES
     */
    suspend fun restoreDebloatedPackages(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog(LogEntry("", "Security Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        onLog(LogEntry("", ">>> RESTORING DEBLOATED PACKAGES <<<", "Reinstalling existing APKs to user 0", true))

        var count = 0
        for (pkg in ALL_GREEN_LIST) {
            val res = ShellExecutor.execute("cmd package install-existing $pkg", requirePrivileged = true)
            ShellExecutor.execute("cmd appops set $pkg RUN_IN_BACKGROUND allow 2>/dev/null || true", requirePrivileged = true)
            if (res.isSuccess) {
                count++
                onLog(LogEntry("", "Restored: $pkg", "Successfully restored for user 0", true))
            }
        }

        onLog(LogEntry("", "Restore Complete", "Restored $count packages", true))
        return true
    }

    /**
     * Cages heavy 3rd party apps while strictly respecting the protected apps list.
     */
    suspend fun cageHeavyBackgroundApps(
        context: Context,
        onLog: ((LogEntry) -> Unit)? = null
    ) {
        val protectedList = getProtectedApps(context)
        val heavyHogs = listOf(
            "com.ss.android.ugc.trill",
            "com.shopee.id",
            "com.tokopedia.tkpd",
            "com.instagram.android",
            "com.facebook.katana",
            "com.netflix.mediaclient",
            "com.maxstream"
        )

        for (pkg in heavyHogs) {
            if (protectedList.contains(pkg)) {
                onLog?.invoke(LogEntry("", "Protected: $pkg", "Excluded from caging (User protected)", true))
                continue
            }
            runStep("cmd appops set $pkg RUN_IN_BACKGROUND ignore 2>/dev/null || true", "Cage $pkg Background", onLog)
            runStep("cmd appops set $pkg RUN_ANY_IN_BACKGROUND ignore 2>/dev/null || true", "Cage $pkg Run Any BG", onLog)
            runStep("cmd appops set $pkg WAKE_LOCK ignore 2>/dev/null || true", "Cage $pkg Wake Lock", onLog)
            runStep("cmd appops set $pkg 10008 ignore 2>/dev/null || true", "Cage $pkg Autostart (10008)", onLog)
            runStep("cmd appops set $pkg 10053 ignore 2>/dev/null || true", "Cage $pkg Service Link", onLog)
            runStep("cmd activity set-bg-restriction-level --user 0 $pkg restricted_bucket 2>/dev/null || true", "Restrict BG Bucket $pkg", onLog)
            runStep("cmd tare set-vip 0 $pkg false 2>/dev/null || true", "TARE Demote VIP $pkg", onLog)
            runStep("cmd deviceidle whitelist -$pkg 2>/dev/null || true", "Remove DeviceIdle Whitelist $pkg", onLog)
        }
    }
}
