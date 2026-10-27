package com.audioshelf.app.player

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.audioshelf.app.library.REWIND_MS

// Resuming rewinds by REWIND_MS anyway, so a position saved this often never loses anything you would notice.
private const val EVERY_MS = REWIND_MS

/** [save] gets true for the periodic tick while playing: that one must not wake the library list. */
class PositionSaver(private val player: Player, private val save: (periodic: Boolean) -> Unit) : Player.Listener, Runnable {
    private val handler = Handler(Looper.getMainLooper())

    init {
        player.addListener(this)
    }

    fun flush() = save(false)

    fun stop() {
        handler.removeCallbacks(this)
        save(false)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        handler.removeCallbacks(this)
        if (isPlaying) handler.postDelayed(this, EVERY_MS) else save(false)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = save(false)

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) save(false)
    }

    override fun run() {
        save(true)
        handler.postDelayed(this, EVERY_MS)
    }
}
