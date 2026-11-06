package com.carlomontec.volumefader.audio

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

class MediaPlaybackController(context: Context) {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Dispatches a global MEDIA_PAUSE key event to pause currently active media sessions.
     */
    fun pausePlayback() {
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)

        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)
    }
}
