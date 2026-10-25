package com.audioshelf.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.library.fmtDate
import com.audioshelf.app.player.ChapterRow
import com.audioshelf.app.player.Kind
import com.audioshelf.app.player.Mark
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.player.fmtTime
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.LocalOverlay
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun ChapterListDialog(rows: List<ChapterRow>, kind: Kind, onPick: (Int) -> Unit, onClose: () -> Unit) {
    val c = AppTheme.colors
    ModalView(stringResource(if (kind == Kind.Album) R.string.list_tracks else R.string.list_chapters), onClose) {
        Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
            rows.forEach { row ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .tap {
                            onClose()
                            onPick(row.index)
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AText("${row.index + 1}", 13f, c.muted, Modifier.padding(end = 2.dp), tabular = true)
                    AText(
                        row.title, 15f, if (row.current) c.accent else c.text,
                        Modifier.weight(1f), if (row.current) FontWeight.Bold else FontWeight.Normal, maxLines = 2,
                    )
                    row.durationMs?.let { AText(fmtTime(it), 13f, c.muted, tabular = true) }
                }
            }
        }
        Actions { Btn(stringResource(R.string.marks_close), onClose, kind = BtnKind.Ghost) }
    }
}

@Composable
fun MarksDialog(player: PlayerViewModel, kind: Kind, onJump: (Mark) -> Unit, onClose: () -> Unit) {
    val c = AppTheme.colors
    var marks by remember { mutableStateOf(player.markList()) }
    val multi = player.isMulti()
    val locale = LocalConfiguration.current.locales[0]
    val overlay = LocalOverlay.current
    ModalView(stringResource(R.string.marks_title), onClose) {
        Btn(stringResource(R.string.marks_add), {
            player.addMark()
            marks = player.markList()
        }, wide = true)
        if (!player.canMark()) AText(stringResource(R.string.marks_add_hint), 12f, c.muted)
        if (marks.isEmpty()) {
            AText(stringResource(R.string.marks_empty), 14f, c.muted)
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                marks.forEach { mark ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).tap {
                                onClose()
                                onJump(mark)
                            },
                        ) {
                            val where = if (multi) {
                                stringResource(if (kind == Kind.Album) R.string.mark_track else R.string.mark_chapter, mark.index + 1) + " · "
                            } else ""
                            AText(where + fmtTime(mark.positionMs), 15f, c.text, tabular = true)
                            AText(fmtDate(mark.created, locale), 12f, c.muted)
                        }
                        AText(
                            "✕", 16f, c.muted,
                            Modifier.tap(description = stringResource(R.string.marks_remove)) {
                                player.removeMark(mark.created)
                                marks = player.markList()
                            }.padding(10.dp),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.notes_title), { overlay.show { NotesDialog(player) { overlay.close() } } }, Modifier.weight(1f), BtnKind.Ghost)
            Btn(stringResource(R.string.people_title), { overlay.show { PeopleDialog(player) { overlay.close() } } }, Modifier.weight(1f), BtnKind.Ghost)
        }
        Actions { Btn(stringResource(R.string.marks_close), onClose, kind = BtnKind.Ghost) }
    }
}
