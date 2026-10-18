package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaMetadata;

final class TrackMeta {
  private TrackMeta() {}

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
