package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

final class EdgeFader implements Runnable, Player.Listener {
  private static final long TICK_MS = 100;
  private static final long CHECK_MS = 10_000;

  private final ExoPlayer player;
  private final Runnable onChange;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private volatile int fadeIn;
  private volatile int fadeOut;

  EdgeFader(ExoPlayer player, Runnable onChange) {
    this.player = player;
    this.onChange = onChange;
    player.addListener(this);
  }

  void set(int fadeInMs, int fadeOutMs) {
    fadeIn = fadeInMs;
    fadeOut = fadeOutMs;
    handler.post(this::run);
  }

  void stop() {
    handler.removeCallbacksAndMessages(null);
  }

  float factor() {
    int in = fadeIn;
    int out = fadeOut;
    long duration = player.getDuration();
    if (duration <= 0 || duration < 2L * (in + out)) return 1f;
    long position = player.getCurrentPosition();
    float up = in > 0 ? Math.min(1f, (float) position / in) : 1f;
    float down = out > 0 ? Math.min(1f, (float) (duration - position) / out) : 1f;
    return Math.max(0f, Math.min(up, down));
  }

  @Override
  public void run() {
    handler.removeCallbacks(this);
    onChange.run();
    if (!player.isPlaying() || (fadeIn == 0 && fadeOut == 0)) return;
    handler.postDelayed(this, nextDelay());
  }

  private long nextDelay() {
    long duration = player.getDuration();
    if (duration <= 0) return 1000;
    long position = player.getCurrentPosition();
    if (position < fadeIn) return TICK_MS;
    long untilFadeOut = duration - fadeOut - position;
    if (untilFadeOut <= 0) return TICK_MS;
    long wall = (long) (untilFadeOut / Math.max(0.25f, player.getPlaybackParameters().speed));
    return Math.max(TICK_MS, Math.min(CHECK_MS, wall - TICK_MS));
  }

  @Override
  public void onIsPlayingChanged(boolean playing) {
    run();
  }

  @Override
  public void onPositionDiscontinuity(
      Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
    run();
  }

  @Override
  public void onPlaybackParametersChanged(androidx.media3.common.PlaybackParameters parameters) {
    run();
  }
}
