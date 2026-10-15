package com.audioshelf.app;

import androidx.media3.common.C;
import androidx.media3.common.audio.BaseAudioProcessor;
import java.nio.ByteBuffer;

/** 10-band equalizer plus a per-file loudness trim, applied to 16-bit PCM before the output. */
final class SoundProcessor extends BaseAudioProcessor {
  static final int BANDS = 10;
  private static final double[] FREQS = {31.25, 62.5, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
  private static final double Q = 1.41;

  private volatile float[] bandsDb = new float[BANDS];
  private volatile float trimDb;

  private int rate;
  private int channels;
  private float[][] z1;
  private float[][] z2;
  private Biquad[] filters = new Biquad[BANDS]; // null = band is flat
  private float[] builtFor;
  private float builtTrim = Float.NaN;
  private float gain = 1f;

  void setBands(float[] db) {
    bandsDb = db;
  }

  void setTrimDb(float db) {
    trimDb = db;
  }

  @Override
  protected AudioFormat onConfigure(AudioFormat input) throws UnhandledAudioFormatException {
    if (input.encoding != C.ENCODING_PCM_16BIT) return AudioFormat.NOT_SET; // leave other formats alone
    rate = input.sampleRate;
    channels = input.channelCount;
    z1 = new float[BANDS][channels];
    z2 = new float[BANDS][channels];
    builtFor = null;
    return input;
  }

  /** Rebuilds filters and the output gain when the settings changed. */
  private void refresh() {
    float[] db = bandsDb;
    float trim = trimDb;
    if (db == builtFor && trim == builtTrim) return;
    float peak = 0;
    for (int i = 0; i < BANDS; i++) {
      boolean on = db[i] != 0 && FREQS[i] < rate * 0.45;
      filters[i] = on ? Biquad.peaking(FREQS[i], db[i], Q, rate) : null;
      peak = Math.max(peak, db[i]);
    }
    gain = (float) Math.pow(10, (trim - peak) / 20); // boosting bands lowers the level: no clipping
    builtFor = db;
    builtTrim = trim;
  }

  private boolean allFlat() {
    for (Biquad f : filters) if (f != null) return false;
    return true;
  }

  @Override
  public void queueInput(ByteBuffer input) {
    int size = input.remaining();
    if (size == 0) return;
    refresh();
    ByteBuffer out = replaceOutputBuffer(size);
    if (gain == 1f && allFlat()) { // nothing to do: copy as is
      out.put(input);
      out.flip();
      return;
    }
    int channel = 0;
    while (input.remaining() >= 2) {
      float x = input.getShort();
      for (int b = 0; b < BANDS; b++) {
        Biquad f = filters[b];
        if (f == null) continue;
        float y = f.b0 * x + z1[b][channel];
        z1[b][channel] = f.b1 * x - f.a1 * y + z2[b][channel];
        z2[b][channel] = f.b2 * x - f.a2 * y;
        x = y;
      }
      x *= gain;
      out.putShort((short) Math.max(-32768, Math.min(32767, Math.round(x))));
      if (++channel == channels) channel = 0;
    }
    out.flip();
  }
}
