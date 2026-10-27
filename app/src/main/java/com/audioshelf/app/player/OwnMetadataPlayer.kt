@file:OptIn(UnstableApi::class)

package com.audioshelf.app.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * What the notification, lock screen and Bluetooth show. ExoPlayer overlays the tags it finds in the
 * file (often windows-1251 read as Latin-1, i.e. garbage) on top of the item's metadata; this shows
 * only the metadata the app put on the item.
 */
class OwnMetadataPlayer(player: Player) : ForwardingPlayer(player) {
    override fun getMediaMetadata(): MediaMetadata = currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
}
