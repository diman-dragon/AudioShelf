package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaMetadata;

/** Title and cover shown in the media notification and on the lock screen. */
final class TrackMeta {
  private TrackMeta() {}

  static MediaMetadata of(Context ctx, String uri, String title, String artist) {
    MediaMetadata.Builder b = new MediaMetadata.Builder().setTitle(title);
    if (artist != null && !artist.isEmpty()) b.setArtist(artist);
    try {
      byte[] art = CoverArt.jpeg(ctx, Uri.parse(uri), 512);
      b.setArtworkData(art, MediaMetadata.PICTURE_TYPE_FRONT_COVER);
    } catch (Exception ignored) {
      // file without a cover
    }
    return b.build();
  }
}
