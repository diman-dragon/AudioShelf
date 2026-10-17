package com.audioshelf.app;

/** Below 80% of full scale the signal is untouched; above it peaks bend smoothly towards full scale instead of clipping. */
final class SoftLimiter {
  private static final float KNEE = 0.8f * 32768f;

  private SoftLimiter() {}

  static float soften(float x) {
    float a = Math.abs(x);
    if (a <= KNEE) return x;
    float room = 32767f - KNEE;
    float y = KNEE + room * (float) Math.tanh((a - KNEE) / room);
    return x < 0 ? -y : y;
  }
}
