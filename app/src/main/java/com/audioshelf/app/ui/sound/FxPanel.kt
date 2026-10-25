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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.sound.GAIN_STEPS
import com.audioshelf.app.sound.NOISE_MAX
import com.audioshelf.app.sound.NOISE_OFF
import com.audioshelf.app.sound.PITCH_MAX
import com.audioshelf.app.sound.PITCH_MIN
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

        AText(stringResource(R.string.fx_noise), 13f, c.muted, weight = FontWeight.SemiBold)
        Segmented(
            listOf(
                SegItem("0", stringResource(R.string.fx_noise_off)),
                SegItem("1", stringResource(R.string.fx_noise_light)),
                SegItem("2", stringResource(R.string.fx_noise_medium)),
                SegItem("3", stringResource(R.string.fx_noise_strong)),
            ),
            fx.noise.toString(),
            { actions.onNoise(it.toIntOrNull() ?: NOISE_OFF) },
            fill = true,
        )
        if (fx.noise in (NOISE_OFF + 1)..NOISE_MAX) AText(stringResource(R.string.fx_noise_hint), 12f, c.muted)

        AText(stringResource(R.string.fx_gain), 13f, c.muted, weight = FontWeight.SemiBold)
        Segmented(
            GAIN_STEPS.map { SegItem(it.toString(), if (it == 0) stringResource(R.string.fx_off) else "+$it") },
            fx.gainDb.toString(),
            { actions.onGain(it.toIntOrNull() ?: 0) },
            fill = true,
        )

        AText(stringResource(R.string.fx_pitch), 13f, c.muted, weight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("−", { actions.onPitch(fx.pitch - 1) }, kind = BtnKind.Ghost)
            AText(
                pitchLabel(fx.pitch), 16f, c.text, Modifier.weight(1f), FontWeight.SemiBold, TextAlign.Center, tabular = true,
            )
            Btn("+", { actions.onPitch(fx.pitch + 1) }, kind = BtnKind.Ghost)
            Btn(stringResource(R.string.sound_reset), { actions.onPitch(0) }, kind = BtnKind.Ghost)
        }
        if (fx.pitch <= PITCH_MIN || fx.pitch >= PITCH_MAX) AText(stringResource(R.string.fx_pitch_limit), 12f, c.muted)

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

private fun pitchLabel(semitones: Int): String = when {
    semitones > 0 -> "+$semitones"
    else -> semitones.toString()
}
