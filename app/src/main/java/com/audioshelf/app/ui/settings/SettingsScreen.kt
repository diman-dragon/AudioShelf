package com.audioshelf.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.Btn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.data.LANG_AUTO
import com.audioshelf.app.data.GAIN_OPTIONS
import com.audioshelf.app.data.nextGain
import com.audioshelf.app.data.nextSkip
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Cycler
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun SettingsScreen(ui: SettingsUi, actions: SettingsActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
    ) {
        AppPage(ui, actions)
        Spacer(Modifier.height(28.dp))
        AboutCard(ui.version)
    }
}

@Composable
private fun AppPage(ui: SettingsUi, actions: SettingsActions) {
    val c = AppTheme.colors
    val langs = listOf(SegItem(LANG_AUTO, stringResource(R.string.lang_auto)), SegItem("ru", "Русский"), SegItem("en", "English"))
    SectionHeading(stringResource(R.string.settings_section_library), first = true)
    FolderRow(ui, actions)
    if (ui.hidden > 0) HiddenRow(ui.hidden, actions::onRestoreHidden)
    SectionHeading(stringResource(R.string.settings_section_playback))
    val sec = stringResource(R.string.unit_sec)
    val db = stringResource(R.string.unit_db)
    val off = stringResource(R.string.fx_off)
    SettingRow(stringResource(R.string.settings_skip_back)) {
        Cycler(String.format(sec, ui.skipBack), { actions.onSkipBack(nextSkip(ui.skipBack)) })
    }
    SettingRow(stringResource(R.string.settings_skip_forward)) {
        Cycler(String.format(sec, ui.skipForward), { actions.onSkipForward(nextSkip(ui.skipForward)) })
    }
    SettingRow(stringResource(R.string.settings_gain)) {
        Cycler(
            gainLabel(ui.gain),
            { actions.onGain(nextGain(ui.gain)) },
            description = if (ui.gain == 0) off else String.format(db, ui.gain),
        )
    }
    SwitchRow(stringResource(R.string.settings_auto_next), ui.autoNext, actions::onAutoNext, stringResource(R.string.auto_next_hint))
    SwitchRow(stringResource(R.string.settings_energy), ui.offload, actions::onOffload, stringResource(R.string.energy_hint))
    SectionHeading(stringResource(R.string.settings_section_data))
    SettingRow(stringResource(R.string.settings_stats)) {
        Btn(stringResource(R.string.settings_open), actions::onOpenStats)
    }
    SettingRow(stringResource(R.string.settings_backup), stacked = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.backup_save), actions::onBackup)
            Btn(stringResource(R.string.backup_restore), actions::onRestore, kind = BtnKind.Ghost)
        }
        AText(stringResource(R.string.backup_hint), 12f, c.muted)
    }
    SectionHeading(stringResource(R.string.settings_section_interface))
    SwitchRow(stringResource(R.string.settings_dark), ui.theme == "dark", { actions.onTheme(if (it) "dark" else "light") })
    SettingRow(stringResource(R.string.settings_language), stacked = true) {
        Segmented(langs, ui.language, actions::onLanguage, fill = true)
    }
}

/** A speaker with one more bracket for every step of boost: 🔈  🔈)  🔉))  🔊)))  🔊)))) */
private fun gainLabel(db: Int): String {
    val level = GAIN_OPTIONS.indexOf(db).coerceAtLeast(0)
    val speaker = when (level) {
        0, 1 -> "\uD83D\uDD08"
        2 -> "\uD83D\uDD09"
        else -> "\uD83D\uDD0A"
    }
    return speaker + ")".repeat(level)
}
