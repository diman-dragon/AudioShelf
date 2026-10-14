package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sound settings of the one player: equalizer bands and a loudness trim for each file. */
final class PlayerSound {
  private static final SoundProcessor PROCESSOR = new SoundProcessor();
  private static final Map<String, Float> TRIMS = new ConcurrentHashMap<>();
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static ExoPlayer player;

  private PlayerSound() {}

  /** Goes into the audio sink when the player is built. */
  static SoundProcessor processor() {
    return PROCESSOR;
  }

  /** Called once, right after the player is built (main thread). */
  static void attach(ExoPlayer p) {
    player = p;
    p.addListener(
        new Player.Listener() {
          @Override
          public void onMediaItemTransition(MediaItem item, int reason) {
            useTrimOf(item);
          }
        });
  }

  static void detach() {
    player = null;
  }

  /** bands: 10 values in dB; trims: file uri -> dB. May be called from any thread. */
  static void apply(float[] bands, Map<String, Float> trims) {
    PROCESSOR.setBands(bands);
    TRIMS.clear();
    TRIMS.putAll(trims);
    MAIN.post(
        () -> {
          if (player != null) useTrimOf(player.getCurrentMediaItem());
        });
  }

  private static void useTrimOf(MediaItem item) {
    Float db = item == null ? null : TRIMS.get(item.mediaId);
    PROCESSOR.setTrimDb(db == null ? 0f : db);
  }
}
