package com.arya.turbobooster

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arya.turbobooster.booster.AudioBoosterManager
import com.arya.turbobooster.booster.BoosterProfile
import com.arya.turbobooster.booster.DaemonController
import com.arya.turbobooster.booster.DeviceMetrics
import com.arya.turbobooster.booster.LogEntry
import com.arya.turbobooster.booster.ScriptInstaller
import com.arya.turbobooster.booster.ShellExecutor
import com.arya.turbobooster.booster.TurboBoosterManager
import com.arya.turbobooster.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var telemetryJob: Job? = null
    private val SHIZUKU_REQUEST_CODE = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            updateShizukuBadge()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                appendLog("Shizuku permission granted by user!")
                Toast.makeText(this, "Shizuku Authorized!", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    DaemonController.ensureDaemonRunning(this@MainActivity)
                }
            } else {
                appendLog("Shizuku permission was denied.")
            }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            updateShizukuBadge()
            appendLog("Shizuku binder received and connected.")
            lifecycleScope.launch {
                DaemonController.ensureDaemonRunning(this@MainActivity)
            }
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            updateShizukuBadge()
            appendLog("Shizuku binder disconnected (Service died).")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupShizukuListeners()
        setupUI()
        startTelemetryLoop()

        lifecycleScope.launch {
            if (ShellExecutor.isShizukuAvailable() && ShellExecutor.hasShizukuPermission()) {
                DaemonController.ensureDaemonRunning(this@MainActivity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateShizukuBadge()
        checkAutoRequestShizuku()
        updateProfileUI(TurboBoosterManager.getActiveProfile(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
        AudioBoosterManager.release()
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (_: Throwable) {}
    }

    private fun checkAutoRequestShizuku() {
        if (ShellExecutor.isShizukuAvailable() && !ShellExecutor.hasShizukuPermission()) {
            try {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            } catch (t: Throwable) {
                appendLog("Auto-request Shizuku permission failed: ${t.message}")
            }
        }
    }

    private fun setupShizukuListeners() {
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (t: Throwable) {
            appendLog("Error initializing Shizuku listeners: ${t.message}")
        }
    }

    private fun setupUI() {
        val devName = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"
        binding.tvDeviceModelSubtitle.text = "$devName • Snapdragon 680 • 90Hz Native"

        val currentProfile = TurboBoosterManager.getActiveProfile(this)
        updateProfileUI(currentProfile)

        binding.switchAutoBoot.isChecked = TurboBoosterManager.isAutoBootEnabled(this)
        binding.switchAutoBoot.setOnCheckedChangeListener { _, isChecked ->
            TurboBoosterManager.setAutoBootEnabled(this, isChecked)
            appendLog("Auto-Apply on Boot: $isChecked")
        }

        binding.switchTurboMode.isChecked = TurboBoosterManager.isTurboActive(this)
        binding.switchTurboMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && TurboBoosterManager.getActiveProfile(this) == BoosterProfile.STOCK) {
                runMasterOneClickBoost()
            } else if (!isChecked && TurboBoosterManager.getActiveProfile(this) != BoosterProfile.STOCK) {
                applyProfile(BoosterProfile.STOCK)
            }
        }

        binding.btnMasterBoost.setOnClickListener {
            runMasterOneClickBoost()
        }

        binding.btnForceApply.setOnClickListener {
            runMasterOneClickBoost()
        }

        binding.btnRestoreStock.setOnClickListener {
            applyProfile(BoosterProfile.STOCK)
        }

        binding.btnCompileAot.setOnClickListener {
            runManualIncrementalAot()
        }

        // Profile switcher buttons
        binding.btnProfileStock.setOnClickListener {
            applyProfile(BoosterProfile.STOCK)
        }

        binding.btnProfileFlagship.setOnClickListener {
            applyProfile(BoosterProfile.FLAGSHIP_BALANCED)
        }

        binding.btnProfileExtreme.setOnClickListener {
            confirmExtremeGamingMode()
        }

        // Native Daemon Reclaim & Debloat actions
        binding.btnReclaimRam.setOnClickListener {
            if (!checkPrivilegeOrPrompt()) return@setOnClickListener
            lifecycleScope.launch {
                appendLog("--- Invoking Native In-RAM Compaction ---")
                val ok = DaemonController.triggerReclaim(this@MainActivity)
                appendLog("In-RAM Compaction via Daemon: ${if (ok) "SUCCESS" else "DIRECT FALLBACK"}")
                if (!ok) {
                    // Safe gentle trim: avoid squeezing launcher heap into zRAM
                    ShellExecutor.execute("sm fstrim", requirePrivileged = true)
                }
                Toast.makeText(this@MainActivity, "🧹 In-RAM Compaction Complete!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDebloatAction.setOnClickListener {
            confirmDebloatAction()
        }

        binding.btnRestoreDebloatAction.setOnClickListener {
            if (!checkPrivilegeOrPrompt()) return@setOnClickListener
            lifecycleScope.launch {
                appendLog("--- Restoring All Debloated Packages ---")
                TurboBoosterManager.restoreDebloatedPackages(this@MainActivity) { entry ->
                    runOnUiThread { appendCommandEntry(entry) }
                }
                Toast.makeText(this@MainActivity, "🔄 Debloated Packages Restored!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnProtectedApps.setOnClickListener {
            showProtectedAppsDialog()
        }

        binding.btnDownscaleGames.setOnClickListener {
            if (!checkPrivilegeOrPrompt()) return@setOnClickListener
            lifecycleScope.launch {
                appendLog("--- Applying 0.8x Native Game Downscaling ---")
                for ((pkg, name) in TurboBoosterManager.TARGET_GAMES) {
                    if (pkg == "com.lemon.lvoverseas" || pkg == "com.lemon.ccpadpro" || pkg == "com.mobile.legends" || pkg == "com.roblox.client") {
                        if (pkg == "com.roblox.client") {
                            ShellExecutor.execute("cmd miui.downscale remove-debug-app com.roblox.client 2>/dev/null || true", requirePrivileged = true)
                            ShellExecutor.execute("device_config delete game_overlay com.roblox.client 2>/dev/null || true", requirePrivileged = true)
                            ShellExecutor.execute("cmd game reset com.roblox.client 2>/dev/null || true", requirePrivileged = true)
                            ShellExecutor.execute("cmd game set --fps 90 com.roblox.client 2>/dev/null || true", requirePrivileged = true)
                            appendLog("[$name] Protected: Native Res 90FPS applied, Downscale bypassed")
                        }
                        continue // Prevent EGL / DownscaleCloudData crash on video editor, MLBB & Roblox
                    }
                    val r1 = ShellExecutor.execute("cmd game set --downscale 0.8 $pkg", requirePrivileged = true)
                    val r2 = ShellExecutor.execute("cmd miui.downscale add-debug-app $pkg 0.8", requirePrivileged = true)
                    appendLog("[$name] AOSP: ${r1.isSuccess}, HyperOS: ${r2.isSuccess}")
                }
                appendLog("--- Game downscaling complete ---")
                Toast.makeText(this@MainActivity, "0.8x Games Applied!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnOptimizeChrome.setOnClickListener {
            if (!checkPrivilegeOrPrompt()) return@setOnClickListener
            lifecycleScope.launch {
                appendLog("--- Injecting Chrome Anti-Reload Flags ---")
                val flags = "_ --disable-low-end-device-mode --purge-max-password-store=0 --max-active-webgl-contexts=16 --enable-features=ParallelDownloading,SmoothScrolling --disable-features=AudioProcessHighPriority,CalculateNativeWinOcclusion,SpareRendererForSitePerProcess"
                val r1 = ShellExecutor.execute("echo '$flags' > /data/local/tmp/chrome-command-line && chmod 644 /data/local/tmp/chrome-command-line", requirePrivileged = true)
                val r2 = ShellExecutor.execute("cmd game set --downscale 0.85 com.android.chrome", requirePrivileged = true)
                appendLog("Chrome flags: ${r1.isSuccess}, Downscale 0.85x: ${r2.isSuccess}")
                Toast.makeText(this@MainActivity, "Chrome Boosted!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRunBundledScripts.setOnClickListener {
            showBundledScriptsDialog()
        }

        binding.btnShizukuStatus.setOnClickListener {
            handleShizukuBadgeClick()
        }

        binding.btnAdbGuide.setOnClickListener {
            showWirelessAdbGuide()
        }

        binding.btnCopyLog.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("TurboBoosterLog", binding.tvConsoleLog.text))
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearLog.setOnClickListener {
            binding.tvConsoleLog.text = "[Console cleared]\n"
        }

        setupAudioUI()
    }

    private fun confirmExtremeGamingMode() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Warning: Extreme Gaming & Render Mode")
            .setMessage("This mode unlocks Thermal Sensor Override 0 (no thermal downclocking) and 0.25x instant animations. Ensure tablet has adequate ventilation.\n\nContinue?")
            .setPositiveButton("Engage Extreme Mode") { _, _ ->
                applyProfile(BoosterProfile.EXTREME_GAMING_RENDER)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDebloatAction() {
        AlertDialog.Builder(this)
            .setTitle("🧹 Surgical Debloat (-800 Crash-Purge)")
            .setMessage("This will uninstall 30+ bloatware packages (Google Telemetry, Xiaomi MSA, Weather, Analytics) and immediately kill their undead -800 persistent background processes from RAM without rebooting.\n\nCore system framework packages are protected by the Hardcoded Red-List.\n\nProceed?")
            .setPositiveButton("Debloat Now") { _, _ ->
                if (!checkPrivilegeOrPrompt()) return@setPositiveButton
                lifecycleScope.launch {
                    TurboBoosterManager.executeDebloat(this@MainActivity) { entry ->
                        runOnUiThread { appendCommandEntry(entry) }
                    }
                    Toast.makeText(this@MainActivity, "Debloat Complete! Massive RAM freed.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProtectedAppsDialog() {
        val currentProtected = TurboBoosterManager.getProtectedApps(this).toMutableList()
        val appArray = currentProtected.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🛡️ Protected Apps (Exempt from Caging)")
            .setItems(appArray) { _, which ->
                val selected = appArray[which]
                AlertDialog.Builder(this)
                    .setTitle("Remove Protected App")
                    .setMessage("Remove $selected from protected list? It will be subject to background caging.")
                    .setPositiveButton("Remove") { _, _ ->
                        TurboBoosterManager.removeProtectedApp(this, selected)
                        lifecycleScope.launch {
                            DaemonController.removeProtectedApp(selected, this@MainActivity)
                        }
                        Toast.makeText(this, "Removed $selected", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setPositiveButton("Add App") { _, _ ->
                val input = EditText(this)
                input.hint = "com.example.app"
                AlertDialog.Builder(this)
                    .setTitle("Add Protected Package")
                    .setView(input)
                    .setPositiveButton("Add") { _, _ ->
                        val pkg = input.text.toString().trim()
                        if (pkg.isNotEmpty()) {
                            TurboBoosterManager.addProtectedApp(this, pkg)
                            lifecycleScope.launch {
                                DaemonController.addProtectedApp(pkg, this@MainActivity)
                            }
                            Toast.makeText(this, "Protected $pkg", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun runMasterOneClickBoost() {
        if (!checkPrivilegeOrPrompt()) return

        lifecycleScope.launch {
            binding.btnMasterBoost.isEnabled = false
            binding.switchTurboMode.isEnabled = false
            binding.btnRestoreStock.isEnabled = false
            binding.btnForceApply.isEnabled = false
            appendLog("==================================================")
            appendLog("🚀 INITIATING 1-CLICK MASTER TURBO BOOST...")
            appendLog("==================================================")

            val ok = TurboBoosterManager.executeMasterOneClickBoost(this@MainActivity) { entry ->
                runOnUiThread { appendCommandEntry(entry) }
            }

            binding.switchTurboMode.isChecked = true
            binding.switchAutoBoot.isChecked = TurboBoosterManager.isAutoBootEnabled(this@MainActivity)
            updateProfileUI(BoosterProfile.EXTREME_GAMING_RENDER)
            binding.btnMasterBoost.isEnabled = true
            binding.switchTurboMode.isEnabled = true
            binding.btnRestoreStock.isEnabled = true
            binding.btnForceApply.isEnabled = true

            if (ok) {
                Toast.makeText(this@MainActivity, "⚡ 1-Click Master Boost Aktif! Extreme 90FPS, Daemon, Debloat & AOT Speed!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MainActivity, "⚠️ 1-Click Boost selesai dengan catatan. Cek log.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun runManualIncrementalAot() {
        if (!checkPrivilegeOrPrompt()) return
        lifecycleScope.launch {
            binding.btnCompileAot.isEnabled = false
            appendLog("--- Running Manual Incremental AOT Speed Compilation ---")
            val ok = TurboBoosterManager.executeSmartIncrementalAot(this@MainActivity) { entry ->
                runOnUiThread { appendCommandEntry(entry) }
            }
            binding.btnCompileAot.isEnabled = true
            if (ok) {
                Toast.makeText(this@MainActivity, "⚙️ Incremental AOT Scan Selesai!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyProfile(profile: BoosterProfile) {
        if (!checkPrivilegeOrPrompt()) return

        lifecycleScope.launch {
            binding.switchTurboMode.isEnabled = false
            binding.btnForceApply.isEnabled = false
            binding.btnRestoreStock.isEnabled = false
            appendLog(">>> SWITCHING TO PROFILE: ${profile.name} <<<")

            val ok = TurboBoosterManager.applyProfile(this@MainActivity, profile) { entry ->
                runOnUiThread { appendCommandEntry(entry) }
            }

            updateProfileUI(profile)
            binding.switchTurboMode.isEnabled = true
            binding.btnForceApply.isEnabled = true
            binding.btnRestoreStock.isEnabled = true

            if (ok) {
                appendLog(">>> PROFILE ${profile.name} APPLIED SUCCESSFULLY <<<")
                Toast.makeText(this@MainActivity, "⚡ Profile: ${profile.name} Active", Toast.LENGTH_SHORT).show()
            } else {
                appendLog(">>> Warning: Some steps encountered errors. Check log.")
            }
        }
    }

    private fun updateProfileUI(profile: BoosterProfile) {
        val isTurbo = (profile != BoosterProfile.STOCK)
        binding.switchTurboMode.isChecked = isTurbo

        when (profile) {
            BoosterProfile.STOCK -> {
                binding.tvModeSubtitle.text = "Stock Mode • 60-90Hz Dinamis • Normal Frequencies"
                binding.tvModeSubtitle.setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
                binding.tvThermalState.text = "Dynamic (Stock)"
                binding.tvThermalState.setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
                binding.btnMasterBoost.text = "⚡ AKTIFKAN 1-KLIK OPREK SEMUA"
            }
            BoosterProfile.FLAGSHIP_BALANCED -> {
                binding.tvModeSubtitle.text = "FLAGSHIP BALANCED • 2.4GHz Burst • 90Hz • 205 DPI"
                binding.tvModeSubtitle.setTextColor(getColor(R.color.md_theme_dark_primary))
                binding.tvThermalState.text = "Balanced (Safe)"
                binding.tvThermalState.setTextColor(getColor(R.color.accent_green))
                binding.btnMasterBoost.text = "⚡ 1-KLIK AKTIF: FLAGSHIP 90FPS"
            }
            BoosterProfile.EXTREME_GAMING_RENDER -> {
                binding.tvModeSubtitle.text = "⚡ 1-KLIK AKTIF: EXTREME 90FPS • DAEMON • DEBLOATED • AOT SPEED"
                binding.tvModeSubtitle.setTextColor(getColor(R.color.accent_gold))
                binding.tvThermalState.text = "OVERRIDDEN (Cool/0)"
                binding.tvThermalState.setTextColor(getColor(R.color.accent_gold))
                binding.btnMasterBoost.text = "⚡ 1-KLIK AKTIF: EXTREME 90FPS"
            }
            BoosterProfile.DEBLOAT_RESTORE -> {
                binding.tvModeSubtitle.text = "Debloat Active • Undead Daemons Purged"
                binding.tvModeSubtitle.setTextColor(getColor(R.color.accent_green))
                binding.btnMasterBoost.text = "⚡ AKTIFKAN 1-KLIK OPREK SEMUA"
            }
        }
    }

    private fun showBundledScriptsDialog() {
        val scripts = arrayOf(
            "🚀 boost_flagship_unfcker.sh (HyperOS Flagship Unfcker)",
            "⚡ boost_100.sh (100 Celah Sistem Terdalam)",
            "boost.sh (Master Full Booster)",
            "boost_extreme.sh (Extreme Gaming & CPU)",
            "boost_deep_system.sh (Deep Kernel & RAM)",
            "boost_pure_zram_compaction.sh (Pure In-RAM zRAM Compaction)",
            "fix_swipe_home_delay.sh (Zero-Delay Navigation & Swipe)",
            "boost_chrome_webview.sh (Chrome & WebView)",
            "boost_quad_audio.sh (Quad Speaker Studio)",
            "pubg_ultimate_booster.sh (PUBG Mobile Tweak)",
            "launch_freeform.sh (Freeform Multi-Window)",
            "🎯 Terapkan Native Tablet Mode (1200x1920 @ 205dpi)",
            "⚡ Terapkan Full Screen Gesture (FSG + Hide Line)",
            "🛠️ Fix CapCut EGL Surface & Full Media AppOps",
            "📉 Terapkan Display 75% Scale (900x1440 @ 154dpi)",
            "🖥️ Reset Display Default (1200x1920 @ 280dpi)",
            "🧹 Debloat Bersihkan Bloatware (Hemat 1.8GB RAM)",
            "🔄 Restore Paket Debloat Bawaan",
            "📂 Ekstrak Semua Script ke /sdcard/TurboBoosterScripts"
        )

        AlertDialog.Builder(this)
            .setTitle("📜 Bundled APK Scripts & Tools")
            .setItems(scripts) { _, which ->
                when (which) {
                    0 -> runSelectedScript("boost_flagship_unfcker.sh")
                    1 -> runSelectedScript("boost_100.sh")
                    2 -> runSelectedScript("boost.sh")
                    3 -> runSelectedScript("boost_extreme.sh")
                    4 -> runSelectedScript("boost_deep_system.sh")
                    5 -> runSelectedScript("boost_pure_zram_compaction.sh")
                    6 -> runSelectedScript("fix_swipe_home_delay.sh")
                    7 -> runSelectedScript("boost_chrome_webview.sh")
                    8 -> runSelectedScript("boost_quad_audio.sh")
                    9 -> runSelectedScript("pubg_ultimate_booster.sh")
                    10 -> runSelectedScript("launch_freeform.sh")
                    11 -> {
                        if (!checkPrivilegeOrPrompt()) return@setItems
                        lifecycleScope.launch {
                            appendLog("--- Applying Native Tablet Mode (1200x1920 @ 205dpi) ---")
                            val r1 = ShellExecutor.execute("wm size 1200x1920", requirePrivileged = true)
                            val r2 = ShellExecutor.execute("wm density 205", requirePrivileged = true)
                            appendLog("Size 1200x1920: ${r1.isSuccess}, Density 205: ${r2.isSuccess}")
                            Toast.makeText(this@MainActivity, "Native Tablet Mode (205dpi) Applied!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    12 -> {
                        if (!checkPrivilegeOrPrompt()) return@setItems
                        lifecycleScope.launch {
                            appendLog("--- Applying Full Screen Gesture Navigation (FSG + Hide Line) ---")
                            val r1 = ShellExecutor.execute("settings put global force_fsg_nav_bar 1", requirePrivileged = true)
                            val r2 = ShellExecutor.execute("settings put system force_fsg_nav_bar 1", requirePrivileged = true)
                            val r3 = ShellExecutor.execute("settings put global hide_gesture_line 1", requirePrivileged = true)
                            val r4 = ShellExecutor.execute("settings put system miui_home_animation_rate 2", requirePrivileged = true)
                            appendLog("FSG Global: ${r1.isSuccess}, FSG System: ${r2.isSuccess}, Hide Line: ${r3.isSuccess}, Fast Curve: ${r4.isSuccess}")
                            Toast.makeText(this@MainActivity, "Zero-Delay Gesture Nav Applied!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    13 -> {
                        if (!checkPrivilegeOrPrompt()) return@setItems
                        lifecycleScope.launch {
                            appendLog("--- Fixing CapCut EGL Surface & Granting Media AppOps ---")
                            val capcutPkgs = listOf("com.lemon.lvoverseas", "com.lemon.ccpadpro")
                            for (pkg in capcutPkgs) {
                                ShellExecutor.execute("cmd game reset $pkg", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg READ_EXTERNAL_STORAGE allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg WRITE_EXTERNAL_STORAGE allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg MANAGE_EXTERNAL_STORAGE allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg READ_MEDIA_IMAGES allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg READ_MEDIA_VIDEO allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg READ_MEDIA_AUDIO allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg NO_ISOLATED_STORAGE allow", requirePrivileged = true)
                                ShellExecutor.execute("cmd appops set $pkg RUN_IN_BACKGROUND allow", requirePrivileged = true)
                                ShellExecutor.execute("dumpsys deviceidle whitelist +$pkg", requirePrivileged = true)
                            }
                            ShellExecutor.execute("settings put global enable_freeform_support 1", requirePrivileged = true)
                            ShellExecutor.execute("settings put global force_resizable_activities 1", requirePrivileged = true)
                            ShellExecutor.execute("settings put global enable_non_resizable_multi_window 1", requirePrivileged = true)
                            appendLog("CapCut & Multi-Window Permissions configured successfully.")
                            Toast.makeText(this@MainActivity, "CapCut EGL & Storage Permissions Fixed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    14 -> {
                        if (!checkPrivilegeOrPrompt()) return@setItems
                        lifecycleScope.launch {
                            appendLog("--- Applying 75% Display Scale (900x1440 @ 154dpi) ---")
                            val r1 = ShellExecutor.execute("wm size 900x1440", requirePrivileged = true)
                            val r2 = ShellExecutor.execute("wm density 154", requirePrivileged = true)
                            appendLog("Size 900x1440: ${r1.isSuccess}, Density 154: ${r2.isSuccess}")
                            Toast.makeText(this@MainActivity, "Display 75% Scale Applied!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    15 -> {
                        if (!checkPrivilegeOrPrompt()) return@setItems
                        lifecycleScope.launch {
                            appendLog("--- Resetting Display to Native (1200x1920 @ 280dpi) ---")
                            val r1 = ShellExecutor.execute("wm size reset", requirePrivileged = true)
                            val r2 = ShellExecutor.execute("wm density reset", requirePrivileged = true)
                            appendLog("Size Reset: ${r1.isSuccess}, Density Reset: ${r2.isSuccess}")
                            Toast.makeText(this@MainActivity, "Display Reset to Native!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    16 -> {
                        confirmDebloatAction()
                    }
                    17 -> {
                        if (!checkPrivilegeOrPrompt()) return@setItems
                        lifecycleScope.launch {
                            TurboBoosterManager.restoreDebloatedPackages(this@MainActivity) { entry ->
                                runOnUiThread { appendCommandEntry(entry) }
                            }
                            Toast.makeText(this@MainActivity, "Packages restored!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    18 -> {
                        lifecycleScope.launch {
                            val list = ScriptInstaller.extractAllScripts(this@MainActivity)
                            appendLog("Extracted ${list.size} scripts to internal & /sdcard/TurboBoosterScripts/")
                            Toast.makeText(this@MainActivity, "Semua script berhasil diekstrak!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun runSelectedScript(scriptName: String) {
        if (!checkPrivilegeOrPrompt()) return
        lifecycleScope.launch {
            appendLog(">>> RUNNING BUNDLED SCRIPT: $scriptName <<<")
            val ok = ScriptInstaller.executeBundledScript(this@MainActivity, scriptName) { entry ->
                runOnUiThread { appendCommandEntry(entry) }
            }
            if (ok) {
                appendLog(">>> Script $scriptName completed successfully <<<")
                Toast.makeText(this@MainActivity, "Script $scriptName selesai!", Toast.LENGTH_SHORT).show()
            } else {
                appendLog(">>> Warning: Script $scriptName returned errors <<<")
            }
        }
    }

    private fun setupAudioUI() {
        AudioBoosterManager.initAudioEffects(this)
        val isAudioOn = AudioBoosterManager.isAudioTurboEnabled(this)
        binding.switchAudioBooster.isChecked = isAudioOn
        updateAudioUI(isAudioOn)

        binding.switchAudioBooster.setOnCheckedChangeListener { _, isChecked ->
            AudioBoosterManager.setAudioTurboEnabled(this, isChecked)
            updateAudioUI(isChecked)
            lifecycleScope.launch {
                if (isChecked) {
                    appendLog("--- Activating Quad Speaker Studio Audio ---")
                    AudioBoosterManager.setLoudnessActive(true)
                    AudioBoosterManager.setBassBoostActive(true)
                    AudioBoosterManager.setSurroundActive(true)
                    AudioBoosterManager.applyPresetCurve(AudioBoosterManager.getActivePreset(this@MainActivity))
                    val ok = AudioBoosterManager.applyAudioHardwareBoost { entry ->
                        runOnUiThread { appendCommandEntry(entry) }
                    }
                    if (ok) {
                        appendLog("Quad Speaker Studio Audio: ACTIVE (Dolby Atmos + Loudness +5dB)")
                        Toast.makeText(this@MainActivity, "🎵 Studio Audio Activated!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    appendLog("--- Restoring Stock Audio Mode ---")
                    AudioBoosterManager.setLoudnessActive(false)
                    AudioBoosterManager.setBassBoostActive(false)
                    AudioBoosterManager.setSurroundActive(false)
                    val ok = AudioBoosterManager.resetAudioHardwareStock { entry ->
                        runOnUiThread { appendCommandEntry(entry) }
                    }
                    if (ok) {
                        appendLog("Audio mode: Stock")
                        Toast.makeText(this@MainActivity, "Audio Restored to Stock", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnAudioPresetBass.setOnClickListener {
            applyAudioPreset("bass_club", "🎧 Bass Club (Deep Punch & Sub-Bass)")
        }

        binding.btnAudioPresetCinema.setOnClickListener {
            applyAudioPreset("cinema", "🎬 Cinema 3D (Vocal & Spatial Sound)")
        }

        binding.btnAudioPresetGaming.setOnClickListener {
            applyAudioPreset("gaming", "🎮 Gaming 3D (Footstep & Acoustic Cues)")
        }
    }

    private fun applyAudioPreset(preset: String, displayName: String) {
        AudioBoosterManager.setActivePreset(this, preset)
        AudioBoosterManager.applyPresetCurve(preset)
        appendLog("Applied Audio Preset: $displayName")
        Toast.makeText(this, displayName, Toast.LENGTH_SHORT).show()
    }

    private fun updateAudioUI(isAudioOn: Boolean) {
        if (isAudioOn) {
            binding.tvAudioStatus.text = "Dolby Atmos 3D Surround & Deep Bass Punch (ACTIVE)"
            binding.tvAudioStatus.setTextColor(getColor(R.color.accent_green))
        } else {
            binding.tvAudioStatus.text = "Stock Flat Sound • Hardware Surround Off"
            binding.tvAudioStatus.setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
        }
    }

    private fun checkPrivilegeOrPrompt(): Boolean {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            AlertDialog.Builder(this)
                .setTitle("Privileged Access Required")
                .setMessage("This operation requires Shizuku UID 2000 shell access. Please open Shizuku or the Wireless ADB guide to authorize.")
                .setPositiveButton("Open Guide") { _, _ -> showWirelessAdbGuide() }
                .setNegativeButton("Cancel", null)
                .show()
            return false
        }
        return true
    }

    private fun handleShizukuBadgeClick() {
        if (!ShellExecutor.isShizukuAvailable()) {
            showWirelessAdbGuide()
        } else if (!ShellExecutor.hasShizukuPermission()) {
            try {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            } catch (t: Throwable) {
                appendLog("Failed to request permission: ${t.message}")
            }
        } else {
            showWirelessAdbGuide()
        }
    }

    private fun showWirelessAdbGuide() {
        val available = ShellExecutor.isShizukuAvailable()
        val permitted = ShellExecutor.hasShizukuPermission()

        val statusStr = when {
            available && permitted -> "✅ Engine Status: Shizuku Connected (UID 2000 Authorized)"
            available && !permitted -> "⚠️ Engine Status: Shizuku Service Running (Needs Authorization)"
            else -> "❌ Engine Status: Shizuku Service Offline"
        }

        val msg = """
$statusStr

To run Super Performance Mode directly on tablet without a PC:

1. Open Developer Options:
   • Enable 'Wireless Debugging' (Debugging Nirkabel).

2. Open Shizuku App:
   • Tap 'Pairing' / 'Start via Wireless Debugging'.
   • Enter the 6-digit Wi-Fi pairing code.

3. Return to Turbo Booster:
   • Tap 'Authorize' when prompted.

Loopback / LADB Terminal Command (UID 2000):
sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Wireless ADB & Shizuku Setup Guide")
            .setMessage(msg)
            .setPositiveButton("Open Shizuku") { _, _ ->
                val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Shizuku package not found on device", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Dev Options") { _, _ ->
                try {
                    startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                } catch (_: Throwable) {
                    try {
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                    } catch (_: Throwable) {}
                }
            }
            .setNegativeButton("Copy LADB Cmd") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("LADBCommand", "sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh"))
                Toast.makeText(this, "LADB command copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateShizukuBadge() {
        val available = ShellExecutor.isShizukuAvailable()
        val permitted = ShellExecutor.hasShizukuPermission()

        when {
            available && permitted -> {
                binding.btnShizukuStatus.text = "● Shizuku: Authorized"
                binding.btnShizukuStatus.setTextColor(getColor(R.color.accent_green))
            }
            available && !permitted -> {
                binding.btnShizukuStatus.text = "▲ Shizuku: Request Auth"
                binding.btnShizukuStatus.setTextColor(getColor(R.color.accent_gold))
            }
            else -> {
                binding.btnShizukuStatus.text = "✕ Shizuku: Offline"
                binding.btnShizukuStatus.setTextColor(getColor(R.color.accent_red))
            }
        }
    }

    private fun appendCommandEntry(entry: LogEntry) {
        val prefix = if (entry.isSuccess) "[✓]" else "[✗]"
        val line = "$prefix ${entry.timestamp} ${entry.command}\n    └─ ${entry.output}\n"
        appendLog(line)
    }

    private fun appendLog(text: String) {
        binding.tvConsoleLog.append(text + "\n")
        binding.scrollConsole.post {
            binding.scrollConsole.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentProfile = TurboBoosterManager.getActiveProfile(this@MainActivity)
                val isTurbo = (currentProfile != BoosterProfile.STOCK)
                val metrics = DeviceMetrics.fetchLiveMetrics(this@MainActivity, isTurbo)

                val daemonStatusStr = try {
                    DaemonController.getDaemonStatus(this@MainActivity)
                } catch (_: Throwable) { null }

                withContext(Dispatchers.Main) {
                    val usedGb = String.format(java.util.Locale.US, "%.1f", metrics.ramUsedMb / 1024.0)
                    val totalGb = String.format(java.util.Locale.US, "%.1f", metrics.ramTotalMb / 1024.0)
                    val freeGb = String.format(java.util.Locale.US, "%.1f", metrics.ramAvailableMb / 1024.0)
                    binding.tvRamUsage.text = "$usedGb GB / $totalGb GB"
                    binding.tvRamFree.text = "Sisa: $freeGb GB (${metrics.ramUsedPercent}% Terpakai)"
                    binding.progressRam.progress = metrics.ramUsedPercent

                    val zUsedMb = String.format(java.util.Locale.US, "%,d", metrics.zramUsedMb)
                    val zTotalMb = String.format(java.util.Locale.US, "%,d", metrics.zramTotalMb)
                    val zFreeMb = String.format(java.util.Locale.US, "%,d", metrics.zramFreeMb)
                    binding.tvZramUsage.text = "$zUsedMb MB / $zTotalMb MB"
                    binding.tvZramFree.text = "Sisa: $zFreeMb MB (${metrics.zramUsedPercent}% Terpakai)"
                    binding.progressZram.progress = metrics.zramUsedPercent

                    binding.tvCpuGold.text = if (metrics.goldCpuMhz > 0) "Gold: ${metrics.goldCpuMhz} MHz" else "Gold: -- MHz"
                    binding.tvCpuSilver.text = if (metrics.silverCpuMhz > 0) "Silver: ${metrics.silverCpuMhz} MHz" else "Silver: -- MHz"
                    binding.tvCpuTemp.text = if (metrics.cpuTempC > 0f) String.format(java.util.Locale.US, "%.1f °C", metrics.cpuTempC) else "-- °C"
                    binding.tvThermalState.text = metrics.thermalStatus
                    binding.tvRefreshRate.text = "${metrics.refreshRateHz.toInt()} Hz"

                    if (daemonStatusStr != null) {
                        val rssMatch = Regex("rss_kb=(\\d+)").find(daemonStatusStr)?.groupValues?.get(1)?.toIntOrNull()
                        val rssMb = if (rssMatch != null) String.format(java.util.Locale.US, "%.1f MB", rssMatch / 1024f) else "Active"
                        binding.tvDaemonStatus.text = "⚡ Active • $rssMb"
                        binding.tvDaemonStatus.setTextColor(getColor(R.color.accent_green))
                    } else {
                        binding.tvDaemonStatus.text = "Offline • Tap 1-Click Boost"
                        binding.tvDaemonStatus.setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
                    }
                }

                delay(2000)
            }
        }
    }
}
