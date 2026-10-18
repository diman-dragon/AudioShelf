package com.audioshelf.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import com.getcapacitor.JSArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.Executors;
import org.json.JSONObject;

final class ResumeLoader {
  private static final ListeningExecutorService IO = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

  private ResumeLoader() {}

  static ListenableFuture<MediaSession.MediaItemsWithStartPosition> load(Context ctx) {
    Context app = ctx.getApplicationContext();
    return IO.submit(
        () -> {
          String raw = ResumeStore.get(app, ResumeStore.SESSION);
          if (raw == null) throw new IllegalStateException("nothing to resume");
          JSONObject s = new JSONObject(raw);
          JSArray items = new JSArray(s.getJSONArray("items").toString());
          List<MediaItem> list = Playlist.from(app, items, s.optString("title"), s.optString("artist"));
          int index = Math.max(0, Math.min(list.size() - 1, ResumeStore.index(app)));
          long position = Math.max(0, ResumeStore.position(app) - ResumeRewind.REWIND_MS);
          restoreSettings(app);
          return new MediaSession.MediaItemsWithStartPosition(list, index, position);
        });
  }

  private static void restoreSettings(Context app) {
    new Handler(Looper.getMainLooper())
        .post(
            () -> {
              try {
                ExoPlayer p = PlayerHolder.get(app);
                String profile = ResumeStore.get(app, ResumeStore.PROFILE);
                if (profile != null) PlayerProfile.apply(p, new JSONObject(profile));
                String sound = ResumeStore.get(app, ResumeStore.SOUND);
                if (sound != null) PlayerSound.applyJson(new JSONObject(sound));
                String speed = ResumeStore.get(app, ResumeStore.SPEED);
                if (speed != null) p.setPlaybackSpeed(Float.parseFloat(speed));
              } catch (Exception ignored) {
              }
            });
  }
}
