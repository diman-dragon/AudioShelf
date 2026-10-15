package com.audioshelf.app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.media3.common.Player;

/**
 * Sleep timer: pauses after N minutes of actual playback, or at the end of the current chapter.
 * The last 15 seconds fade the sound out instead of cutting it.
 */
final class SleepTimer implements Player.Listener, Runnable {
  private static final long TICK_MS = 1000;
  private static final long FADE_TICK_MS = 200;
  private static final long FADE_MS = 15_000;

  private final Player player;
  private final Runnable onChange; // applies the volume
  private final Handler handler = new Handler(Looper.getMainLooper());
  private volatile long left;
  private volatile boolean atChapterEnd;
  private long lastTick;

  SleepTimer(Player player, Runnable onChange) {
    this.player = player;
    this.onChange = onChange;
    player.addListener(this);
  }

  /** minutes > 0: count down while playing; chapter: pause when the current file ends; both 0/false: off. */
  void set(long minutes, boolean chapter) {
    handler.post(
        () -> {
          handler.removeCallbacks(this);
          left = minutes * 60000;
          atChapterEnd = chapter;
          lastTick = SystemClock.elapsedRealtime();
          onChange.run();
          if (left > 0) handler.postDelayed(this, TICK_MS);
        });
  }

  long left() {
    return left;
  }

  boolean chapterEnd() {
    return atChapterEnd;
  }

  /** 1 normally, falling to 0 over the last 15 seconds of the countdown. */
  float factor() {
    long l = left;
    return l > 0 && l < FADE_MS ? (float) l / FADE_MS : 1f;
  }

  void stop() {
    handler.removeCallbacksAndMessages(null);
    left = 0;
    atChapterEnd = false;
  }

  @Override
  public void run() {
    long now = SystemClock.elapsedRealtime();
    if (player.isPlaying()) left -= now - lastTick; // paused time does not count
    lastTick = now;
    if (left <= 0) {
      left = 0;
      player.pause();
      onChange.run(); // volume is back to normal for the next start
      return;
    }
    onChange.run();
    handler.postDelayed(this, left < FADE_MS ? FADE_TICK_MS : TICK_MS);
  }

  @Override
  public void onPositionDiscontinuity(
      Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
    if (atChapterEnd && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
      atChapterEnd = false;
      player.pause();
    }
  }

  @Override
  public void onPlaybackStateChanged(int state) {
    if (state == Player.STATE_ENDED) atChapterEnd = false; // the last file ended by itself
  }
}
