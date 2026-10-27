package com.audioshelf.app.ui.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.audioshelf.app.ui.components.Cycler
import com.audioshelf.app.ui.player.SPath
import com.audioshelf.app.ui.player.SvgSpec
import com.audioshelf.app.ui.theme.AppTheme

private val BODY = "M3 9.5h3L10 6v12l-4-3.5H3z"
private val WAVES = listOf(
    "M12.5 10.2a2.6 2.6 0 0 1 0 3.6",
    "M15 8.2a5.4 5.4 0 0 1 0 7.6",
    "M17.5 6.2a8.2 8.2 0 0 1 0 11.6",
    "M20 4.2a11 11 0 0 1 0 15.6",
)

/** A speaker; the number of brackets is the boost level (0 = crossed out, 1..4 = +3..+12 dB). */
private val GAIN_ICONS: List<SvgSpec> = GAIN_STEPS.indices.map { level ->
    val shapes = mutableListOf(SPath(BODY))
    if (level == 0) {
        shapes += SPath("M14 9.5l5 5M19 9.5l-5 5")
    } else {
        WAVES.take(level).forEach { shapes += SPath(it) }
    }
    SvgSpec(true, shapes)
}

@Composable
fun FxPanel(ui: SoundUi, actions: SoundActions) {
    val c = AppTheme.colors
    val gain = ui.fx.gainDb
    val level = GAIN_STEPS.indexOf(gain).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AText(stringResource(R.string.fx_title), 17f, c.text, weight = FontWeight.Bold)

        AText(stringResource(R.string.fx_gain), 13f, c.muted, weight = FontWeight.SemiBold)
        Cycler(
            text = "",
            onNext = { actions.onGain(GAIN_STEPS[(level + 1) % GAIN_STEPS.size]) },
            description = stringResource(R.string.fx_gain),
            active = gain != 0,
            icon = GAIN_ICONS[level],
        )
        if (ui.ownFx) {
            Row(Modifier.padding(top = 4.dp)) {
                Btn(stringResource(R.string.sound_use_global), actions::onFxUseGlobal, kind = BtnKind.Ghost)
            }
        }
    }
}
