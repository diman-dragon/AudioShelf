package com.audioshelf.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.player.PlayMode
import com.audioshelf.app.player.PlayerActions
import com.audioshelf.app.player.PlayerUi
import com.audioshelf.app.player.SleepChoice
import com.audioshelf.app.player.fmtSpeed
import com.audioshelf.app.player.fmtTime
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme

private val SHAPE = RoundedCornerShape(12.dp)

@Composable
fun ExtrasRow(ui: PlayerUi, actions: PlayerActions, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val sleepOn = ui.sleep.choice != SleepChoice.Off
    val modeLabel = stringResource(modeShortRes(ui.mode))
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        Extra(stringResource(R.string.player_speed), false, true, actions::onSpeed) {
            AText(fmtSpeed(ui.speed), 15f, c.text, weight = FontWeight.Bold, lineHeight = 22f, maxLines = 1)
        }
        Extra(sleepLabel(ui), sleepOn, true, actions::onSleep) {
            SvgIcon(Icons.Moon, c.text, 22.dp)
        }
        Extra(stringResource(R.string.player_list), false, ui.multi, actions::onList) {
            SvgIcon(Icons.List, c.text, 22.dp)
        }
        Extra(modeLabel, false, true, actions::onMode) {
            SvgIcon(Icons.mode(ui.mode), c.text, 22.dp)
        }
        Extra(
            if (ui.marks > 0) stringResource(R.string.player_marks_n, ui.marks) else stringResource(R.string.player_marks),
            false,
            true,
            actions::onMarks,
        ) {
            SvgIcon(Icons.Mark, c.text, 22.dp)
        }
    }
}

@Composable
private fun sleepLabel(ui: PlayerUi): String = when {
    ui.sleep.choice == SleepChoice.Chapter -> stringResource(R.string.sleep_chapter)
    ui.sleep.leftMs > 0 -> fmtTime(ui.sleep.leftMs)
    ui.sleep.choice == SleepChoice.Off -> stringResource(R.string.player_sleep)
    else -> stringResource(R.string.sleep_min, ui.sleep.choice.minutes)
}

private fun modeShortRes(mode: PlayMode): Int = when (mode) {
    PlayMode.Continue -> R.string.mode_short_continue
    PlayMode.Chapter -> R.string.mode_short_chapter
    PlayMode.Off -> R.string.mode_short_off
    PlayMode.All -> R.string.mode_short_all
    PlayMode.One -> R.string.mode_short_one
    PlayMode.Shuffle -> R.string.mode_short_shuffle
}

@Composable
private fun RowScope.Extra(
    label: String,
    on: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val c = AppTheme.colors
    Column(
        Modifier
            .weight(1f)
            .clip(SHAPE)
            .background(c.surface)
            .border(1.dp, if (on) c.accent else c.line, SHAPE)
            .tap(enabled = enabled, description = label, onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) { icon() }
        AText(
            text = label,
            size = 11f,
            color = if (on) c.accent else c.muted,
            align = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
