package com.audioshelf.app;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

final class PlayerToggle {
  private PlayerToggle() {}

  static void apply(ExoPlayer p, Runnable startService) {
    boolean ended = p.getPlaybackState() == Player.STATE_ENDED;
    if (p.getPlayWhenReady() && !ended) {
      p.pause();
      return;
    }
    if (ended) p.seekTo(0, 0);
    startService.run();
    p.play();
  }
}
