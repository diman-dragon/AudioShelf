package com.audioshelf.app.ui.sound

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.audioshelf.app.sound.SoundActions
import com.audioshelf.app.sound.SoundUi

@Composable
fun SoundScreen(ui: SoundUi, actions: SoundActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        EqPanel(ui, actions)
        FxPanel(ui, actions)
    }
}
