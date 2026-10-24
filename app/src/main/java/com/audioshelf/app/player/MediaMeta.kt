package com.audioshelf.app.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

fun mediaItemOf(item: PlayItem, album: String, artist: String, art: ByteArray?): MediaItem {
    val meta = MediaMetadata.Builder().setTitle(item.title).setAlbumTitle(album)
    if (artist.isNotEmpty()) meta.setArtist(artist)
    if (art != null) meta.setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
    val builder = MediaItem.Builder().setUri(Uri.parse(item.uri)).setMediaId(item.uri).setMediaMetadata(meta.build())
    if (item.clipped) {
        val clip = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(item.startMs)
        if (item.endMs > item.startMs) clip.setEndPositionMs(item.endMs)
        builder.setClippingConfiguration(clip.build())
    }
    return builder.build()
}
