package com.audioshelf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.AudioShelfApp

class MainActivity : ComponentActivity() {
    private val player: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AudioShelfApp(player) }
    }
}
