package com.audioshelf.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Collects the loudness of decoded PCM: mean energy of 100 ms blocks (silence skipped) and the peak. */
final class LoudnessStats {
  private static final double SILENCE = Math.pow(10, -50.0 / 10); // blocks quieter than -50 dBFS are not counted
  static final double TARGET_DB = -16; // wanted level, dBFS (RMS)
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

  /** Adds decoded samples: 16-bit PCM, or 32-bit float when isFloat. */
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

  /** Gain in dB that brings the file to the target level without clipping; 0 if nothing was heard. */
  double gainDb() {
    if (energyBlocks == 0 || peak <= 0) return 0;
    double loud = 10 * Math.log10(energySum / energyBlocks);
    double gain = Math.max(-MAX_GAIN_DB, Math.min(MAX_GAIN_DB, TARGET_DB - loud));
    double headroom = -1 - 20 * Math.log10(peak); // keep the peak under -1 dBFS
    if (gain > headroom) gain = Math.max(-MAX_GAIN_DB, headroom);
    return Math.round(gain * 10) / 10.0;
  }
}
