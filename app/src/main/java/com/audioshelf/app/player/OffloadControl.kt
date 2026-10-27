@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * Switches audio offload on and off. Every switch rebuilds the AudioTrack, so it must never happen in a burst:
 * the flags are set at once, and the one real change is applied after they stop moving.
 */
class OffloadControl {
    private val main = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    @Volatile private var allowed = false
    @Volatile private var neutral = true
    private var applied = false
    private val settle = Runnable { update() }

    fun attach(p: ExoPlayer) {
        player = p
        applied = false
        update()
    }

    fun detach() {
        main.removeCallbacks(settle)
        player = null
    }

    fun setAllowed(allow: Boolean) {
        allowed = allow
        schedule()
    }

    fun setNeutral(flat: Boolean) {
        neutral = flat
        schedule()
    }

    private fun schedule() {
        main.removeCallbacks(settle)
        main.postDelayed(settle, SETTLE_MS)
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

    private companion object {
        const val SETTLE_MS = 600L
    }
}
