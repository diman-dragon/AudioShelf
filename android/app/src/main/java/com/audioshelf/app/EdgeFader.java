package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

/** Fades the volume in at the start of every file and out at its end (a book: short, an album: long). */
final class EdgeFader implements Runnable {
  private static final long TICK_MS = 100;

  private final ExoPlayer player;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private volatile int fadeIn;
  private volatile int fadeOut;

  EdgeFader(ExoPlayer player) {
    this.player = player;
    player.addListener(
        new Player.Listener() {
          @Override
          public void onIsPlayingChanged(boolean playing) {
            handler.removeCallbacks(EdgeFader.this);
            if (playing) handler.post(EdgeFader.this);
          }
        });
  }

  /** Durations in milliseconds; 0 means no fade on that edge. */
  void set(int fadeInMs, int fadeOutMs) {
    fadeIn = fadeInMs;
    fadeOut = fadeOutMs;
    handler.post(this::apply);
  }

  void stop() {
    handler.removeCallbacksAndMessages(null);
  }

  @Override
  public void run() {
    apply();
    handler.postDelayed(this, TICK_MS);
  }

  private void apply() {
    player.setVolume(volume(player.getCurrentPosition(), player.getDuration()));
  }

  /** 0..1 from the position in the current file; very short files are left alone. */
  private float volume(long position, long duration) {
    int in = fadeIn;
    int out = fadeOut;
    if (duration <= 0 || duration < 2L * (in + out)) return 1f;
    float up = in > 0 ? Math.min(1f, (float) position / in) : 1f;
    float down = out > 0 ? Math.min(1f, (float) (duration - position) / out) : 1f;
    return Math.max(0f, Math.min(up, down));
  }
}
