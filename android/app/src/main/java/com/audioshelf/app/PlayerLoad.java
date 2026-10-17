package com.audioshelf.app;

import android.content.Context;
import androidx.media3.common.MediaItem;
import com.getcapacitor.JSArray;
import com.getcapacitor.PluginCall;
import java.util.List;

/** Loading a book: remembers the session for the notification/resume, builds the playlist (reads tags, so not on the main thread) and starts it. */
final class PlayerLoad {
  private PlayerLoad() {}

  /** items: [{uri, title}] — the files of one book; index/position say where to start. */
  static void run(Context ctx, PluginCall call, PlayerRunner runner, Runnable startService) {
    JSArray items = call.getArray("items");
    if (items == null || items.length() == 0) {
      call.reject("items required");
      return;
    }
    int index = call.getInt("index", 0);
    long position = call.getData().optLong("position", 0L);
    boolean autoplay = Boolean.TRUE.equals(call.getBoolean("autoplay", false));
    List<MediaItem> list;
    try {
      String title = call.getString("title", "");
      String artist = call.getString("artist", "");
      ResumeStore.saveSession(ctx, call.getString("id", ""), title, artist, items, index, position);
      list = Playlist.from(ctx, items, title, artist);
    } catch (Exception e) {
      call.reject("bad items: " + e.getMessage());
      return;
    }
    runner.run(
        call,
        p -> {
          p.setMediaItems(list, index, position);
          p.prepare();
          p.setPlayWhenReady(autoplay);
          if (autoplay) startService.run();
        });
  }
}
