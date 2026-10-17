package com.arya.turbobooster

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arya.turbobooster.booster.DeviceMetrics
import com.arya.turbobooster.booster.LogEntry
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
            } else {
                appendLog("Shizuku permission was denied.")
            }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            updateShizukuBadge()
            appendLog("Shizuku binder received and connected.")
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
    }

    override fun onResume() {
        super.onResume()
        updateShizukuBadge()
        checkAutoRequestShizuku()
        updateModeUI(TurboBoosterManager.isTurboActive(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        telemetryJob?.cancel()
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
        val isTurbo = TurboBoosterManager.isTurboActive(this)
        binding.switchTurboMode.isChecked = isTurbo
        updateModeUI(isTurbo)

        binding.switchAutoBoot.isChecked = TurboBoosterManager.isAutoBootEnabled(this)
        binding.switchAutoBoot.setOnCheckedChangeListener { _, isChecked ->
            TurboBoosterManager.setAutoBootEnabled(this, isChecked)
            appendLog("Auto-Apply on Boot: $isChecked")
        }

        binding.switchTurboMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != TurboBoosterManager.isTurboActive(this)) {
                if (isChecked) applyTurbo() else resetStock()
            }
        }

        binding.btnForceApply.setOnClickListener { applyTurbo() }
        binding.btnRestoreStock.setOnClickListener { resetStock() }

        binding.btnDownscaleGames.setOnClickListener {
            if (!checkPrivilegeOrPrompt()) return@setOnClickListener
            lifecycleScope.launch {
                appendLog("--- Applying 0.7x Game Downscaling ---")
                for ((pkg, name) in TurboBoosterManager.TARGET_GAMES) {
                    val r1 = ShellExecutor.execute("cmd game set --downscale 0.7 $pkg", requirePrivileged = true)
                    val r2 = ShellExecutor.execute("cmd miui.downscale add-debug-app $pkg 0.7", requirePrivileged = true)
                    appendLog("[$name] AOSP: ${r1.isSuccess}, HyperOS: ${r2.isSuccess}")
                }
                appendLog("--- Game downscaling complete ---")
                Toast.makeText(this@MainActivity, "0.7x Games Applied!", Toast.LENGTH_SHORT).show()
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

    private fun updateModeUI(isTurbo: Boolean) {
        if (isTurbo) {
            binding.tvModeSubtitle.text = "ACTIVE • 2.4GHz Locked • Thermal Override 0 • 90Hz"
            binding.tvModeSubtitle.setTextColor(getColor(R.color.md_theme_dark_primary))
            binding.tvThermalState.text = "OVERRIDDEN (Cool/0)"
            binding.tvThermalState.setTextColor(getColor(R.color.accent_green))
        } else {
            binding.tvModeSubtitle.text = "Stock Balanced Mode • Normal Frequencies"
            binding.tvModeSubtitle.setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
            binding.tvThermalState.text = "Dynamic (Stock)"
            binding.tvThermalState.setTextColor(getColor(R.color.md_theme_dark_onSurfaceVariant))
        }
    }

    private fun applyTurbo() {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            binding.switchTurboMode.isChecked = false
            updateModeUI(false)
            showWirelessAdbGuide()
            return
        }

        lifecycleScope.launch {
            binding.switchTurboMode.isEnabled = false
            binding.btnForceApply.isEnabled = false
            appendLog(">>> ENABLING SUPER PERFORMANCE MODE <<<")

            val ok = TurboBoosterManager.applySuperPerformanceMode(this@MainActivity) { entry ->
                runOnUiThread {
                    appendCommandEntry(entry)
                }
            }

            binding.switchTurboMode.isChecked = ok
            updateModeUI(ok)
            binding.switchTurboMode.isEnabled = true
            binding.btnForceApply.isEnabled = true

            if (ok) {
                appendLog(">>> TURBO MODE ACTIVATED SUCCESSFULLY <<<")
                Toast.makeText(this@MainActivity, "⚡ Turbo Mode Activated!", Toast.LENGTH_SHORT).show()
            } else {
                appendLog(">>> Warning: Some commands returned errors. Check log above.")
                Toast.makeText(this@MainActivity, "⚠️ Some commands failed. Check log.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resetStock() {
        if (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission()) {
            binding.switchTurboMode.isChecked = TurboBoosterManager.isTurboActive(this)
            showWirelessAdbGuide()
            return
        }

        lifecycleScope.launch {
            binding.switchTurboMode.isEnabled = false
            binding.btnRestoreStock.isEnabled = false
            appendLog(">>> RESTORING STOCK BALANCED MODE <<<")

            val ok = TurboBoosterManager.applyStockMode(this@MainActivity) { entry ->
                runOnUiThread {
                    appendCommandEntry(entry)
                }
            }

            binding.switchTurboMode.isChecked = !ok
            updateModeUI(!ok)
            binding.switchTurboMode.isEnabled = true
            binding.btnRestoreStock.isEnabled = true

            if (ok) {
                appendLog(">>> STOCK MODE RESTORED SUCCESSFULLY <<<")
                Toast.makeText(this@MainActivity, "↺ Stock Mode Restored", Toast.LENGTH_SHORT).show()
            } else {
                appendLog(">>> Warning: Reset encountered errors.")
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
                val isTurbo = TurboBoosterManager.isTurboActive(this@MainActivity)
                val metrics = DeviceMetrics.fetchLiveMetrics(this@MainActivity, isTurbo)

                withContext(Dispatchers.Main) {
                    binding.tvCpuGold.text = if (metrics.goldCpuMhz > 0) "${metrics.goldCpuMhz} MHz" else "-- MHz"
                    binding.tvCpuSilver.text = if (metrics.silverCpuMhz > 0) "${metrics.silverCpuMhz} MHz" else "-- MHz"
                    binding.tvThermalState.text = metrics.thermalStatus
                    binding.tvRefreshRate.text = "${metrics.refreshRateHz.toInt()} Hz"
                }

                delay(2000)
            }
        }
    }
}
