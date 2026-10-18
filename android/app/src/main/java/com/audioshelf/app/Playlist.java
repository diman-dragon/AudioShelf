package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import com.getcapacitor.JSArray;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

final class Playlist {
  private Playlist() {}

  static List<MediaItem> from(Context ctx, JSArray items, String book, String artist) throws Exception {
    byte[] art = items.length() > 0 ? TrackMeta.cover(ctx, items.getJSONObject(0).getString("uri")) : null;
    List<MediaItem> list = new ArrayList<>();
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.getJSONObject(i);
      String uri = item.getString("uri");
      MediaItem.Builder builder =
          new MediaItem.Builder()
              .setUri(Uri.parse(uri))
              .setMediaId(uri)
              .setMediaMetadata(TrackMeta.of(item.optString("title", book), book, artist, art));
      long start = item.optLong("start", 0L);
      long end = item.optLong("end", -1L);
      if (start > 0 || end > 0) {
        MediaItem.ClippingConfiguration.Builder clip = new MediaItem.ClippingConfiguration.Builder().setStartPositionMs(start);
        if (end > start) clip.setEndPositionMs(end);
        builder.setClippingConfiguration(clip.build());
      }
      list.add(builder.build());
    }
    return list;
  }
}
