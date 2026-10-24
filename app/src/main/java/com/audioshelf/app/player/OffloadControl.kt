@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

class OffloadControl {
    private val main = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    @Volatile private var allowed = false
    @Volatile private var neutral = true
    private var applied = false

    fun attach(p: ExoPlayer) {
        player = p
        applied = false
        update()
    }

    fun detach() {
        player = null
    }

    fun setAllowed(allow: Boolean) {
        main.post {
            allowed = allow
            update()
        }
    }

    fun setNeutral(flat: Boolean) {
        main.post {
            neutral = flat
            update()
        }
    }

    private fun update() {
        val on = allowed && neutral
        val p = player ?: return
        if (on == applied) return
        applied = on
        val mode = if (on) AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED else AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
        val prefs = AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(mode)
            .setIsGaplessSupportRequired(true)
            .setIsSpeedChangeSupportRequired(true)
            .build()
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon().setAudioOffloadPreferences(prefs).build()
    }
}
