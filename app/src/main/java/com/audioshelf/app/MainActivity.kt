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

    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStop() {
        player.remember()
        container.kv.flushNow()
        super.onStop()
    }
}
