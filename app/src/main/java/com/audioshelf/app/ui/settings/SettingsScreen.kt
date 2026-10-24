package com.audioshelf.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.data.LANG_AUTO
import com.audioshelf.app.data.nextSkip
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Cycler
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.components.noSwipe
import com.audioshelf.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(ui: SettingsUi, actions: SettingsActions, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val titles = listOf(stringResource(R.string.settings_page_app), stringResource(R.string.settings_section_about))
    val pager = rememberPagerState(initialPage = SettingsNav.index.coerceIn(0, titles.lastIndex)) { titles.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { SettingsNav.index = it } }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            titles.forEachIndexed { i, title ->
                Chip(title, pager.currentPage == i) { scope.launch { pager.animateScrollToPage(i) } }
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth().noSwipe()) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            ) {
                if (page == 0) AppPage(ui, actions) else AboutCard(ui.version)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            titles.indices.forEach { i ->
                val on = pager.currentPage == i
                Row(
                    Modifier
                        .width(if (on) 20.dp else 7.dp)
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (on) c.accent else c.line),
                ) {}
            }
        }
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
    SettingRow(stringResource(R.string.settings_skip_back)) {
        Cycler(String.format(sec, ui.skipBack), { actions.onSkipBack(nextSkip(ui.skipBack)) })
    }
    SettingRow(stringResource(R.string.settings_skip_forward)) {
        Cycler(String.format(sec, ui.skipForward), { actions.onSkipForward(nextSkip(ui.skipForward)) })
    }
    SwitchRow(stringResource(R.string.settings_energy), ui.offload, actions::onOffload, stringResource(R.string.energy_hint))
    SectionHeading(stringResource(R.string.settings_section_interface))
    SwitchRow(stringResource(R.string.settings_dark), ui.theme == "dark", { actions.onTheme(if (it) "dark" else "light") })
    SettingRow(stringResource(R.string.settings_language), stacked = true) {
        Segmented(langs, ui.language, actions::onLanguage, fill = true)
    }
}
