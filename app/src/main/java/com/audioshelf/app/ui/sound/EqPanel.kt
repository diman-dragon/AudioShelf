package com.audioshelf.app.ui.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.sound.BAND_LABELS
import com.audioshelf.app.sound.EqScope
import com.audioshelf.app.sound.PRESETS
import com.audioshelf.app.sound.SoundActions
import com.audioshelf.app.sound.SoundUi
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.settings.Chip
import com.audioshelf.app.ui.theme.AppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqPanel(ui: SoundUi, actions: SoundActions) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AText(stringResource(R.string.sound_eq), 17f, c.text, weight = FontWeight.Bold)
        Segmented(
            listOf(
                SegItem(EqScope.Global.id, stringResource(R.string.sound_global)),
                SegItem(EqScope.Track.id, stringResource(R.string.sound_track)),
            ),
            ui.scope.id,
            { id -> if (id == EqScope.Global.id || ui.trackAvailable) actions.onScope(EqScope.of(id)) },
            fill = true,
        )
        val note = when {
            ui.scope == EqScope.Track -> stringResource(R.string.sound_editing_track, ui.trackTitle)
            ui.trackAvailable -> stringResource(R.string.sound_editing_global)
            else -> stringResource(R.string.sound_no_track)
        }
        AText(note, 13f, c.muted)
        AText(stringResource(R.string.sound_presets), 13f, c.muted, weight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { preset ->
                Chip(stringResource(presetRes(preset.id)), ui.eq.preset == preset.id) { actions.onPreset(preset.id) }
            }
        }
        AText(stringResource(R.string.sound_bands), 13f, c.muted, weight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BAND_LABELS.forEachIndexed { i, label ->
                BandSlider(label, ui.eq.bands[i], { actions.onBand(i, it) })
            }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.sound_reset), actions::onReset, kind = BtnKind.Ghost)
            if (ui.ownEq) Btn(stringResource(R.string.sound_use_global), actions::onUseGlobal, kind = BtnKind.Ghost)
        }
    }
}

private fun presetRes(id: String): Int = when (id) {
    "voice" -> R.string.preset_voice
    "rock" -> R.string.preset_rock
    "pop" -> R.string.preset_pop
    "jazz" -> R.string.preset_jazz
    "classical" -> R.string.preset_classical
    "electronic" -> R.string.preset_electronic
    "bass" -> R.string.preset_bass
    "treble" -> R.string.preset_treble
    else -> R.string.preset_flat
}
