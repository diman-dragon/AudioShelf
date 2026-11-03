package com.audioshelf.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import com.audioshelf.app.data.Kv

private const val STORE = "audioshelf.battery"
private const val ASKED = "asked"

/**
 * With the screen off many phones cut a background app's CPU time, and the sound stutters until
 * the screen is switched on again. Being exempt from battery optimization is what lifts that
 * limit; this opens the system battery-optimization list so the user can choose an exemption.
 */
class BatteryGuard(private val app: Application, private val kv: Kv) {
    fun isExempt(): Boolean =
        (app.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(app.packageName) ?: true

    fun shouldAsk(): Boolean = !isExempt() && kv.get(STORE, ASKED) == null

    fun markAsked() = kv.put(STORE, ASKED, "1")

    /** Opens system battery settings, then falls back to this app's details page if unavailable. */
    fun open(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri()),
        )
        AppLog.i("BatteryGuard", "asked; exempt=${isExempt()}")
        for (intent in intents) {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) {
                AppLog.i("BatteryGuard", "opened ${intent.action} (exempt=${isExempt()})")
                return
            }
        }
    }
}
