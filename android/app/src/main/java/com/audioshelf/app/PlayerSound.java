package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

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

  /** {bands: [10 dB values], trims: {file uri: dB}, boost: linear factor} as sent by JavaScript. Any thread. */
  static void applyJson(JSONObject d) {
    JSONArray list = d.optJSONArray("bands");
    float[] bands = new float[SoundProcessor.BANDS];
    for (int i = 0; list != null && i < bands.length && i < list.length(); i++) {
      bands[i] = (float) list.optDouble(i, 0);
    }
    Map<String, Float> trims = new HashMap<>();
    JSONObject raw = d.optJSONObject("trims");
    if (raw != null) {
      Iterator<String> keys = raw.keys();
      while (keys.hasNext()) {
        String uri = keys.next();
        trims.put(uri, (float) raw.optDouble(uri, 0));
      }
    }
    apply(bands, trims, (float) d.optDouble("boost", 1.0));
  }

  private static void apply(float[] bands, Map<String, Float> trims, float boost) {
    boolean flat = trims.isEmpty() && boost <= 1f;
    for (float b : bands) flat &= b == 0;
    PlayerOffload.setNeutral(flat); // offload would bypass the equalizer and the trims
    PROCESSOR.setBands(bands);
    PROCESSOR.setBoost(boost);
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
