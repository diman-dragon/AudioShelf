package com.audioshelf.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.audioshelf.app.data.Kv

private const val STORE = "audioshelf.battery"
private const val ASKED = "asked"

/**
 * With the screen off many phones cut a background app's CPU time, and the sound stutters until
 * the screen is switched on again. Being exempt from battery optimization is what lifts that
 * limit; this asks for it once, and the settings row can ask again.
 */
class BatteryGuard(private val app: Application, private val kv: Kv) {
    fun isExempt(): Boolean =
        (app.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(app.packageName) ?: true

    fun shouldAsk(): Boolean = !isExempt() && kv.get(STORE, ASKED) == null

    fun markAsked() = kv.put(STORE, ASKED, "1")

    /** Opens the system dialog that grants the exemption, or the list where it can be revoked when it is already granted. */
    fun open(context: Context) {
        val intents = listOf(
            if (isExempt()) Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            else Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${app.packageName}")),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")),
        )
        for (intent in intents) {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }
}
