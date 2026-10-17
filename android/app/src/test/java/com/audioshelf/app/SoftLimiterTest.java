package com.audioshelf.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoftLimiterTest {
  @Test
  public void quietSignalIsUntouched() {
    for (float x = -26000f; x <= 26000f; x += 1000f) assertEquals(x, SoftLimiter.soften(x), 0f);
  }

  @Test
  public void peaksNeverReachClippingAndKeepSign() {
    for (float x = 27000f; x <= 200000f; x += 3000f) {
      float y = SoftLimiter.soften(x);
      assertTrue(y > 0 && y <= 32767f);
      assertEquals(-y, SoftLimiter.soften(-x), 0f);
    }
  }

  @Test
  public void louderInputIsNeverQuieterOutput() {
    float prev = 0;
    for (float x = 0; x <= 100000f; x += 500f) {
      float y = SoftLimiter.soften(x);
      assertTrue(y >= prev);
      prev = y;
    }
  }
}
