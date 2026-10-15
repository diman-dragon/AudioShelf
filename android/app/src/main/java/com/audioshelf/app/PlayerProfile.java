package com.audioshelf.app;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import org.json.JSONObject;

/** Playback mode, file-edge effects, sleep timer and audio focus type; books and albums get different ones from JavaScript. */
final class PlayerProfile {
  private static EdgeFader fader;
  private static ChapterStop chapterStop;
  private static SleepTimer sleep;
  private static boolean speech;

  private PlayerProfile() {}

  /** Called once, right after the player is built (main thread). */
  static synchronized void attach(ExoPlayer player) {
    Runnable volume = () -> refreshVolume(player);
    fader = new EdgeFader(player, volume);
    chapterStop = new ChapterStop(player);
    sleep = new SleepTimer(player, volume);
    speech = false;
  }

  static synchronized void detach() {
    if (fader != null) fader.stop();
    fader = null;
    chapterStop = null;
    if (sleep != null) sleep.stop();
    sleep = null;
  }

  /** One writer of the volume: the edge fade times the sleep-timer fade. */
  static synchronized void refreshVolume(ExoPlayer player) {
    float edge = fader == null ? 1f : fader.factor();
    float timer = sleep == null ? 1f : sleep.factor();
    player.setVolume(edge * timer);
  }

  static synchronized void setSleep(long minutes, boolean chapter) {
    if (sleep != null) sleep.set(minutes, chapter);
  }

  /** Milliseconds of the sleep timer left; 0 when off. */
  static synchronized long sleepLeft() {
    return sleep == null ? 0 : sleep.left();
  }

  static synchronized boolean sleepAtChapterEnd() {
    return sleep != null && sleep.chapterEnd();
  }

  /**
   * repeat: "off" | "all" | "one"; shuffle; pauseAfter (stop after each chapter); fadeIn / fadeOut in ms;
   * speech: a book (a short notification pauses it instead of lowering the volume).
   */
  static synchronized void apply(ExoPlayer player, JSONObject d) {
    String repeat = d.optString("repeat", "off");
    player.setRepeatMode(
        "all".equals(repeat)
            ? Player.REPEAT_MODE_ALL
            : "one".equals(repeat) ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    player.setShuffleModeEnabled(d.optBoolean("shuffle", false));
    if (chapterStop != null) chapterStop.set(d.optBoolean("pauseAfter", false));
    if (fader != null) fader.set(d.optInt("fadeIn", 0), d.optInt("fadeOut", 0));
    boolean wantSpeech = d.optBoolean("speech", false);
    if (wantSpeech != speech) {
      speech = wantSpeech;
      player.setAudioAttributes(PlayerHolder.attributes(wantSpeech), true);
    }
  }
}
