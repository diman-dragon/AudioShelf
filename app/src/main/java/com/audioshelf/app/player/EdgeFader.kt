package com.audioshelf.app.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

private const val TICK_MS = 100L
private const val CHECK_MS = 10_000L

class EdgeFader(private val player: Player, private val onChange: () -> Unit) : Runnable, Player.Listener {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var fadeIn = 0
    @Volatile private var fadeOut = 0

    init {
        player.addListener(this)
    }

    fun set(fadeInMs: Int, fadeOutMs: Int) {
        fadeIn = fadeInMs
        fadeOut = fadeOutMs
        handler.post(this)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
    }

    fun factor(): Float {
        val inMs = fadeIn
        val outMs = fadeOut
        val duration = player.duration
        if (duration <= 0 || duration < 2L * (inMs + outMs)) return 1f
        val position = player.currentPosition
        val up = if (inMs > 0) minOf(1f, position.toFloat() / inMs) else 1f
        val down = if (outMs > 0) minOf(1f, (duration - position).toFloat() / outMs) else 1f
        return maxOf(0f, minOf(up, down))
    }

    override fun run() {
        handler.removeCallbacks(this)
        onChange()
        if (!player.isPlaying || (fadeIn == 0 && fadeOut == 0)) return
        handler.postDelayed(this, nextDelay())
    }

    private fun nextDelay(): Long {
        val duration = player.duration
        if (duration <= 0) return 1000
        val position = player.currentPosition
        if (position < fadeIn) return TICK_MS
        val untilFadeOut = duration - fadeOut - position
        if (untilFadeOut <= 0) return TICK_MS
        val wall = (untilFadeOut / maxOf(0.25f, player.playbackParameters.speed)).toLong()
        return maxOf(TICK_MS, minOf(CHECK_MS, wall - TICK_MS))
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) = run()

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) = run()

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = run()
}
