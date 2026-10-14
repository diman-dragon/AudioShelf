package com.audioshelf.app;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import com.getcapacitor.JSArray;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** A book is a playlist of files (chapters). The cover is read once, from the first file. */
final class Playlist {
  private Playlist() {}

  /** items: [{uri, title}, ...]; book and artist go to every item's metadata. */
  static List<MediaItem> from(Context ctx, JSArray items, String book, String artist) throws Exception {
    byte[] art = items.length() > 0 ? TrackMeta.cover(ctx, items.getJSONObject(0).getString("uri")) : null;
    List<MediaItem> list = new ArrayList<>();
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.getJSONObject(i);
      String uri = item.getString("uri");
      list.add(
          new MediaItem.Builder()
              .setUri(Uri.parse(uri))
              .setMediaId(uri)
              .setMediaMetadata(TrackMeta.of(item.optString("title", book), book, artist, art))
              .build());
    }
    return list;
  }
}
