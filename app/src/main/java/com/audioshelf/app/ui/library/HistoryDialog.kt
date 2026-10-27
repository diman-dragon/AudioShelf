package com.audioshelf.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.CoverSource
import com.audioshelf.app.library.QueueStore
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.player.Icons
import com.audioshelf.app.ui.player.SvgIcon
import com.audioshelf.app.ui.player.SvgSpec
import com.audioshelf.app.ui.theme.AppTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

private const val HISTORY_LIMIT = 30

/** History and queue use the same rows as the library: cover, title, author, progress. */
@Composable
fun HistoryDialog(rows: List<BookRow>, queue: QueueStore, covers: CoverSource, onPlay: (Book) -> Unit, onClose: () -> Unit) {
    val c = AppTheme.colors
    var queued by remember { mutableStateOf(queue.list()) }
    val byId = remember(rows) { rows.associateBy { it.book.id } }
    val history = remember(rows) { rows.filter { it.lastPlayed > 0 }.sortedByDescending { it.lastPlayed }.take(HISTORY_LIMIT) }
    ModalView(stringResource(R.string.history_title), onClose) {
        Header(Icons.List, stringResource(R.string.queue_title))
        val queuedRows = queued.mapNotNull { byId[it] }
        if (queuedRows.isEmpty()) {
            AText(stringResource(R.string.queue_empty), 14f, c.muted)
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Top) {
                queuedRows.forEach { row ->
                    CompactBookItem(
                        row, covers,
                        onOpen = {
                            queue.remove(row.book.id)
                            onClose()
                            onPlay(row.book)
                        },
                        trailing = {
                            Box(
                                Modifier.size(36.dp).tap(description = stringResource(R.string.marks_remove)) {
                                    queue.remove(row.book.id)
                                    queued = queue.list()
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                SvgIcon(LibIcons.Close, c.muted, 18.dp)
                            }
                        },
                    )
                }
            }
        }
        Header(LibIcons.History, stringResource(R.string.history_recent))
        if (history.isEmpty()) {
            AText(stringResource(R.string.history_empty), 14f, c.muted)
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                history.forEach { row ->
                    CompactBookItem(row, covers, onOpen = {
                        onClose()
                        onPlay(row.book)
                    })
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

@Composable
private fun Header(icon: SvgSpec, text: String) {
    val c = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SvgIcon(icon, c.muted, 18.dp)
        AText(text, 13f, c.muted, weight = FontWeight.SemiBold)
    }
}
