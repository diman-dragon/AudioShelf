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

final class PlayerHolder {
  private static ExoPlayer player;

  private PlayerHolder() {}

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

  static AudioAttributes attributes(boolean speech) {
    return new AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(speech ? C.AUDIO_CONTENT_TYPE_SPEECH : C.AUDIO_CONTENT_TYPE_MUSIC)
        .build();
  }

  static synchronized ExoPlayer get(Context ctx) {
    if (player == null) {
      Context app = ctx.getApplicationContext();
      DefaultExtractorsFactory extractors = new DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true);
      player =
          new ExoPlayer.Builder(app, renderers(app), new DefaultMediaSourceFactory(app, extractors))
              .setAudioAttributes(attributes(false), true)
              .setHandleAudioBecomingNoisy(true)
              .setSeekParameters(SeekParameters.EXACT)
              .setSeekBackIncrementMs(15000)
              .setSeekForwardIncrementMs(30000)
              .setWakeMode(C.WAKE_MODE_LOCAL)
              .build();
      PlayerProfile.attach(player);
      PlayerSound.attach(player);
      PositionSaver.attach(app, player);
      new ResumeRewind(player);
      PlayerOffload.attach(player, "1".equals(ResumeStore.get(app, ResumeStore.OFFLOAD)));
    }
    return player;
  }

  static synchronized void release() {
    PositionSaver.detach();
    PlayerProfile.detach();
    PlayerSound.detach();
    PlayerOffload.detach();
    if (player != null) {
      player.release();
      player = null;
    }
  }
}
