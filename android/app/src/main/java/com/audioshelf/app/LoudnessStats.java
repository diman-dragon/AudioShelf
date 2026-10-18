package com.audioshelf.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class LoudnessStats {
  private static final double SILENCE = Math.pow(10, -50.0 / 10);
  static final double TARGET_DB = -16;
  static final double MAX_GAIN_DB = 12;

  private final long blockSamples;
  private double blockSum;
  private long blockCount;
  private double energySum;
  private long energyBlocks;
  private double peak;

  LoudnessStats(int sampleRate, int channels) {
    blockSamples = Math.max(1, sampleRate / 10) * (long) channels;
  }

  void add(ByteBuffer data, boolean isFloat) {
    ByteBuffer buf = data.duplicate().order(ByteOrder.nativeOrder());
    while (buf.remaining() >= (isFloat ? 4 : 2)) {
      double x = isFloat ? buf.getFloat() : buf.getShort() / 32768.0;
      peak = Math.max(peak, Math.abs(x));
      blockSum += x * x;
      if (++blockCount == blockSamples) {
        double energy = blockSum / blockCount;
        if (energy > SILENCE) {
          energySum += energy;
          energyBlocks++;
        }
        blockSum = 0;
        blockCount = 0;
      }
    }
  }

  double gainDb() {
    if (energyBlocks == 0 || peak <= 0) return 0;
    double loud = 10 * Math.log10(energySum / energyBlocks);
    double gain = Math.max(-MAX_GAIN_DB, Math.min(MAX_GAIN_DB, TARGET_DB - loud));
    double headroom = -1 - 20 * Math.log10(peak);
    if (gain > headroom) gain = Math.max(-MAX_GAIN_DB, headroom);
    return Math.round(gain * 10) / 10.0;
  }
}
