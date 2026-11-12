package com.arya.turbobooster

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log

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
            val afd = assets.openFd("audio/kuntilanak_seram.mp3")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setVolume(1.0f, 1.0f)
                start()
                setOnCompletionListener {
                    Log.d("AudioPlayerService", "Kuntilanak playback finished")
                    stopSelf()
                }
            }
            Log.d("AudioPlayerService", "PLAYING_KUNTILANAK_SUCCESS!")
        } catch (t: Throwable) {
            Log.e("AudioPlayerService", "Error: ${t.message}", t)
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
