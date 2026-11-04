package com.audioshelf.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.AudioShelfApp

private const val MAX_BLUETOOTH_ASKS = 2

class MainActivity : ComponentActivity() {
    private val player: PlayerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = PlayerViewModel(container) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        askNotifications()
        setContent { AudioShelfApp(container, player) }
    }

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        AppLog.i("Permission", "notifications permission ${if (granted) "granted" else "denied"}")
        askBluetooth()
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < 33) {
            askBluetooth()
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) askBluetooth() else notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val bluetooth = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val prefs = getSharedPreferences("diag", MODE_PRIVATE)
        prefs.edit { putInt("bluetooth_asks", prefs.getInt("bluetooth_asks", 0) + 1) }
        container.bluetooth.onPermissionResult(granted)
    }

    /**
     * Asked at the next starts while it is missing, twice at most (after two refusals Android stops showing
     * the dialog; Settings has a button that opens the app's permissions then). It lets the app see the
     * Bluetooth headset (connection, stream state, codec) and the log record it by name.
     */
    private fun askBluetooth() {
        if (Build.VERSION.SDK_INT < 31 || container.bluetooth.hasPermission()) return
        val prefs = getSharedPreferences("diag", MODE_PRIVATE)
        if (prefs.getInt("bluetooth_asks", 0) >= MAX_BLUETOOTH_ASKS) return
        AppLog.i("Permission", "requesting Bluetooth permission for device and route diagnostics")
        bluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    override fun onStart() {
        super.onStart()
        AppLog.i("Activity", "app in front")
        container.bluetooth.onForeground()
    }

    override fun onStop() {
        AppLog.i("Activity", "app in background")
        player.remember()
        container.kv.flushNow()
        super.onStop()
    }
}
