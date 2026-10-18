package com.audioshelf.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EqChainTest {
  private static float[] flat() {
    return new float[EqChain.BANDS];
  }

  private static double amplitude(EqChain eq, double freq, int rate) {
    double peak = 0;
    for (int i = 0; i < rate; i++) {
      float y = eq.process((float) (10000 * Math.sin(2 * Math.PI * freq * i / rate)), 0);
      if (i > rate / 2) peak = Math.max(peak, Math.abs(y));
    }
    return peak;
  }

  @Test
  public void flatEqualizerIsFlatAndPassesSignalThrough() {
    EqChain eq = new EqChain();
    eq.reset(1);
    assertEquals(0f, eq.build(flat(), 44100), 0f);
    assertTrue(eq.flat());
    assertEquals(1234f, eq.process(1234f, 0), 0f);
  }

  @Test
  public void buildReturnsTheHighestBoost() {
    float[] db = flat();
    db[3] = 4;
    db[6] = 9;
    EqChain eq = new EqChain();
    eq.reset(2);
    assertEquals(9f, eq.build(db, 44100), 0f);
    assertFalse(eq.flat());
  }

  @Test
  public void boostedBandRaisesItsFrequencyOnly() {
    float[] db = flat();
    db[5] = 12;
    EqChain eq = new EqChain();
    eq.reset(1);
    eq.build(db, 44100);
    double at1k = amplitude(eq, 1000, 44100);
    eq = new EqChain();
    eq.reset(1);
    eq.build(db, 44100);
    double at100 = amplitude(eq, 100, 44100);
    assertEquals(10000 * Math.pow(10, 12 / 20.0), at1k, 400);
    assertEquals(10000, at100, 600);
  }

  @Test
  public void bandsAboveTheNyquistLimitAreSkipped() {
    float[] db = flat();
    db[9] = 12;
    EqChain eq = new EqChain();
    eq.reset(1);
    eq.build(db, 8000);
    assertTrue(eq.flat());
  }
}
