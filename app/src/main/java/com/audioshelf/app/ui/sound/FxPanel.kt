package com.audioshelf.app.ui.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.sound.GAIN_STEPS
import com.audioshelf.app.sound.SoundActions
import com.audioshelf.app.sound.SoundUi
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.components.Toggle
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun FxPanel(ui: SoundUi, actions: SoundActions) {
    val c = AppTheme.colors
    val fx = ui.fx
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AText(stringResource(R.string.fx_title), 17f, c.text, weight = FontWeight.Bold)

        AText(stringResource(R.string.fx_gain), 13f, c.muted, weight = FontWeight.SemiBold)
        Segmented(
            GAIN_STEPS.map { SegItem(it.toString(), if (it == 0) stringResource(R.string.fx_off) else "+$it") },
            fx.gainDb.toString(),
            { actions.onGain(it.toIntOrNull() ?: 0) },
            fill = true,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AText(stringResource(R.string.fx_skip_silence), 16f, c.text)
                AText(stringResource(R.string.fx_skip_silence_hint), 12f, c.muted)
            }
            Toggle(fx.skipSilence, actions::onSkipSilence, stringResource(R.string.fx_skip_silence))
        }
        if (ui.ownFx) {
            Row(Modifier.padding(top = 4.dp)) {
                Btn(stringResource(R.string.sound_use_global), actions::onFxUseGlobal, kind = BtnKind.Ghost)
            }
        }
    }
}
