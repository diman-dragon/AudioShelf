package com.audioshelf.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerUi
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun Controls(ui: PlayerUi, actions: PlayerActions, modifier: Modifier = Modifier) {
    val back = remember(ui.skipBackSec) { Icons.back(ui.skipBackSec) }
    val forward = remember(ui.skipForwardSec) { Icons.forward(ui.skipForwardSec) }
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Skip(Icons.Prev, stringResource(R.string.player_prev), true, actions::onPrev)
        Skip(back, stringResource(R.string.player_back, ui.skipBackSec), true, actions::onBack)
        PlayButton(ui.playing, actions::onToggle)
        Skip(forward, stringResource(R.string.player_forward, ui.skipForwardSec), true, actions::onForward)
        Skip(Icons.Next, stringResource(R.string.player_next), ui.multi, actions::onNext)
    }
}

@Composable
private fun Skip(icon: SvgSpec, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .size(52.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .tap(enabled = enabled, description = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(icon, c.text, 36.dp)
    }
}

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val description = stringResource(if (playing) R.string.player_pause else R.string.player_play)
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(c.accent)
            .tap(description = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(if (playing) Icons.Pause else Icons.Play, c.onAccent, 34.dp)
    }
}
