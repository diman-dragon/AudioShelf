package com.audioshelf.app;

import android.content.Context;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;

/** One ExoPlayer for the whole app, shared by the plugin and the playback service. */
final class PlayerHolder {
  private static ExoPlayer player;

  private PlayerHolder() {}

  /** Must be called on the main thread. */
  static synchronized ExoPlayer get(Context ctx) {
    if (player == null) {
      player =
          new ExoPlayer.Builder(ctx.getApplicationContext())
              .setAudioAttributes(
                  new AudioAttributes.Builder()
                      .setUsage(C.USAGE_MEDIA)
                      .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                      .build(),
                  true) // pause on calls and when another app takes audio focus
              .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
              .setSeekBackIncrementMs(15000)
              .setSeekForwardIncrementMs(30000)
              .setWakeMode(C.WAKE_MODE_LOCAL)
              .build();
    }
    return player;
  }

  static synchronized void release() {
    if (player != null) {
      player.release();
      player = null;
    }
  }
}
