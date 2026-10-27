package com.audioshelf.app.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

const val ART_SCHEME = "audioshelf-art"

/**
 * A tiny pointer to the cover instead of the picture itself: the notification asks for the real
 * bitmap later, off the playback path, and every playlist update stays a few bytes per item.
 */
fun artUriOf(fileUri: String): Uri =
    Uri.Builder().scheme(ART_SCHEME).authority("cover").appendQueryParameter("u", fileUri).build()

fun mediaItemOf(item: PlayItem, album: String, artist: String, art: Uri?): MediaItem {
    val meta = MediaMetadata.Builder().setTitle(item.title).setAlbumTitle(album)
    if (artist.isNotEmpty()) meta.setArtist(artist)
    if (art != null) meta.setArtworkUri(art)
    val builder = MediaItem.Builder().setUri(Uri.parse(item.uri)).setMediaId(item.uri).setMediaMetadata(meta.build())
    if (item.clipped) {
        val clip = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(item.startMs)
        if (item.endMs > item.startMs) clip.setEndPositionMs(item.endMs)
        builder.setClippingConfiguration(clip.build())
    }
    return builder.build()
}
