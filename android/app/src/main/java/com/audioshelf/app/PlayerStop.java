package com.audioshelf.app;

import android.content.Context;
import androidx.media3.exoplayer.ExoPlayer;

final class PlayerStop {
  private PlayerStop() {}

  static void apply(Context ctx, ExoPlayer p) {
    ResumeStore.forgetSession(ctx);
    p.stop();
    p.clearMediaItems();
  }
}
