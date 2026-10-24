package com.audioshelf.app.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

private const val EVERY_MS = 10_000L

class PositionSaver(private val player: Player, private val save: () -> Unit) : Player.Listener, Runnable {
    private val handler = Handler(Looper.getMainLooper())

    init {
        player.addListener(this)
    }

    fun flush() = save()

    fun stop() {
        handler.removeCallbacks(this)
        save()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        handler.removeCallbacks(this)
        if (isPlaying) handler.postDelayed(this, EVERY_MS) else save()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = save()

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) save()
    }

    override fun run() {
        save()
        handler.postDelayed(this, EVERY_MS)
    }
}
