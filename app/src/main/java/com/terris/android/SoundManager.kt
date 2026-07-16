package com.terris.android

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val dropSoundId = soundPool.load(appContext, R.raw.drop, 1)
    private val lineSoundId = soundPool.load(appContext, R.raw.line, 1)
    private var backgroundPlayer: MediaPlayer? = null
    private var released = false

    fun startBackground() {
        if (released) return
        val player = backgroundPlayer
        if (player != null) {
            if (!player.isPlaying) player.start()
            return
        }
        backgroundPlayer = MediaPlayer.create(appContext, R.raw.background)?.apply {
            isLooping = true
            setVolume(0.45f, 0.45f)
            start()
        }
    }

    fun pauseBackground() {
        backgroundPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun resumeBackground() {
        if (released) return
        val player = backgroundPlayer
        if (player != null) {
            if (!player.isPlaying) player.start()
        } else {
            startBackground()
        }
    }

    fun playDrop() {
        if (!released) soundPool.play(dropSoundId, 0.85f, 0.85f, 1, 0, 1f)
    }

    fun playLineClear() {
        if (!released) soundPool.play(lineSoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        released = true
        backgroundPlayer?.run {
            stop()
            release()
        }
        backgroundPlayer = null
        soundPool.release()
    }
}
