package com.audioshelf.app;

import android.content.Context;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.extractor.DefaultExtractorsFactory;

/** One ExoPlayer for the whole app, shared by the plugin and the playback service. */
final class PlayerHolder {
  private static ExoPlayer player;

  private PlayerHolder() {}

  /** The default renderers, but the audio goes through our equalizer first. */
  private static DefaultRenderersFactory renderers(Context app) {
    return new DefaultRenderersFactory(app) {
      @Override
      protected AudioSink buildAudioSink(
          Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams) {
        return new DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(new AudioProcessor[] {PlayerSound.processor()})
            .build();
      }
    };
  }

  /** Must be called on the main thread. */
  static synchronized ExoPlayer get(Context ctx) {
    if (player == null) {
      Context app = ctx.getApplicationContext();
      // Many mp3 files have no seek table: without this, seeking in them does nothing.
      DefaultExtractorsFactory extractors = new DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);
      player =
          new ExoPlayer.Builder(app, renderers(app), new DefaultMediaSourceFactory(app, extractors))
              .setAudioAttributes(
                  new AudioAttributes.Builder()
                      .setUsage(C.USAGE_MEDIA)
                      .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                      .build(),
                  true) // pause on calls and when another app takes audio focus
              .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
              .setSeekParameters(SeekParameters.EXACT) // ±N seconds land exactly, not on the nearest frame
              .setSeekBackIncrementMs(15000)
              .setSeekForwardIncrementMs(30000)
              .setWakeMode(C.WAKE_MODE_LOCAL)
              .build();
      PlayerProfile.attach(player);
      PlayerSound.attach(player);
    }
    return player;
  }

  static synchronized void release() {
    PlayerProfile.detach();
    PlayerSound.detach();
    if (player != null) {
      player.release();
      player = null;
    }
  }
}
