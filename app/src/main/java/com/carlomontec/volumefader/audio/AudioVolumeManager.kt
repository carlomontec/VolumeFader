package com.carlomontec.volumefader.audio

import android.content.Context
import android.media.AudioManager

class AudioVolumeManager(context: Context) {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    val currentVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    /**
     * Sets the music stream volume step quietly (suppressing volume HUD and audio beeps).
     */
    fun setVolume(volumeStep: Int) {
        val safeVolume = volumeStep.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            safeVolume,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
    }

    /**
     * Scales volume to a target float fraction [0.0..1.0] of a baseline starting volume.
     */
    fun setVolumeFraction(initialVolume: Int, factor: Float) {
        val targetStep = (initialVolume * factor.coerceIn(0.0f, 1.0f)).toInt()
        setVolume(targetStep)
    }
}
