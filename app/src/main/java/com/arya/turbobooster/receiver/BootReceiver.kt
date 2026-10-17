package com.arya.turbobooster.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arya.turbobooster.booster.TurboBoosterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device boot completed")
            if (TurboBoosterManager.isAutoBootEnabled(context) && TurboBoosterManager.isTurboActive(context)) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Wait up to 15 seconds for Shizuku service to bind post-boot
                        var attempts = 0
                        while (attempts < 15 && (!com.arya.turbobooster.booster.ShellExecutor.isShizukuAvailable() || !com.arya.turbobooster.booster.ShellExecutor.hasShizukuPermission())) {
                            kotlinx.coroutines.delay(1000)
                            attempts++
                        }

                        if (com.arya.turbobooster.booster.ShellExecutor.isShizukuAvailable() && com.arya.turbobooster.booster.ShellExecutor.hasShizukuPermission()) {
                            Log.d("BootReceiver", "Shizuku ready after ${attempts}s, restoring Turbo Mode profile")
                            TurboBoosterManager.applySuperPerformanceMode(context) { entry ->
                                Log.d("BootReceiver", "[${entry.command}] ${entry.output}")
                            }
                        } else {
                            Log.w("BootReceiver", "Shizuku not ready within 15s post-boot; skipping auto-boost")
                        }
                    } catch (t: Throwable) {
                        Log.e("BootReceiver", "Error in BootReceiver: ${t.message}", t)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
