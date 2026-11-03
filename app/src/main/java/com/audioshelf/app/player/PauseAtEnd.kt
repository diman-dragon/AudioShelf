@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.audioshelf.app.player

import androidx.media3.exoplayer.ExoPlayer

/** Combines chapter-mode and sleep-timer requests into one atomic Media3 boundary pause. */
class PauseAtEnd(private val player: ExoPlayer) {
    private var forChapters = false
    private var forSleep = false

    fun setForChapters(enabled: Boolean) {
        forChapters = enabled
        apply()
    }

    fun setForSleep(enabled: Boolean) {
        forSleep = enabled
        apply()
    }

    private fun apply() {
        player.setPauseAtEndOfMediaItems(forChapters || forSleep)
    }
}
