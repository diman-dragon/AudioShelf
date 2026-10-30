package com.audioshelf.app.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

/**
 * Keeps the player alive after an error. Without it a failed player sits idle: the screen still
 * shows the book as loaded and Play does nothing. Here it retries, skips the broken file, or
 * pauses and reports why.
 */
class ErrorRecovery(
    private val player: Player,
    private val policy: PlayerErrorPolicy = PlayerErrorPolicy(),
    private val report: (PlaybackFailure) -> Unit,
) : Player.Listener {
    private var attempt = 0

    init {
        player.addListener(this)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) attempt = 0
    }

    override fun onPlayerError(error: PlaybackException) {
        val kind = failureKindOf(error.errorCode)
        val next = nextFileIndex()
        val title = player.currentMediaItem?.mediaMetadata?.title?.toString()
        when (policy.decide(kind, attempt, next >= 0)) {
            Recovery.Retry -> {
                attempt++
                player.prepare()
            }
            Recovery.SkipFile -> {
                attempt = 0
                report(PlaybackFailure(kind, title, skipped = true))
                player.seekTo(next, 0L)
                player.prepare()
            }
            Recovery.GiveUp -> {
                attempt = 0
                player.pause()
                report(PlaybackFailure(kind, title, skipped = false))
            }
        }
    }

    /** The first later item that comes from a different file: the chapters of one m4b share a file and fail together. */
    private fun nextFileIndex(): Int {
        val here = player.currentMediaItem?.localConfiguration?.uri
        for (i in player.currentMediaItemIndex + 1 until player.mediaItemCount) {
            if (player.getMediaItemAt(i).localConfiguration?.uri != here) return i
        }
        return -1
    }
}
