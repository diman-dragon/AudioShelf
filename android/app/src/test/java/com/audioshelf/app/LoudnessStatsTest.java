package com.audioshelf.app;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;

public class LoudnessStatsTest {
  private static final int RATE = 8000;

  private static LoudnessStats measured(double amplitude, double seconds) {
    LoudnessStats stats = new LoudnessStats(RATE, 1);
    int n = (int) (RATE * seconds);
    ByteBuffer buf = ByteBuffer.allocate(n * 2).order(ByteOrder.nativeOrder());
    for (int i = 0; i < n; i++) buf.putShort((short) Math.round(amplitude * 32767 * Math.sin(2 * Math.PI * 440 * i / RATE)));
    buf.flip();
    stats.add(buf, false);
    return stats;
  }

  @Test
  public void silenceGivesNoCorrection() {
    assertEquals(0.0, measured(0, 3).gainDb(), 0.0);
  }

  @Test
  public void quietFileIsRaisedToTheTarget() {
    assertEquals(10.0, measured(0.0709, 3).gainDb(), 0.3);
  }

  @Test
  public void loudFileIsLowered() {
    assertEquals(-10.0, measured(0.709, 3).gainDb(), 0.3);
  }

  @Test
  public void correctionIsLimitedToTwelveDecibels() {
    assertEquals(12.0, measured(0.011, 3).gainDb(), 0.1);
  }
}
