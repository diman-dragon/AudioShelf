package com.audioshelf.app.sound

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock

private const val LOW_PERCENT = 20
private const val EVERY_MS = 5000L

object BatteryGuard {
    fun shouldPause(context: Context, onlyCharging: Boolean): Boolean {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) return false
        if (onlyCharging) return true
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (power?.isPowerSaveMode == true) return true
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return level >= 0 && scale > 0 && level * 100 / scale < LOW_PERCENT
    }
}

class BatteryWatch(private val context: Context, private val onlyCharging: Boolean) {
    private var next = 0L
    @Volatile private var tripped = false

    fun tripped(): Boolean {
        if (tripped) return true
        val now = SystemClock.elapsedRealtime()
        if (now < next) return false
        next = now + EVERY_MS
        tripped = BatteryGuard.shouldPause(context, onlyCharging)
        return tripped
    }
}
