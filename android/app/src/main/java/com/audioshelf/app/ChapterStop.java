package com.audioshelf.app;

import androidx.media3.common.Player;

final class ChapterStop implements Player.Listener {
  private final Player player;
  private volatile boolean enabled;

  ChapterStop(Player player) {
    this.player = player;
    player.addListener(this);
  }

  void set(boolean on) {
    enabled = on;
  }

  @Override
  public void onPositionDiscontinuity(
      Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
    if (enabled && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) player.pause();
  }
}
