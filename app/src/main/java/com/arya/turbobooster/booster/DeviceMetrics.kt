package com.arya.turbobooster.booster

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import java.io.File

data class LiveMetrics(
    val silverCpuMhz: Int,
    val goldCpuMhz: Int,
    val thermalStatus: String,
    val refreshRateHz: Float,
    val ramAvailableMb: Long,
    val ramTotalMb: Long,
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
        } catch (t: Throwable) {
            0
        }
    }

    fun readRamMetrics(context: Context): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L to 0L
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val availMb = memInfo.availMem / (1024 * 1024)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        return availMb to totalMb
    }

    suspend fun fetchLiveMetrics(context: Context, isTurboActive: Boolean): LiveMetrics {
        var silver = readCurCpuFreqMhz(0)
        var gold = readCurCpuFreqMhz(4)

        // If direct file read failed (SELinux sandbox), query via ShellExecutor if Shizuku is active
        if (silver == 0 || gold == 0) {
            val res = ShellExecutor.execute("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq /sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq")
            if (res.isSuccess && res.stdout.isNotBlank()) {
                val lines = res.stdout.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) silver = (lines.getOrNull(0)?.toIntOrNull() ?: 0) / 1000
                if (lines.size > 1) gold = (lines.getOrNull(1)?.toIntOrNull() ?: 0) / 1000
            }
        }

        val (availMb, totalMb) = readRamMetrics(context)

        val refreshRate = try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            val display = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            display?.refreshRate ?: 90f
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
            thermalStatus = thermalStatus,
            refreshRateHz = refreshRate,
            ramAvailableMb = availMb,
            ramTotalMb = totalMb,
            isTurboActive = isTurboActive
        )
    }
}
