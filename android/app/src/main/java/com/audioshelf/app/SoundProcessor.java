package com.audioshelf.app;

import androidx.media3.common.C;
import androidx.media3.common.audio.BaseAudioProcessor;
import java.nio.ByteBuffer;

/** 10-band equalizer plus a per-file loudness trim and volume boost, applied to 16-bit PCM before the output. */
final class SoundProcessor extends BaseAudioProcessor {
  static final int BANDS = EqChain.BANDS;

  private volatile float[] bandsDb = new float[BANDS];
  private volatile float trimDb;
  private volatile float boost = 1f; // linear, 1 = as recorded

  private final EqChain eq = new EqChain();
  private int rate;
  private int channels;
  private float[] builtFor;
  private float builtTrim = Float.NaN;
  private float builtBoost = Float.NaN;
  private boolean limit;
  private float gain = 1f;

  void setBands(float[] db) {
    bandsDb = db;
  }

  void setTrimDb(float db) {
    trimDb = db;
  }

  /** Volume above 100%: linear factor (1..4); loud peaks are then softly limited instead of clipped. */
  void setBoost(float factor) {
    boost = Math.max(1f, Math.min(4f, factor));
  }

  @Override
  protected AudioFormat onConfigure(AudioFormat input) throws UnhandledAudioFormatException {
    if (input.encoding != C.ENCODING_PCM_16BIT) return AudioFormat.NOT_SET; // leave other formats alone
    rate = input.sampleRate;
    channels = input.channelCount;
    eq.reset(channels);
    builtFor = null;
    return input;
  }

  /** Rebuilds filters and the output gain when the settings changed. */
  private void refresh() {
    float[] db = bandsDb;
    float trim = trimDb;
    float factor = boost;
    if (db == builtFor && trim == builtTrim && factor == builtBoost) return;
    float peak = eq.build(db, rate);
    gain = (float) Math.pow(10, (trim - peak) / 20) * factor; // boosting bands lowers the level: no clipping
    limit = factor > 1f;
    builtBoost = factor;
    builtFor = db;
    builtTrim = trim;
  }

  @Override
  public void queueInput(ByteBuffer input) {
    int size = input.remaining();
    if (size == 0) return;
    refresh();
    ByteBuffer out = replaceOutputBuffer(size);
    if (gain == 1f && eq.flat()) { // nothing to do: copy as is
      out.put(input);
      out.flip();
      return;
    }
    int channel = 0;
    while (input.remaining() >= 2) {
      float x = eq.process(input.getShort(), channel) * gain;
      if (limit) x = SoftLimiter.soften(x);
      out.putShort((short) Math.max(-32768, Math.min(32767, Math.round(x))));
      if (++channel == channels) channel = 0;
    }
    out.flip();
  }
}
