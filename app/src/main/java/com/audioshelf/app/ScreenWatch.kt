package com.audioshelf.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether the screen is on. With a dark screen nobody looks at the app, so nothing is written or redrawn. */
class ScreenWatch(app: Application) {
    private val flow = MutableStateFlow(interactive(app))

    val on: StateFlow<Boolean> = flow.asStateFlow()

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                flow.value = intent.action != Intent.ACTION_SCREEN_OFF
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun interactive(app: Application): Boolean =
        (app.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive ?: true
}
