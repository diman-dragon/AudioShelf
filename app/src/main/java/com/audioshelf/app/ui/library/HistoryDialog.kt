package com.audioshelf.app.ui.library

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
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.QueueStore
import com.audioshelf.app.library.fmtDate
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme

private const val HISTORY_LIMIT = 30

@Composable
fun HistoryDialog(rows: List<BookRow>, queue: QueueStore, onPlay: (Book) -> Unit, onClose: () -> Unit) {
    val c = AppTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    var queued by remember { mutableStateOf(queue.list()) }
    val byId = remember(rows) { rows.associateBy { it.book.id } }
    val history = remember(rows) { rows.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(HISTORY_LIMIT) }
    ModalView(stringResource(R.string.history_title), onClose) {
        AText(stringResource(R.string.queue_title), 13f, c.muted, weight = FontWeight.SemiBold)
        val queuedRows = queued.mapNotNull { byId[it] }
        if (queuedRows.isEmpty()) {
            AText(stringResource(R.string.queue_empty), 14f, c.muted)
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                queuedRows.forEachIndexed { index, row ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AText("${index + 1}", 13f, c.muted, Modifier.padding(end = 10.dp), tabular = true)
                        AText(
                            row.title, 15f, c.text,
                            Modifier.weight(1f).tap {
                                queue.remove(row.book.id)
                                onClose()
                                onPlay(row.book)
                            },
                            maxLines = 2,
                        )
                        AText(
                            "✕", 16f, c.muted,
                            Modifier.tap(description = stringResource(R.string.marks_remove)) {
                                queue.remove(row.book.id)
                                queued = queue.list()
                            }.padding(10.dp),
                        )
                    }
                }
            }
        }
        AText(stringResource(R.string.history_recent), 13f, c.muted, weight = FontWeight.SemiBold)
        if (history.isEmpty()) {
            AText(stringResource(R.string.history_empty), 14f, c.muted)
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                history.forEach { row ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(
                        Modifier.fillMaxWidth().tap {
                            onClose()
                            onPlay(row.book)
                        }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            AText(row.title, 15f, c.text, maxLines = 2)
                            AText(fmtDate(row.lastPlayed, locale), 12f, c.muted)
                        }
                        row.percent?.let { AText("$it%", 13f, c.muted, tabular = true) }
                    }
                }
            }
        }
        Actions {
            if (queuedRows.isNotEmpty()) {
                Btn(stringResource(R.string.queue_clear), {
                    queue.clear()
                    queued = queue.list()
                }, kind = BtnKind.Ghost)
            }
            Btn(stringResource(R.string.marks_close), onClose, kind = BtnKind.Ghost)
        }
    }
}
