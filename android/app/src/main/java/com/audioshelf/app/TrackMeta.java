package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaMetadata;

/** Title, author and cover shown in the media notification and on the lock screen. */
final class TrackMeta {
  private TrackMeta() {}

  /** Embedded cover as a small JPEG, or null if the file has none. */
  static byte[] cover(Context ctx, String uri) {
    try {
      return CoverArt.jpeg(ctx, Uri.parse(uri), 512);
    } catch (Exception e) {
      return null;
    }
  }

  static MediaMetadata of(String title, String album, String artist, byte[] art) {
    MediaMetadata.Builder b = new MediaMetadata.Builder().setTitle(title).setAlbumTitle(album);
    if (artist != null && !artist.isEmpty()) b.setArtist(artist);
    if (art != null) b.setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
    return b.build();
  }
}
