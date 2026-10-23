package com.audioshelf.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.components.rememberSwipeGuard
import com.audioshelf.app.ui.player.PlayerScreen
import com.audioshelf.app.ui.screens.ComingSoonScreen
import com.audioshelf.app.ui.theme.AudioShelfTheme

@Composable
fun AudioShelfApp(player: PlayerViewModel) {
    AudioShelfTheme {
        var screen by rememberSaveable { mutableStateOf(Screen.Library) }
        var direction by remember { mutableIntStateOf(0) }
        val guard = rememberSwipeGuard()

        val navigate = { target: Screen ->
            if (target != screen) {
                direction = if (target.ordinal > screen.ordinal) 1 else -1
                screen = target
            }
        }
        val goLibrary by rememberUpdatedState { navigate(Screen.Library) }
        val actions = remember(player) {
            object : PlayerActions by player {
                override fun onToLibrary() = goLibrary()
            }
        }

        BackHandler(enabled = screen != Screen.Library) { navigate(Screen.Library) }

        AppFrame(screen, direction, guard, navigate) { current ->
            when (current) {
                Screen.Player -> {
                    val ui by player.ui.collectAsState()
                    PlayerScreen(ui, actions)
                }
                else -> ComingSoonScreen(current)
            }
        }
    }
}
