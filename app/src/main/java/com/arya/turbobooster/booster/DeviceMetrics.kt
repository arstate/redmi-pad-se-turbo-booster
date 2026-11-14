package com.arya.turbobooster.booster

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.io.File

data class LiveMetrics(
    val silverCpuMhz: Int,
    val goldCpuMhz: Int,
    val cpuTempC: Float,
    val thermalStatus: String,
    val refreshRateHz: Float,
    val ramTotalMb: Long,
    val ramAvailableMb: Long,
    val ramUsedMb: Long,
    val ramUsedPercent: Int,
    val zramTotalMb: Long,
    val zramUsedMb: Long,
    val zramFreeMb: Long,
    val zramUsedPercent: Int,
    val isTurboActive: Boolean
)

object DeviceMetrics {
    private const val TAG = "DeviceMetrics"

    fun readCurCpuFreqMhz(cpuIndex: Int): Int {
        return try {
            val file = File("/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/scaling_cur_freq")
            if (file.exists() && file.canRead()) {
                val text = file.readText().trim()
                text.toIntOrNull()?.div(1000) ?: 0
            } else {
                0
            }
        } catch (_: Throwable) {
            0
        }
    }

    fun readCpuTempC(): Float {
        val candidates = listOf(
            "/sys/class/thermal/thermal_zone21/temp", // cpu-therm
            "/sys/class/thermal/thermal_zone20/temp",
            "/sys/class/thermal/thermal_zone14/temp", // mdm-0
            "/sys/class/thermal/thermal_zone0/temp"
        )
        for (path in candidates) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val raw = file.readText().trim().toFloatOrNull() ?: 0f
                    if (raw > 1000f) return raw / 1000f
                    if (raw in 10f..100f) return raw
                }
            } catch (_: Throwable) {}
        }
        return 0f
    }

    fun readMemInfo(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        try {
            val file = File("/proc/meminfo")
            if (file.exists() && file.canRead()) {
                file.forEachLine { line ->
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        val key = line.substring(0, colonIdx).trim()
                        val rest = line.substring(colonIdx + 1).trim()
                        val numStr = rest.split("\\s+".toRegex())[0]
                        val kb = numStr.toLongOrNull() ?: 0L
                        map[key] = kb
                    }
                }
            }
        } catch (_: Throwable) {}
        return map
    }

    suspend fun fetchLiveMetrics(context: Context, isTurboActive: Boolean): LiveMetrics {
        var silver = readCurCpuFreqMhz(0)
        var gold = readCurCpuFreqMhz(4)

        // If direct file read failed (SELinux sandbox), query via ShellExecutor
        if (silver == 0 || gold == 0) {
            val res = ShellExecutor.execute("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq /sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq")
            if (res.isSuccess && res.stdout.isNotBlank()) {
                val lines = res.stdout.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) silver = (lines.getOrNull(0)?.toIntOrNull() ?: 0) / 1000
                if (lines.size > 1) gold = (lines.getOrNull(1)?.toIntOrNull() ?: 0) / 1000
            }
        }

        var cpuTemp = readCpuTempC()
        if (cpuTemp <= 0f) {
            val tempRes = ShellExecutor.execute("cat /sys/class/thermal/thermal_zone21/temp 2>/dev/null || cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null")
            if (tempRes.isSuccess && tempRes.stdout.isNotBlank()) {
                val raw = tempRes.stdout.trim().toFloatOrNull() ?: 0f
                cpuTemp = if (raw > 1000f) raw / 1000f else raw
            }
        }

        // Memory metrics from /proc/meminfo with ActivityManager fallback
        var memMap = readMemInfo()
        if (memMap.isEmpty()) {
            val res = ShellExecutor.execute("cat /proc/meminfo")
            if (res.isSuccess && res.stdout.isNotBlank()) {
                memMap = mutableMapOf()
                res.stdout.lines().forEach { line ->
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        val key = line.substring(0, colonIdx).trim()
                        val rest = line.substring(colonIdx + 1).trim()
                        val numStr = rest.split("\\s+".toRegex())[0]
                        val kb = numStr.toLongOrNull() ?: 0L
                        memMap[key] = kb
                    }
                }
            }
        }

        val totalKb = memMap["MemTotal"] ?: 0L
        val availKb = memMap["MemAvailable"] ?: (memMap["MemFree"] ?: 0L)

        val ramTotalMb = if (totalKb > 0L) totalKb / 1024L else {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            (mi.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
        }
        val ramAvailMb = if (availKb > 0L) availKb / 1024L else {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            mi.availMem / (1024L * 1024L)
        }
        val ramUsedMb = (ramTotalMb - ramAvailMb).coerceAtLeast(0L)
        val ramUsedPercent = if (ramTotalMb > 0L) ((ramUsedMb.toDouble() / ramTotalMb.toDouble()) * 100).toInt() else 0

        val swapTotalKb = memMap["SwapTotal"] ?: 0L
        val swapFreeKb = memMap["SwapFree"] ?: 0L
        val swapUsedKb = (swapTotalKb - swapFreeKb).coerceAtLeast(0L)
        val zramTotalMb = swapTotalKb / 1024L
        val zramUsedMb = swapUsedKb / 1024L
        val zramFreeMb = swapFreeKb / 1024L
        val zramUsedPercent = if (zramTotalMb > 0L) ((zramUsedMb.toDouble() / zramTotalMb.toDouble()) * 100).toInt() else 0

        val refreshRate = try {
            val peak = android.provider.Settings.System.getFloat(context.contentResolver, "peak_refresh_rate", 90f)
            if (peak >= 89f) {
                90f
            } else {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                val display = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                display?.refreshRate ?: peak
            }
        } catch (_: Throwable) {
            90f
        }

        val thermalStatus = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                val status = pm?.currentThermalStatus ?: 0
                val desc = when (status) {
                    android.os.PowerManager.THERMAL_STATUS_NONE -> "Cool (0)"
                    android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Light (1)"
                    android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Moderate (2)"
                    android.os.PowerManager.THERMAL_STATUS_SEVERE -> "Severe (3)"
                    android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "Critical (4)"
                    android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency (5)"
                    android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown (6)"
                    else -> "Status $status"
                }
                if (isTurboActive) "OVERRIDDEN ($desc)" else "Dynamic ($desc)"
            } else {
                if (isTurboActive) "OVERRIDDEN (Cool/0)" else "Dynamic (Stock)"
            }
        } catch (_: Throwable) {
            if (isTurboActive) "OVERRIDDEN (Cool/0)" else "Dynamic (Stock)"
        }

        return LiveMetrics(
            silverCpuMhz = silver,
            goldCpuMhz = gold,
            cpuTempC = cpuTemp,
            thermalStatus = thermalStatus,
            refreshRateHz = refreshRate,
            ramTotalMb = ramTotalMb,
            ramAvailableMb = ramAvailMb,
            ramUsedMb = ramUsedMb,
            ramUsedPercent = ramUsedPercent,
            zramTotalMb = zramTotalMb,
            zramUsedMb = zramUsedMb,
            zramFreeMb = zramFreeMb,
            zramUsedPercent = zramUsedPercent,
            isTurboActive = isTurboActive
        )
    }
}
