package com.audioshelf.app.player

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.audioshelf.app.library.REWIND_MS
import kotlin.math.abs

private const val MIN_STOP_MS = 3000L
private const val SAME_SPOT_MS = 1000L

class ResumeRewind(private val player: Player) : Player.Listener {
    private var stoppedAt = 0L
    private var stoppedIndex = 0
    private var stoppedPosition = 0L

    init {
        player.addListener(this)
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) stoppedAt = 0
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            val stopped = player.playbackState == Player.STATE_READY && player.mediaItemCount > 0
            stoppedAt = if (stopped) SystemClock.elapsedRealtime() else 0
            stoppedIndex = player.currentMediaItemIndex
            stoppedPosition = player.currentPosition
            return
        }
        val at = stoppedAt
        stoppedAt = 0
        if (at == 0L || SystemClock.elapsedRealtime() - at < MIN_STOP_MS) return
        val position = player.currentPosition
        val sameSpot = player.currentMediaItemIndex == stoppedIndex && abs(position - stoppedPosition) < SAME_SPOT_MS
        if (sameSpot && position > 0) player.seekTo(maxOf(0L, position - REWIND_MS))
    }
}
