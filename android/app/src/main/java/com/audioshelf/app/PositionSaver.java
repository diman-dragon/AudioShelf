package com.audioshelf.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

/** Writes the playing position to disk natively, so it survives a frozen WebView or a killed process. */
final class PositionSaver implements Player.Listener, Runnable {
  private static final long EVERY_MS = 10_000;
  private static PositionSaver current;

  private final Context app;
  private final ExoPlayer player;
  private final Handler handler = new Handler(Looper.getMainLooper());

  private PositionSaver(Context app, ExoPlayer player) {
    this.app = app;
    this.player = player;
    player.addListener(this);
  }

  static synchronized void attach(Context app, ExoPlayer player) {
    current = new PositionSaver(app, player);
  }

  /** Saves one last time and stops. */
  static synchronized void detach() {
    if (current == null) return;
    current.handler.removeCallbacks(current);
    current.save();
    current = null;
  }

  /** Call before anything that clears the playlist or ends the process. */
  static synchronized void flush() {
    if (current != null) current.save();
  }

  private void save() {
    if (player.getMediaItemCount() == 0 || player.getPlaybackState() == Player.STATE_ENDED) return;
    ResumeStore.putSpot(app, player.getCurrentMediaItemIndex(), player.getCurrentPosition());
  }

  @Override
  public void onIsPlayingChanged(boolean playing) {
    handler.removeCallbacks(this);
    if (playing) handler.postDelayed(this, EVERY_MS);
    else save();
  }

  @Override
  public void onMediaItemTransition(MediaItem item, int reason) {
    save();
  }

  @Override
  public void onPositionDiscontinuity(
      Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
    if (reason == Player.DISCONTINUITY_REASON_SEEK) save();
  }

  @Override
  public void run() {
    save();
    handler.postDelayed(this, EVERY_MS);
  }
}
