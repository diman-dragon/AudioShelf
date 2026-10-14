package com.audioshelf.app;

/** One peaking equalizer band (RBJ cookbook), normalized coefficients. */
final class Biquad {
  final float b0;
  final float b1;
  final float b2;
  final float a1;
  final float a2;

  private Biquad(double b0, double b1, double b2, double a1, double a2) {
    this.b0 = (float) b0;
    this.b1 = (float) b1;
    this.b2 = (float) b2;
    this.a1 = (float) a1;
    this.a2 = (float) a2;
  }

  static Biquad peaking(double freq, double gainDb, double q, double sampleRate) {
    double a = Math.pow(10, gainDb / 40);
    double w = 2 * Math.PI * freq / sampleRate;
    double alpha = Math.sin(w) / (2 * q);
    double cos = Math.cos(w);
    double a0 = 1 + alpha / a;
    return new Biquad(
        (1 + alpha * a) / a0, -2 * cos / a0, (1 - alpha * a) / a0, -2 * cos / a0, (1 - alpha / a) / a0);
  }
}
