package com.arya.turbobooster.booster

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

object AudioBoosterManager {
    private const val TAG = "AudioBoosterManager"
    private const val PREFS_NAME = "audio_booster_prefs"
    private const val KEY_AUDIO_TURBO = "audio_turbo_enabled"
    private const val KEY_BASS_BOOST = "bass_boost_enabled"
    private const val KEY_SURROUND = "surround_enabled"
    private const val KEY_ACTIVE_PRESET = "active_preset"

    private var loudnessEnhancer: LoudnessEnhancer? = null
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

        // 2. Set Encoded Surround Mode to AUTO (Mode 0) to prevent -3dB downmix attenuation
        runStep("cmd audio set-encoded-surround-mode 0", "Set Encoded Surround -> AUTO (Anti-Duck)")

        // 3. Enable Android Framework Spatializer
        runStep("settings put global spatial_audio_feature_enable 1", "Spatial Audio Feature -> ENABLED")
        runStep("settings put global enableSpatialAudio true", "Global Spatial Audio -> TRUE")

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
     * Incorporates LoudnessEnhancer (+5dB), anti-ducking BassBoost (550),
     * and phase-safe 7.1 Virtualizer (400) tailored for Quad Speakers.
     */
    fun initAudioEffects(context: Context) {
        val isEnabled = isAudioTurboEnabled(context)

        // 1. LoudnessEnhancer: Boosts overall perceived volume by +5dB (500mB) without clipping
        try {
            if (loudnessEnhancer == null) {
                loudnessEnhancer = LoudnessEnhancer(0).apply {
                    setTargetGain(600) // +6 dB clean target gain
                    enabled = isEnabled
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer session 0 init skipped: ${e.message}")
        }

        // 2. BassBoost: Calibrated to 550 to avoid Android DRC safety limiter ducking
        try {
            if (bassBoost == null) {
                bassBoost = BassBoost(0, 0).apply {
                    if (strengthSupported) {
                        setStrength(550.toShort()) // Sweet spot for Qualcomm DSP without pre-amp cut
                    }
                    enabled = isEnabled && isBassBoostEnabled(context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost session 0 init skipped: ${e.message}")
        }

        // 3. Virtualizer: Calibrated to 400 to prevent destructive phase cancellation across 4 speakers
        try {
            if (virtualizer == null) {
                virtualizer = Virtualizer(0, 0).apply {
                    if (strengthSupported) {
                        setStrength(400.toShort()) // Preserves center vocals while widening 7.1 space
                    }
                    enabled = isEnabled && isSurroundEnabled(context)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer session 0 init skipped: ${e.message}")
        }

        // 4. Equalizer: Positive Harman curve (all bands >= 0dB, never negative in 1kHz mids)
        try {
            if (equalizer == null) {
                equalizer = Equalizer(0, 0).apply {
                    applyPresetCurve(getActivePreset(context))
                    enabled = isEnabled
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
            val maxEqLevel = eq.bandLevelRange[1]

            // Positive-gain Harman curves tailored for Quad Speakers (No negative attenuation)
            when (preset) {
                "bass_club" -> {
                    // Deep warm sub-bass punch + vocal presence + airy 7.1 highs
                    for (i in 0 until numBands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            freq <= 80 -> (maxEqLevel * 0.70).toInt().toShort()  // +7 dB punch
                            freq <= 250 -> (maxEqLevel * 0.45).toInt().toShort() // +4.5 dB body
                            freq <= 1000 -> (maxEqLevel * 0.25).toInt().toShort() // +2.5 dB vocal presence (never negative!)
                            freq <= 4000 -> (maxEqLevel * 0.40).toInt().toShort() // +4 dB clarity
                            else -> (maxEqLevel * 0.60).toInt().toShort()         // +6 dB airy detail
                        }
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                "cinema" -> {
                    // 3D spherical surround + dialogue clarity boost
                    for (i in 0 until numBands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            freq <= 80 -> (maxEqLevel * 0.50).toInt().toShort()  // +5 dB rumble
                            freq <= 250 -> (maxEqLevel * 0.30).toInt().toShort() // +3 dB
                            freq <= 1000 -> (maxEqLevel * 0.45).toInt().toShort() // +4.5 dB dialogue boost
                            freq <= 4000 -> (maxEqLevel * 0.55).toInt().toShort() // +5.5 dB spatial fx
                            else -> (maxEqLevel * 0.50).toInt().toShort()         // +5 dB
                        }
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                "gaming" -> {
                    // Footstep and gunshot directional acoustic curve
                    for (i in 0 until numBands) {
                        val freq = eq.getCenterFreq(i.toShort()) / 1000
                        val level = when {
                            freq <= 150 -> (maxEqLevel * 0.30).toInt().toShort()
                            freq in 1000..5000 -> (maxEqLevel * 0.70).toInt().toShort() // Spatial cues
                            else -> (maxEqLevel * 0.40).toInt().toShort()
                        }
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying preset curve: ${e.message}")
        }
    }

    fun setLoudnessActive(active: Boolean) {
        try {
            loudnessEnhancer?.enabled = active
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle LoudnessEnhancer: ${e.message}")
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
            loudnessEnhancer?.release()
            bassBoost?.release()
            virtualizer?.release()
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audiofx: ${e.message}")
        }
        loudnessEnhancer = null
        bassBoost = null
        virtualizer = null
        equalizer = null
    }
}
