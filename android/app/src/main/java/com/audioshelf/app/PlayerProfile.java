package com.audioshelf.app;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

/** Playback mode and file-edge effects; books and albums get different ones from JavaScript. */
final class PlayerProfile {
  private static EdgeFader fader;
  private static ChapterStop chapterStop;

  private PlayerProfile() {}

  /** Called once, right after the player is built (main thread). */
  static synchronized void attach(ExoPlayer player) {
    fader = new EdgeFader(player);
    chapterStop = new ChapterStop(player);
  }

  static synchronized void detach() {
    if (fader != null) fader.stop();
    fader = null;
    chapterStop = null;
  }

  /** repeat: "off" | "all" | "one". */
  static synchronized void apply(
      ExoPlayer player, String repeat, boolean shuffle, boolean pauseAfter, int fadeIn, int fadeOut) {
    player.setRepeatMode(
        "all".equals(repeat)
            ? Player.REPEAT_MODE_ALL
            : "one".equals(repeat) ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    player.setShuffleModeEnabled(shuffle);
    if (chapterStop != null) chapterStop.set(pauseAfter);
    if (fader != null) fader.set(fadeIn, fadeOut);
  }
}
