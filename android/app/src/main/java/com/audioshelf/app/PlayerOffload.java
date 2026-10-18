package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences;
import androidx.media3.exoplayer.ExoPlayer;

final class PlayerOffload {
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static ExoPlayer player;
  private static volatile boolean allowed;
  private static volatile boolean neutral = true;
  private static boolean applied;

  private PlayerOffload() {}

  static void attach(ExoPlayer p, boolean allow) {
    player = p;
    allowed = allow;
    applied = false;
    update();
  }

  static boolean isAllowed() {
    return allowed;
  }

  static boolean isNeutral() {
    return neutral;
  }

  static void detach() {
    player = null;
  }

  static void setAllowed(boolean allow) {
    MAIN.post(
        () -> {
          allowed = allow;
          update();
        });
  }

  static void setNeutral(boolean flat) {
    MAIN.post(
        () -> {
          neutral = flat;
          update();
        });
  }

  private static void update() {
    boolean on = allowed && neutral;
    if (player == null || on == applied) return;
    applied = on;
    AudioOffloadPreferences prefs =
        new AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(
                on ? AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED : AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
            .setIsGaplessSupportRequired(true)
            .setIsSpeedChangeSupportRequired(true)
            .build();
    TrackSelectionParameters params = player.getTrackSelectionParameters().buildUpon().setAudioOffloadPreferences(prefs).build();
    player.setTrackSelectionParameters(params);
  }
}
