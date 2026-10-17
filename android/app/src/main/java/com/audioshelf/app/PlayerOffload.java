package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Hardware playback (audio offload): the phone's audio DSP decodes, the CPU sleeps. Used only when the user
 * allowed it and no sound processing is needed, because offload bypasses the equalizer and loudness trim.
 */
final class PlayerOffload {
  private static final Handler MAIN = new Handler(Looper.getMainLooper());
  private static ExoPlayer player;
  private static volatile boolean allowed;
  private static volatile boolean neutral = true;
  private static boolean applied;

  private PlayerOffload() {}

  /** Called once, right after the player is built (main thread). */
  static void attach(ExoPlayer p, boolean allow) {
    player = p;
    allowed = allow;
    applied = false;
    update();
  }

  /** For the settings screen: is it allowed, and is the sound untouched right now (so that it can be used)? */
  static boolean isAllowed() {
    return allowed;
  }

  static boolean isNeutral() {
    return neutral;
  }

  static void detach() {
    player = null;
  }

  /** The "hardware playback" setting. Any thread. */
  static void setAllowed(boolean allow) {
    MAIN.post(
        () -> {
          allowed = allow;
          update();
        });
  }

  /** True when the sound is untouched: flat equalizer and no loudness trims. Any thread. */
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
            .setIsSpeedChangeSupportRequired(true) // speeds other than 1x use the normal decoder
            .build();
    TrackSelectionParameters params = player.getTrackSelectionParameters().buildUpon().setAudioOffloadPreferences(prefs).build();
    player.setTrackSelectionParameters(params);
  }
}
