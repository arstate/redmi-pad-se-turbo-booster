package com.arya.turbobooster.booster

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

object AudioBoosterManager {
    private const val TAG = "AudioBoosterManager"
    private const val PREFS_NAME = "audio_booster_prefs"
    private const val KEY_AUDIO_TURBO = "audio_turbo_enabled"
    private const val KEY_BASS_BOOST = "bass_boost_enabled"
    private const val KEY_SURROUND = "surround_enabled"
    private const val KEY_ACTIVE_PRESET = "active_preset"

    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var equalizer: Equalizer? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAudioTurboEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUDIO_TURBO, true)
    }

    fun setAudioTurboEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUDIO_TURBO, enabled).apply()
    }

    fun isBassBoostEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BASS_BOOST, true)
    }

    fun setBassBoostEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BASS_BOOST, enabled).apply()
    }

    fun isSurroundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SURROUND, true)
    }

    fun setSurroundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SURROUND, enabled).apply()
    }

    fun getActivePreset(context: Context): String {
        return getPrefs(context).getString(KEY_ACTIVE_PRESET, "bass_club") ?: "bass_club"
    }

    fun setActivePreset(context: Context, preset: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_PRESET, preset).apply()
    }

    suspend fun applyAudioHardwareBoost(
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        var allOk = true

        suspend fun runStep(cmd: String, desc: String) {
            val ts = timeFormat.format(java.util.Date())
            val res = ShellExecutor.execute(cmd, requirePrivileged = true)
            val out = if (res.stdout.isNotBlank()) res.stdout else res.stderr
            val entry = LogEntry(
                timestamp = ts,
                command = desc,
                output = out.ifBlank { if (res.isSuccess) "OK" else "Code ${res.exitCode}" },
                isSuccess = res.isSuccess
            )
            onLog?.invoke(entry)
            if (!res.isSuccess) allOk = false
        }

        // 1. Unlock Bitstream Surround Formats
        runStep("cmd audio set-surround-format-enabled 5 true", "Unlock Dolby Digital (AC3)")
        runStep("cmd audio set-surround-format-enabled 6 true", "Unlock Dolby Digital Plus (E-AC3)")
        runStep("cmd audio set-surround-format-enabled 18 true", "Unlock Dolby Atmos Spatial (E-AC3-JOC)")
        runStep("cmd audio set-surround-format-enabled 19 true", "Unlock Dolby MAT (Lossless Spatial)")

        // 2. Force Encoded Surround Mode to ALWAYS (Mode 2)
        runStep("cmd audio set-encoded-surround-mode 2", "Set Encoded Surround -> ALWAYS")

        // 3. Enable Android Framework Spatializer
        runStep("settings put global spatial_audio_feature_enable 1", "Spatial Audio Feature -> ENABLED")
        runStep("settings put global enableSpatialAudio true", "Global Spatial Audio -> TRUE")
        runStep("cmd audio set-spatializer-enabled true", "Audio Spatializer Service -> ON")

        return allOk
    }

    suspend fun resetAudioHardwareStock(
        onLog: ((LogEntry) -> Unit)? = null
    ): Boolean {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        var allOk = true

        suspend fun runStep(cmd: String, desc: String) {
            val ts = timeFormat.format(java.util.Date())
            val res = ShellExecutor.execute(cmd, requirePrivileged = true)
            val out = if (res.stdout.isNotBlank()) res.stdout else res.stderr
            val entry = LogEntry(
                timestamp = ts,
                command = desc,
                output = out.ifBlank { if (res.isSuccess) "OK" else "Code ${res.exitCode}" },
                isSuccess = res.isSuccess
            )
            onLog?.invoke(entry)
            if (!res.isSuccess) allOk = false
        }

        runStep("cmd audio set-encoded-surround-mode 0", "Reset Encoded Surround Mode -> AUTO")
        runStep("settings put global spatial_audio_feature_enable 0", "Reset Spatial Audio Feature")
        runStep("cmd audio set-spatializer-enabled false", "Disable Audio Spatializer")

        return allOk
    }

    /**
     * Initializes in-app AudioFX on audio session 0 (Global Output).
     * Provides instant hardware bass boost & 3D virtualizer without reboot.
     */
    fun initAudioEffects(context: Context) {
        try {
            if (bassBoost == null) {
                bassBoost = BassBoost(0, 0).apply {
                    if (strengthSupported) {
                        setStrength(1000.toShort()) // Maximum deep punch
                    }
                    enabled = isBassBoostEnabled(context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost session 0 init skipped: ${e.message}")
        }

        try {
            if (virtualizer == null) {
                virtualizer = Virtualizer(0, 0).apply {
                    if (strengthSupported) {
                        setStrength(1000.toShort()) // Maximum 3D stereo widening
                    }
                    enabled = isSurroundEnabled(context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer session 0 init skipped: ${e.message}")
        }

        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, 0).apply {
                    applyPresetCurve(getActivePreset(context))
                    enabled = isAudioTurboEnabled(context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer session 0 init skipped: ${e.message}")
        }
    }

    fun applyPresetCurve(preset: String) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val minEqLevel = eq.bandLevelRange[0]
            val maxEqLevel = eq.bandLevelRange[1]

            // Harman V-Shape custom curves tailored for Redmi Pad SE Quad Speakers
            when (preset) {
                "bass_club" -> {
                    // Maximum warm sub-bass punch & crisp treble
                    for (i in 0 until numBands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            freq <= 100 -> (maxEqLevel * 0.9).toInt().toShort()   // Sub-bass heavy
                            freq <= 250 -> (maxEqLevel * 0.6).toInt().toShort()   // Mid-bass punch
                            freq <= 1000 -> (minEqLevel * 0.1).toInt().toShort()  // Anti-tinny/cempreng cut
                            freq <= 4000 -> (maxEqLevel * 0.3).toInt().toShort()  // Clear vocals
                            else -> (maxEqLevel * 0.7).toInt().toShort()          // Airy treble detail
                        }
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                "cinema" -> {
                    // Maximum spatial separation & vocal intelligibility
                    for (i in 0 until numBands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            freq <= 100 -> (maxEqLevel * 0.7).toInt().toShort()
                            freq <= 1000 -> (maxEqLevel * 0.2).toInt().toShort()
                            freq <= 4000 -> (maxEqLevel * 0.8).toInt().toShort()  // Dialogue clarity boost
                            else -> (maxEqLevel * 0.6).toInt().toShort()
                        }
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                "gaming" -> {
                    // Footstep and gunshot directional acoustic curve
                    for (i in 0 until numBands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            freq <= 150 -> (maxEqLevel * 0.4).toInt().toShort()
                            freq in 1000..5000 -> (maxEqLevel * 0.9).toInt().toShort() // Spatial cues
                            else -> (maxEqLevel * 0.5).toInt().toShort()
                        }
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying preset curve: ${e.message}")
        }
    }

    fun setBassBoostActive(active: Boolean) {
        try {
            bassBoost?.enabled = active
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle BassBoost: ${e.message}")
        }
    }

    fun setSurroundActive(active: Boolean) {
        try {
            virtualizer?.enabled = active
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle Virtualizer: ${e.message}")
        }
    }

    fun release() {
        try {
            bassBoost?.release()
            virtualizer?.release()
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audiofx: ${e.message}")
        }
        bassBoost = null
        virtualizer = null
        equalizer = null
    }
}
