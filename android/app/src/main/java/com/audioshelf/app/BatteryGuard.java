package com.audioshelf.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;

/** Heavy background work (loudness analysis) must not drain a weak battery. */
final class BatteryGuard {
  private static final int LOW_PERCENT = 20;

  private BatteryGuard() {}

  /** True when not charging and either Battery Saver is on or the charge is below 20%. */
  static boolean shouldPause(Context ctx) {
    return shouldPause(ctx, false);
  }

  /** onlyCharging: automatic background work runs only on the charger, so any unplugged state pauses it. */
  static boolean shouldPause(Context ctx, boolean onlyCharging) {
    Intent battery = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (battery == null) return false;
    int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) return false;
    if (onlyCharging) return true;
    PowerManager power = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
    if (power != null && power.isPowerSaveMode()) return true;
    int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    return level >= 0 && scale > 0 && level * 100 / scale < LOW_PERCENT;
  }
}
