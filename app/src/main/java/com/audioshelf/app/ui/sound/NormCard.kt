package com.audioshelf.app.ui.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.sound.NormStatus
import com.audioshelf.app.sound.SoundActions
import com.audioshelf.app.sound.SoundUi
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.settings.SettingRow
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun NormCard(ui: SoundUi, actions: SoundActions) {
    val c = AppTheme.colors
    val n = ui.norm
    val running = n.status == NormStatus.Running
    val total = ui.totalFiles
    val fraction = if (total > 0) (total - ui.pending).toFloat() / total else 0f
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AText(stringResource(R.string.norm_title), 17f, c.text, weight = FontWeight.Bold)
        AText(stringResource(R.string.norm_hint), 13f, c.muted)
        if (running) {
            Btn(stringResource(R.string.norm_stop, n.done, n.total), actions::onNormStop, kind = BtnKind.Ghost)
        } else if (ui.pending > 0) {
            Btn(stringResource(R.string.norm_start), actions::onNormStart)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.line)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(c.accent))
        }
        AText(status(ui), 13f, c.text)
        if (running && n.current.isNotEmpty()) AText(n.current, 12f, c.muted, maxLines = 1)
        SettingRow(stringResource(R.string.norm_apply)) {
            Segmented(
                listOf(SegItem("on", stringResource(R.string.norm_on)), SegItem("off", stringResource(R.string.norm_off))),
                if (ui.normOn) "on" else "off",
                { actions.onNormOn(it == "on") },
            )
        }
    }
}

@Composable
private fun status(ui: SoundUi): String {
    val n = ui.norm
    return when {
        n.status == NormStatus.Running -> stringResource(R.string.norm_running, n.done, n.total)
        n.status == NormStatus.Battery -> stringResource(R.string.norm_battery, n.done, n.total)
        n.status == NormStatus.Cancelled -> stringResource(R.string.norm_cancelled, n.done, n.total)
        n.status == NormStatus.Done && n.total > 0 -> stringResource(R.string.norm_done, n.done - n.failed, n.failed)
        ui.totalFiles == 0 -> stringResource(R.string.norm_empty)
        ui.pending > 0 -> stringResource(R.string.norm_idle, ui.totalFiles - ui.pending, ui.totalFiles)
        else -> stringResource(R.string.norm_all_done)
    }
}
