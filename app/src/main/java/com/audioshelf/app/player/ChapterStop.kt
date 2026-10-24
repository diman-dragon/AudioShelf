package com.audioshelf.app.player

import androidx.media3.common.Player

class ChapterStop(private val player: Player) : Player.Listener {
    @Volatile private var enabled = false

    init {
        player.addListener(this)
    }

    fun set(on: Boolean) {
        enabled = on
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (enabled && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) player.pause()
    }
}
