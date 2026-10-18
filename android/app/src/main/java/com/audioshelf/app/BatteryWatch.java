package com.audioshelf.app;

import android.content.Context;
import android.os.SystemClock;

final class BatteryWatch {
  private static final long EVERY_MS = 5000;

  private final Context ctx;
  private final boolean onlyCharging;
  private long next;
  private volatile boolean tripped;

  BatteryWatch(Context ctx, boolean onlyCharging) {
    this.ctx = ctx;
    this.onlyCharging = onlyCharging;
  }

  boolean tripped() {
    if (tripped) return true;
    long now = SystemClock.elapsedRealtime();
    if (now < next) return false;
    next = now + EVERY_MS;
    tripped = BatteryGuard.shouldPause(ctx, onlyCharging);
    return tripped;
  }
}
