package com.audioshelf.app.ui.sound

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.sound.BOOSTS
import com.audioshelf.app.sound.SoundActions
import com.audioshelf.app.sound.SoundUi
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun BoostCard(ui: SoundUi, actions: SoundActions) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AText(stringResource(R.string.boost_title), 17f, c.text, weight = FontWeight.Bold)
        AText(stringResource(R.string.boost_hint), 13f, c.muted)
        Segmented(
            BOOSTS.map { SegItem(it.toString(), "$it%") },
            ui.boost.toString(),
            { actions.onBoost(it.toInt()) },
            Modifier.horizontalScroll(rememberScrollState()),
        )
        AText(stringResource(R.string.boost_warn), 13f, c.muted)
    }
}
