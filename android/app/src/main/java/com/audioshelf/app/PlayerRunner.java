package com.audioshelf.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.exoplayer.ExoPlayer;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class PlayerRunner {
  private final Handler main = new Handler(Looper.getMainLooper());
  private final Context context;
  private final BiConsumer<String, JSObject> notify;
  private ExoPlayer listened;

  PlayerRunner(Context context, BiConsumer<String, JSObject> notify) {
    this.context = context;
    this.notify = notify;
  }

  void run(PluginCall call, Consumer<ExoPlayer> job) {
    main.post(
        () -> {
          try {
            ExoPlayer p = PlayerHolder.get(context);
            if (p != listened) {
              listened = p;
              new StateEvents(p, (player) -> notify.accept("state", PlayerState.of(player)));
            }
            job.accept(p);
            call.resolve(PlayerState.of(p));
          } catch (Exception e) {
            call.reject("player: " + e.getMessage());
          }
        });
  }
}
