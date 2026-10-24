package com.audioshelf.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.library.LibStatus
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.LocalOverlay
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun SectionHeading(text: String, first: Boolean = false) {
    AText(text, 17f, AppTheme.colors.text, Modifier.padding(top = if (first) 4.dp else 22.dp), FontWeight.Bold)
}

@Composable
fun SettingRow(label: String, stacked: Boolean = false, control: @Composable () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth()) {
        if (stacked) {
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AText(label, 16f, c.text)
                control()
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AText(label, 16f, c.text, Modifier.weight(1f))
                control()
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@Composable
fun FolderRow(ui: SettingsUi, actions: SettingsActions) {
    val c = AppTheme.colors
    val overlay = LocalOverlay.current
    val folder = ui.folder
    val internal = stringResource(R.string.path_internal)
    val card = stringResource(R.string.path_card)
    val path = if (folder == null) stringResource(R.string.settings_no_folder) else folderPath(folder.uri, folder.name, internal, card)
    val stat = when {
        ui.status == LibStatus.NoFolder || ui.status == LibStatus.Error -> ""
        ui.scanning -> stringResource(R.string.settings_scanning, ui.scanned)
        ui.status == LibStatus.Ok -> stringResource(R.string.settings_found, ui.books, ui.files)
        else -> ""
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                AText(stringResource(R.string.settings_folder), 16f, c.text)
                AText(path, 13f, c.text)
                if (stat.isNotEmpty()) AText(stat, 13f, c.text)
            }
            Column(Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(R.string.settings_choose), actions::onPickFolder, Modifier.fillMaxWidth())
                if (folder != null) {
                    Btn(
                        stringResource(R.string.settings_forget),
                        { overlay.show { ForgetDialog(actions) { overlay.close() } } },
                        Modifier.fillMaxWidth(),
                        BtnKind.Ghost,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}

@Composable
fun HiddenRow(count: Int, onRestore: () -> Unit) {
    SettingRow(stringResource(R.string.settings_hidden, count)) {
        Btn(stringResource(R.string.settings_restore), onRestore)
    }
}

@Composable
private fun ForgetDialog(actions: SettingsActions, onClose: () -> Unit) {
    ModalView(stringResource(R.string.forget_heading), onClose) {
        AText(stringResource(R.string.forget_text), 12f, AppTheme.colors.muted)
        Actions {
            Btn(stringResource(R.string.lib_cancel), onClose, kind = BtnKind.Ghost)
            Btn(stringResource(R.string.forget_ok), {
                onClose()
                actions.onForgetFolder()
            }, kind = BtnKind.Danger)
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    AText(
        text = text,
        size = 14f,
        color = if (selected) c.text else c.muted,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) c.accentSoft else c.surface)
            .border(1.dp, if (selected) c.accent else c.line, shape)
            .tap(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        weight = FontWeight.SemiBold,
        maxLines = 1,
    )
}
