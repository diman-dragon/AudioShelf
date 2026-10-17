package com.audioshelf.app;

/** The ten equalizer filters in a row, with their per-channel state. */
final class EqChain {
  static final int BANDS = 10;
  private static final double[] FREQS = {31.25, 62.5, 125, 250, 500, 1000, 2000, 4000, 8000, 16000};
  private static final double Q = 1.41;

  private final Biquad[] filters = new Biquad[BANDS]; // null = band is flat
  private float[][] z1 = new float[BANDS][0];
  private float[][] z2 = new float[BANDS][0];

  void reset(int channels) {
    z1 = new float[BANDS][channels];
    z2 = new float[BANDS][channels];
  }

  /** Builds the filters for these gains (dB); returns the highest boost, which the output gain has to make up for. */
  float build(float[] db, int rate) {
    float peak = 0;
    for (int i = 0; i < BANDS; i++) {
      boolean on = db[i] != 0 && FREQS[i] < rate * 0.45;
      filters[i] = on ? Biquad.peaking(FREQS[i], db[i], Q, rate) : null;
      peak = Math.max(peak, db[i]);
    }
    return peak;
  }

  boolean flat() {
    for (Biquad f : filters) if (f != null) return false;
    return true;
  }

  float process(float x, int channel) {
    for (int b = 0; b < BANDS; b++) {
      Biquad f = filters[b];
      if (f == null) continue;
      float y = f.b0 * x + z1[b][channel];
      z1[b][channel] = f.b1 * x - f.a1 * y + z2[b][channel];
      z2[b][channel] = f.b2 * x - f.a2 * y;
      x = y;
    }
    return x;
  }
}
