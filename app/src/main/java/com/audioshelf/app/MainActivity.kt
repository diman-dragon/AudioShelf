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
        getSharedPreferences("diag", MODE_PRIVATE).edit { putBoolean("bluetooth_asked", true) }
        container.bluetooth.onPermissionResult(granted)
    }

    /** Asked once: it lets the log record Bluetooth devices by name. Nothing else uses it. */
    private fun askBluetooth() {
        if (Build.VERSION.SDK_INT < 31 || container.bluetooth.hasPermission()) return
        val prefs = getSharedPreferences("diag", MODE_PRIVATE)
        if (prefs.getBoolean("bluetooth_asked", false)) return
        AppLog.i("Permission", "requesting Bluetooth permission for device and route diagnostics")
        bluetooth.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    override fun onStart() {
        super.onStart()
        AppLog.i("Activity", "app in front")
    }

    override fun onStop() {
        AppLog.i("Activity", "app in background")
        player.remember()
        container.kv.flushNow()
        super.onStop()
    }
}
