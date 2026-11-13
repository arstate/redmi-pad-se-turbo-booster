package com.arya.turbobooster.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arya.turbobooster.MainActivity
import com.arya.turbobooster.R
import com.arya.turbobooster.booster.ShellExecutor
import com.arya.turbobooster.booster.TurboBoosterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val CHANNEL_ID = "turbo_booster_boot_channel"
        private const val NOTIFICATION_ID = 2001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Device boot completed received")

        if (!TurboBoosterManager.isAutoBootEnabled(context)) {
            Log.d(TAG, "Auto-apply on boot is disabled; doing nothing")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait up to 15 seconds for Shizuku binder to become ready post-boot
                var attempts = 0
                while (attempts < 15 && (!ShellExecutor.isShizukuAvailable() || !ShellExecutor.hasShizukuPermission())) {
                    delay(1000)
                    attempts++
                }

                if (ShellExecutor.isShizukuAvailable() && ShellExecutor.hasShizukuPermission()) {
                    Log.d(TAG, "Shizuku ready after ${attempts}s, restoring Turbo profile")
                    val currentProfile = TurboBoosterManager.getActiveProfile(context)
                    TurboBoosterManager.applyProfile(context, currentProfile) { entry ->
                        Log.d(TAG, "[${entry.command}] ${entry.output}")
                    }
                } else {
                    Log.w(TAG, "Shizuku not ready within 15s post-boot; posting prompt notification")
                    postShizukuPromptNotification(context)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in BootReceiver: ${t.message}", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postShizukuPromptNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Turbo Booster Status",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts when Shizuku service needs re-starting"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Target intent: try to open Shizuku or open TurboBooster
            val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                ?: Intent(context, MainActivity::class.java)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Turbo Booster: Action Required")
                .setContentText("Shizuku is offline post-boot. Tap to start Shizuku and restore Turbo mode.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to post Shizuku prompt notification: ${t.message}", t)
        }
    }
}
