package com.audioshelf.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;

final class OffloadSupport {
  private OffloadSupport() {}

  static boolean supported(Context ctx) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
    AudioManager audio = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    if (audio == null) return false;
    AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    return has(audio, attrs, AudioFormat.ENCODING_MP3) || has(audio, attrs, AudioFormat.ENCODING_AAC_LC);
  }

  private static boolean has(AudioManager audio, AudioAttributes attrs, int encoding) {
    AudioFormat format = new AudioFormat.Builder().setEncoding(encoding).setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
    return audio.isOffloadedPlaybackSupported(format, attrs);
  }
}
