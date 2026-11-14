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
        "com.miui.home",
        "com.android.systemui",
        "com.arya.turbobooster",
        "moe.shizuku.privileged.api",
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

    // Core System UI & Framework packages targeted for AOT speed compilation
    val CORE_AOT_PACKAGES = listOf(
        "com.android.systemui",
        "com.miui.home",
        "com.google.android.inputmethod.latin",
        "com.android.settings",
        "com.google.android.webview",
        "com.android.chrome",
        "com.arya.turbobooster"
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
        val stored = getPrefs(context).getStringSet(KEY_PROTECTED_APPS, null)
        val base = stored?.toMutableSet() ?: DEFAULT_PROTECTED_APPS.toMutableSet()
        // Guarantee core frameworks & booster remain strictly protected across upgrades
        base.add("com.miui.home")
        base.add("com.android.systemui")
        base.add("com.arya.turbobooster")
        base.add("moe.shizuku.privileged.api")
        return base
    }

    fun setProtectedApps(context: Context, apps: Set<String>) {
        val safeApps = apps.toMutableSet()
        safeApps.add("com.miui.home")
        safeApps.add("com.android.systemui")
        safeApps.add("com.arya.turbobooster")
        safeApps.add("moe.shizuku.privileged.api")
        getPrefs(context).edit().putStringSet(KEY_PROTECTED_APPS, safeApps).apply()
    }

    fun addProtectedApp(context: Context, pkg: String) {
        val current = getProtectedApps(context).toMutableSet()
        current.add(pkg)
        setProtectedApps(context, current)
    }

    fun removeProtectedApp(context: Context, pkg: String) {
        if (pkg == "com.miui.home" || pkg == "com.android.systemui" || pkg == "com.arya.turbobooster" || pkg == "moe.shizuku.privileged.api") {
            return // Immutable system framework & booster immunity
        }
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
        mqsasSetProp("persist.sys.mms.kill_fas_cached_idle", "false", "MMS Kill FAS Idle -> FALSE (Protect Launcher)", onLog)
        mqsasSetProp("persist.sys.mms.bg_apps_limit", "24", "MMS Background Apps Limit -> 24", onLog)
        mqsasSetProp("persist.sys.spc.cpulimit.enabled", "true", "SPC CPU Limit -> TRUE", onLog)
        mqsasSetProp("persist.sys.smartpower.intercept.enable", "true", "SmartPower Intercept -> TRUE", onLog)
        mqsasSetProp("persist.sys.miui.adj_update_foreground_state.enable.delayMs", "3000", "ADJ FG Update Delay -> 3000ms", onLog)

        // Adreno 610 libhwui Anti-Crash Guard: smoothcorner triggers null pointer dereference in clipSmoothCorner on Bengal GPU
        mqsasSetProp("persist.sys.support_window_smoothcorner", "false", "Disable Window SmoothCorner (Anti-SIGSEGV)", onLog)
        mqsasSetProp("persist.sys.support_view_smoothcorner", "false", "Disable View SmoothCorner (Anti-SIGSEGV)", onLog)
        mqsasSetProp("persist.sys.advanced_visual_release", "0", "Reset Advanced Visual Release -> 0", onLog)
        mqsasSetProp("persist.sys.computility.cpulevel", "0", "Reset Computility CPU Level -> 0", onLog)
        mqsasSetProp("persist.sys.computility.gpulevel", "0", "Reset Computility GPU Level -> 0", onLog)

        // Stop Overhead Tracing/Logging Daemons
        for (daemon in TRACING_DAEMONS) {
            mqsasStopDaemon(daemon, onLog)
        }
        // Safe log trimming (NEVER stop logd!)
        runStep("logcat -G 1M", "Set Logcat Buffer to 1M", onLog)
        runStep("cmd looper_stats disable", "Disable Looper Stats Overhead", onLog)

        // --- LAYER 2: ANDROID 15 DEVICE_CONFIG & LMKD TUNING ---
        runStep("device_config set_sync_disabled_for_tests persistent", "Lock device_config Against GMS Overwrite", onLog)
        runStep("device_config put activity_manager_native_boot use_freezer true", "Cgroup v2 Cached Freezer -> TRUE", onLog)
        runStep("device_config put activity_manager_native_boot freeze_debounce_timeout 3000", "Freeze Debounce -> 3000ms", onLog)
        runStep("settings put global cached_apps_freezer enabled", "Global Cached Apps Freezer -> ENABLED", onLog)
        runStep("device_config put activity_manager max_cached_processes 24", "Max Cached Processes -> 24 (Headroom for Launcher)", onLog)
        runStep("device_config put activity_manager max_phantom_processes 128", "Max Phantom Processes -> 128", onLog)
        runStep("device_config put activity_manager use_compaction true", "Android 15 In-RAM Compaction -> TRUE", onLog)
        runStep("device_config put activity_manager compact_throttle_1 45000", "Compact Throttle 1 -> 45000ms", onLog)
        runStep("device_config put activity_manager compact_throttle_2 45000", "Compact Throttle 2 -> 45000ms", onLog)
        runStep("device_config put activity_manager compact_full_rss_throttle_kb 16000", "Compact Full RSS Throttle -> 16MB", onLog)
        runStep("device_config put activity_manager compact_full_delta_rss_throttle_kb 8000", "Compact Delta RSS -> 8MB", onLog)

        // LMKD Standard OOM Order (Anti-Launcher-Kill: NEVER kill heaviest task first)
        runStep("device_config override lmkd_native kill_heaviest_task false", "LMKD: Kill Heaviest Task -> FALSE", onLog)
        runStep("device_config delete lmkd_native kill_heaviest_task", "LMKD: Delete Heaviest Task Override", onLog)
        runStep("device_config put lmkd_native psi_partial_stall_ms 60", "LMKD PSI Partial Stall -> 60ms", onLog)
        runStep("device_config put lmkd_native psi_complete_stall_ms 450", "LMKD PSI Complete Stall -> 450ms", onLog)
        runStep("device_config put lmkd_native thrashing_limit 50", "LMKD Thrashing Limit -> 50", onLog)
        runStep("device_config put lmkd_native thrashing_limit_decay 50", "LMKD Thrashing Decay -> 50", onLog)
        runStep("device_config put lmkd_native swap_free_low_percentage 15", "LMKD Swap Free Low -> 15%", onLog)

        // --- LAYER 2.5: EXPLICIT HOME LAUNCHER & SYSTEMUI IMMUNITY ---
        runStep("settings put system miui_home_animation_rate 2", "MIUI Home Fast Animation Rate -> 2", onLog)
        runStep("cmd activity set-bg-restriction-level --user 0 com.miui.home exempted", "Exempt Launcher from BG Restriction", onLog)
        runStep("am set-standby-bucket com.miui.home active", "Launcher Standby Bucket -> ACTIVE", onLog)
        runStep("cmd appops set com.miui.home RUN_IN_BACKGROUND allow", "Allow Launcher Background AppOps", onLog)
        runStep("cmd appops set com.miui.home RUN_ANY_IN_BACKGROUND allow", "Allow Launcher Run Any BG AppOps", onLog)
        runStep("cmd activity set-bg-restriction-level --user 0 com.android.systemui exempted", "Exempt SystemUI from BG Restriction", onLog)
        runStep("am set-standby-bucket com.android.systemui active", "SystemUI Standby Bucket -> ACTIVE", onLog)
        runStep("cmd appops set com.android.systemui RUN_IN_BACKGROUND allow", "Allow SystemUI Background AppOps", onLog)
        runStep("cmd appops set com.android.systemui RUN_ANY_IN_BACKGROUND allow", "Allow SystemUI Run Any BG AppOps", onLog)
        runStep("cmd activity set-bg-restriction-level --user 0 com.arya.turbobooster exempted", "Exempt TurboBooster from BG Restriction", onLog)
        runStep("am set-standby-bucket com.arya.turbobooster active", "TurboBooster Standby Bucket -> ACTIVE", onLog)
        runStep("dumpsys deviceidle whitelist +com.miui.home", "DeviceIdle Whitelist -> com.miui.home", onLog)
        runStep("dumpsys deviceidle whitelist +com.android.systemui", "DeviceIdle Whitelist -> com.android.systemui", onLog)
        runStep("dumpsys deviceidle whitelist +com.arya.turbobooster", "DeviceIdle Whitelist -> com.arya.turbobooster", onLog)
        runStep("device_config put system_performance com.android.server.flags.skip_home_art_pins false", "Enable Home ART Pins in Pinner", onLog)
        runStep("device_config put system_performance com.android.server.flags.pin_webview true", "Pin WebView in Pinner", onLog)

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

        // SQLite Framework Default: do not override debug.sqlite.* globally to prevent multi-process locks in apps (e.g. Facebook)
        runStep("setprop debug.sqlite.journalmode \"\"", "Reset SQLite Journal Mode (Anti-Crash)", onLog)
        runStep("setprop debug.sqlite.wal.syncmode \"\"", "Reset SQLite WAL Sync Mode (Anti-Crash)", onLog)

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

        // Do not force force_fsg_nav_bar or hide_gesture_line (prevents com.miui.home crash to lockscreen)

        // Flagship Balanced Animation Scale (0.60x - Smooth & Fast)
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

        // Trimming and repin
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
        runStep("device_config override lmkd_native kill_heaviest_task false", "Reset LMKD Heaviest Task", onLog)

        // Reset HyperOS / MMS & SmoothCorner
        mqsasSetProp("persist.sys.mms.kill_fas_cached_idle", "false", "Reset MMS Kill FAS Idle -> FALSE", onLog)
        mqsasSetProp("persist.sys.support_window_smoothcorner", "false", "Reset Window SmoothCorner -> FALSE", onLog)
        mqsasSetProp("persist.sys.support_view_smoothcorner", "false", "Reset View SmoothCorner -> FALSE", onLog)
        mqsasSetProp("persist.sys.advanced_visual_release", "0", "Reset Advanced Visual Release -> 0", onLog)
        mqsasSetProp("persist.sys.computility.cpulevel", "0", "Reset Computility CPU Level -> 0", onLog)
        mqsasSetProp("persist.sys.computility.gpulevel", "0", "Reset Computility GPU Level -> 0", onLog)

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
     * LAYER 3: SURGICAL DEBLOAT & UNDEAD -800 CRASH-PURGE (WITH SMART DETECTION)
     * Checks if package is already uninstalled/disabled for user 0. If already removed, skips it!
     */
    suspend fun executeDebloat(
        _context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog(LogEntry("", "Security Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        onLog(LogEntry("", ">>> SURGICAL DEBLOAT (SMART DETECTION) INITIATED <<<", "Checking installed packages for user 0...", true))

        val installedRes = ShellExecutor.execute("pm list packages --user 0", requirePrivileged = true)
        val installedPkgs = installedRes.stdout.lines()
            .map { it.trim().removePrefix("package:").trim() }
            .filter { it.isNotBlank() }
            .toSet()

        var purgedCount = 0
        var skippedCount = 0

        for (pkg in ALL_GREEN_LIST) {
            // Anti-Bootloop Rule: NEVER touch packages in RED_LIST
            if (RED_LIST.contains(pkg)) {
                onLog(LogEntry("", "SECURITY BLOCKED: $pkg", "Package is protected by Hardcoded Red-List", false))
                continue
            }

            if (!installedPkgs.contains(pkg)) {
                skippedCount++
                onLog(LogEntry("", "Skip Debloat: $pkg", "Already debloated (not installed for user 0)", true))
                continue
            }

            // 1. Uninstall for user 0
            val uninst = ShellExecutor.execute("pm uninstall -k --user 0 $pkg", requirePrivileged = true)
            // 2. Crash undead persistent -800 process from memory immediately
            ShellExecutor.execute("cmd activity crash --user 0 $pkg 2>/dev/null || true", requirePrivileged = true)
            ShellExecutor.execute("cmd appops set $pkg RUN_IN_BACKGROUND ignore 2>/dev/null || true", requirePrivileged = true)
            ShellExecutor.execute("am force-stop $pkg 2>/dev/null || true", requirePrivileged = true)

            if (uninst.isSuccess) {
                purgedCount++
                onLog(LogEntry("", "Debloated: $pkg", "Uninstalled & Undead Process Reaped from RAM", true))
            }
        }

        onLog(LogEntry("", "Debloat Complete", "Purged: $purgedCount packages, Skipped: $skippedCount (already uninstalled)", true))
        return true
    }

    /**
     * SMART INCREMENTAL AOT SPEED COMPILATION
     * Automatically skips packages already compiled with [status=speed],
     * running directly from APK ([status=run-from-apk]), optimized via Cloud Dex Metadata
     * ([reason=vdex-dm]), compiled via commandline ([reason=cmdline]), or WebAPKs.
     * Only compiles packages that are NOT yet compiled to speed (e.g. newly installed apps).
     */
    suspend fun executeSmartIncrementalAot(
        context: Context,
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog?.invoke(LogEntry("", "AOT Compilation Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        onLog?.invoke(LogEntry("", ">>> SMART INCREMENTAL AOT SPEED COMPILATION <<<", "Scanning package compilation status...", true))

        val prefs = context.getSharedPreferences("turbobooster_aot_prefs", Context.MODE_PRIVATE)
        val previouslyCompiled = prefs.getStringSet("compiled_packages", emptySet())?.toMutableSet() ?: mutableSetOf()

        // 1. Gather Candidate Packages: Core UI/Framework + 3rd Party User Apps
        val candidatePkgs = mutableListOf<String>()
        candidatePkgs.addAll(CORE_AOT_PACKAGES)

        val thirdPartyRes = ShellExecutor.execute("pm list packages -3", requirePrivileged = true, timeoutMs = 8000L)
        if (thirdPartyRes.isSuccess && thirdPartyRes.stdout.isNotBlank()) {
            thirdPartyRes.stdout.lines().forEach { line ->
                val pkg = line.trim().removePrefix("package:").trim()
                if (pkg.isNotEmpty() && !candidatePkgs.contains(pkg)) {
                    candidatePkgs.add(pkg)
                }
            }
        }

        // 2. Fast Dexopt Status Parsing via dumpsys package dexopt (timeout 25s for full system dump)
        data class DexoptInfo(val status: String, val reason: String)
        val statusMap = mutableMapOf<String, DexoptInfo>()
        val dexoptDump = ShellExecutor.execute("dumpsys package dexopt", requirePrivileged = true, timeoutMs = 25000L)
        if (dexoptDump.isSuccess && dexoptDump.stdout.isNotBlank()) {
            var currentPkg: String? = null
            for (line in dexoptDump.stdout.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.contains(" ")) {
                    currentPkg = trimmed.substring(1, trimmed.length - 1)
                    continue
                }
                if (currentPkg != null && trimmed.contains("[status=")) {
                    val sStart = trimmed.indexOf("[status=") + 8
                    val sEnd = trimmed.indexOf(']', sStart)
                    val status = if (sEnd > sStart) trimmed.substring(sStart, sEnd) else ""

                    val rStart = trimmed.indexOf("[reason=")
                    val reason = if (rStart != -1) {
                        val rEnd = trimmed.indexOf(']', rStart + 8)
                        if (rEnd > rStart + 8) trimmed.substring(rStart + 8, rEnd) else ""
                    } else ""

                    if (!statusMap.containsKey(currentPkg)) {
                        statusMap[currentPkg] = DexoptInfo(status, reason)
                    }
                }
            }
        }

        var skippedCount = 0
        var compiledCount = 0
        var updatedPrefs = false

        for (pkg in candidatePkgs) {
            // PWA WebAPKs have no native dex code to compile
            if (pkg.startsWith("org.chromium.webapk.")) {
                skippedCount++
                continue
            }

            // Check status from bulk dump map or per-package fallback
            var info = statusMap[pkg]
            if (info == null) {
                val pCheck = ShellExecutor.execute("dumpsys package $pkg | grep -E '\\[status='", requirePrivileged = true, timeoutMs = 5000L)
                if (pCheck.isSuccess && pCheck.stdout.isNotBlank()) {
                    val out = pCheck.stdout
                    val sStart = out.indexOf("[status=")
                    val status = if (sStart != -1) {
                        val sEnd = out.indexOf(']', sStart + 8)
                        if (sEnd > sStart + 8) out.substring(sStart + 8, sEnd) else ""
                    } else ""

                    val rStart = out.indexOf("[reason=")
                    val reason = if (rStart != -1) {
                        val rEnd = out.indexOf(']', rStart + 8)
                        if (rEnd > rStart + 8) out.substring(rStart + 8, rEnd) else ""
                    } else ""

                    info = DexoptInfo(status, reason)
                    statusMap[pkg] = info
                }
            }

            val status = info?.status ?: "unknown"
            val reason = info?.reason ?: "unknown"

            // Smart Skip Condition:
            // 1. Fully compiled to speed
            // 2. Already compiled via cmdline
            // 3. Optimized via Google Play Cloud Dex Metadata (vdex-dm)
            // 4. Executed directly from APK (embedded dex, cannot produce odex)
            // 5. Already recorded in persistent compiled cache
            val isAlreadyOptimized = (
                status == "speed" ||
                status == "run-from-apk" ||
                reason == "cmdline" ||
                reason == "vdex-dm" ||
                previouslyCompiled.contains(pkg)
            )

            if (isAlreadyOptimized) {
                skippedCount++
                onLog?.invoke(LogEntry("", "Skip AOT: $pkg", "Already optimized ($status, $reason) - skipped", true))
                continue
            }

            // Needs compilation (newly installed app or uncompiled app)
            onLog?.invoke(LogEntry("", "AOT Compiling: $pkg", "Status: $status ($reason) -> compiling to speed...", true))
            val compRes = ShellExecutor.execute("cmd package compile -m speed $pkg", requirePrivileged = true, timeoutMs = 60000L)
            if (compRes.isSuccess) {
                compiledCount++
                previouslyCompiled.add(pkg)
                updatedPrefs = true
                onLog?.invoke(LogEntry("", "Compiled: $pkg", "Successfully compiled to speed", true))
            } else {
                onLog?.invoke(LogEntry("", "Compile Failed: $pkg", compRes.stderr.ifBlank { "Exit ${compRes.exitCode}" }, false))
            }
        }

        if (updatedPrefs) {
            prefs.edit().putStringSet("compiled_packages", previouslyCompiled).apply()
        }

        // Repin buffers & fstrim
        runStep("cmd pinner repin 2>/dev/null || true", "Pinner Repin Core Buffers", onLog)
        runStep("sm fstrim", "Trim eMMC 5.1 Flash Storage", onLog)

        onLog?.invoke(LogEntry("", "Incremental AOT Complete", "Compiled: $compiledCount packages, Skipped: $skippedCount (already optimized)", true))
        return true
    }

    /**
     * MASTER 1-CLICK ACTION: Full automated system performance optimization.
     * 1. Extreme 90 FPS Profile (CPU Gold Core 4-7, 90Hz lock, 0.25x instant animation, thermal override 0)
     * 2. Rust Native Daemon (turbodaemon IPC connect & Extreme profile)
     * 3. Surgical Debloat with smart skip (skips already uninstalled)
     * 4. Smart Incremental AOT Compilation (skips already compiled speed apps)
     * 5. Auto-Apply on Boot toggle enabled and synchronized
     * NOTE: Audio subsystem is ISOLATED and NOT touched.
     */
    suspend fun executeMasterOneClickBoost(
        context: Context,
        onLog: (LogEntry) -> Unit
    ): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            onLog(LogEntry("", "Pre-flight Security Check", "FAILED: Shizuku UID 2000 required", false))
            return false
        }

        onLog(LogEntry("", "==================================================", "", true))
        onLog(LogEntry("", "⚡ STARTING MASTER 1-CLICK TURBO BOOST", "Engaging all system performance optimizations", true))
        onLog(LogEntry("", "==================================================", "", true))

        // 1. Extreme 90 FPS Profile & Hardware Clocks
        onLog(LogEntry("", "▶ [1/5] Applying Profile Extreme 90 FPS & Gold Core Affinity...", "MQSAS 21, 90Hz Lock, Thermal Override 0, 0.25x Scale", true))
        applyExtremeGamingRenderMode(context, onLog)

        // 2. Rust Native Daemon IPC
        onLog(LogEntry("", "▶ [2/5] Engaging Rust Native Daemon (turbodaemon)...", "Connecting abstract IPC socket & applying Extreme profile", true))
        DaemonController.ensureDaemonRunning(context)
        DaemonController.setProfile("EXTREME_GAMING_RENDER", context)

        // 3. Surgical Debloat with Smart Skip
        onLog(LogEntry("", "▶ [3/5] Running Surgical Debloat (Smart Skip)...", "Purging persistent bloatware hogs from RAM", true))
        executeDebloat(context, onLog)

        // 4. Smart Incremental AOT Compilation
        onLog(LogEntry("", "▶ [4/5] Running Smart Incremental AOT Compilation...", "Skipping already compiled apps (status=speed)", true))
        executeSmartIncrementalAot(context, onLog)

        // 5. Auto-Apply on Boot
        onLog(LogEntry("", "▶ [5/5] Enabling Auto-Apply on Tablet Boot...", "Persisting boost state across reboots", true))
        setAutoBootEnabled(context, true)

        setActiveProfile(context, BoosterProfile.EXTREME_GAMING_RENDER)
        setTurboActive(context, true)

        onLog(LogEntry("", "==================================================", "", true))
        onLog(LogEntry("", "✅ MASTER 1-CLICK TURBO BOOST COMPLETED!", "Extreme 90FPS Lock, Daemon, Debloat & AOT Speed Active", true))
        onLog(LogEntry("", "==================================================", "", true))
        return true
    }

    /**
     * 1-TAP RESTORE ALL DEBLOATED PACKAGES
     */
    suspend fun restoreDebloatedPackages(
        _context: Context,
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
            // Safe caging: set standby bucket and restriction level without revoking core AppOps that cause crashes
            runStep("cmd activity set-bg-restriction-level --user 0 $pkg restricted_bucket 2>/dev/null || true", "Restrict BG Bucket $pkg", onLog)
            runStep("am set-standby-bucket $pkg rare 2>/dev/null || true", "Set Standby Bucket Rare $pkg", onLog)
            runStep("cmd tare set-vip 0 $pkg false 2>/dev/null || true", "TARE Demote VIP $pkg", onLog)
            runStep("cmd deviceidle whitelist -$pkg 2>/dev/null || true", "Remove DeviceIdle Whitelist $pkg", onLog)
        }
    }
}
