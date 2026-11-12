package com.arya.turbobooster

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import java.io.File

class AudioPlayerService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0)
            }
            mediaPlayer?.release()

            val customPath = intent?.getStringExtra("path")
            val mp = MediaPlayer()

            if (!customPath.isNullOrBlank() && File(customPath).exists()) {
                Log.d("AudioPlayerService", "Playing audio from custom path: $customPath")
                mp.setDataSource(customPath)
                mp.prepare()
            } else {
                Log.d("AudioPlayerService", "Playing embedded asset audio: audio/kuntilanak_seram.mp3")
                val afd = assets.openFd("audio/kuntilanak_seram.mp3")
                try {
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    mp.prepare()
                } finally {
                    afd.close()
                }
            }

            mp.setVolume(1.0f, 1.0f)
            mp.setOnCompletionListener {
                Log.d("AudioPlayerService", "Audio playback finished")
                stopSelf()
            }
            mediaPlayer = mp
            mp.start()
            Log.d("AudioPlayerService", "PLAYING_AUDIO_SUCCESS!")
        } catch (t: Throwable) {
            Log.e("AudioPlayerService", "Error in AudioPlayerService: ${t.message}", t)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
