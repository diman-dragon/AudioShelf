package com.audioshelf.app;

import android.os.SystemClock;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;

final class ResumeRewind implements Player.Listener {
  static final long REWIND_MS = 5000;
  private static final long MIN_STOP_MS = 3000;

  private final Player player;
  private long stoppedAt;
  private int stoppedIndex;
  private long stoppedPosition;

  ResumeRewind(Player player) {
    this.player = player;
    player.addListener(this);
  }

  @Override
  public void onTimelineChanged(Timeline timeline, int reason) {
    if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) stoppedAt = 0;
  }

  @Override
  public void onIsPlayingChanged(boolean playing) {
    if (!playing) {
      boolean stopped = player.getPlaybackState() == Player.STATE_READY && player.getMediaItemCount() > 0;
      stoppedAt = stopped ? SystemClock.elapsedRealtime() : 0;
      stoppedIndex = player.getCurrentMediaItemIndex();
      stoppedPosition = player.getCurrentPosition();
      return;
    }
    long at = stoppedAt;
    stoppedAt = 0;
    if (at == 0 || SystemClock.elapsedRealtime() - at < MIN_STOP_MS) return;
    long position = player.getCurrentPosition();
    boolean sameSpot =
        player.getCurrentMediaItemIndex() == stoppedIndex && Math.abs(position - stoppedPosition) < 1000;
    if (sameSpot && position > 0) player.seekTo(Math.max(0, position - REWIND_MS));
  }
}
