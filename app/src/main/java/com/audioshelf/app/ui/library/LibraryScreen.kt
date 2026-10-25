package com.audioshelf.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.data.LibView
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.CoverSource
import com.audioshelf.app.library.LibStatus
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.LibraryUi
import com.audioshelf.app.ui.components.EmptyState
import com.audioshelf.app.ui.components.LocalOverlay
import com.audioshelf.app.ui.components.SheetAction
import com.audioshelf.app.ui.components.sheet
import com.audioshelf.app.ui.theme.Dimens

@Composable
fun LibraryScreen(ui: LibraryUi, actions: LibraryActions, covers: CoverSource, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(Dimens.screenPadding)) {
        if (ui.barVisible) {
            LibBar(ui, actions)
            Spacer(Modifier.height(12.dp))
        }
        if (ui.hasBooks) {
            LibFilter(ui, actions)
            Spacer(Modifier.height(12.dp))
        }
        if (ui.scanning) {
            ScanPanel(ui.progress, actions::onStop)
            Spacer(Modifier.height(12.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                ui.rows.isNotEmpty() -> Items(ui, actions, covers)
                ui.hasBooks -> EmptyState(R.drawable.emoji_books, stringResource(R.string.lib_no_match), stringResource(R.string.lib_no_match_hint))
                !ui.scanning -> Empty(ui.status, actions)
            }
        }
    }
}

@Composable
private fun Empty(status: LibStatus, actions: LibraryActions) {
    val (title, hint) = when (status) {
        LibStatus.NoFolder -> R.string.lib_no_folder to R.string.lib_no_folder_hint
        LibStatus.Error -> R.string.lib_error to R.string.lib_error_hint
        LibStatus.Unscanned -> R.string.lib_unscanned to R.string.lib_unscanned_hint
        LibStatus.Ok -> R.string.lib_empty to R.string.lib_empty_hint
    }
    val toSettings = status == LibStatus.NoFolder || status == LibStatus.Error
    EmptyState(
        emoji = R.drawable.emoji_books,
        title = stringResource(title),
        hint = stringResource(hint),
        action = if (toSettings) stringResource(R.string.lib_choose) else null,
        onAction = actions::onOpenSettings,
    )
}

@Composable
private fun Items(ui: LibraryUi, actions: LibraryActions, covers: CoverSource) {
    val overlay = LocalOverlay.current
    val play = stringResource(R.string.lib_play)
    val edit = stringResource(R.string.lib_edit)
    val open = stringResource(R.string.lib_open)
    val delete = stringResource(R.string.lib_delete)
    val queue = stringResource(R.string.queue_add)
    val menu: (BookRow) -> Unit = { row ->
        val entries = mutableListOf(
            SheetAction(play) { actions.onPlay(row.book) },
            SheetAction(queue) { actions.onQueue(row.book) },
            SheetAction(edit) { overlay.show { EditDialog(row, actions) { overlay.close() } } },
        )
        if (!row.book.folder) entries.add(SheetAction(open) { actions.onOpenExternal(row.book) })
        entries.add(SheetAction(delete, danger = true) { overlay.show { DeleteDialog(row, actions) { overlay.close() } } })
        overlay.sheet(row.title, entries)
    }
    if (ui.view == LibView.Cards) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            gridItems(ui.rows, key = { it.book.id }) { row ->
                CardBookItem(row, covers, { actions.onPlay(row.book) }, { menu(row) })
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(ui.rows, key = { it.book.id }) { row ->
                ListBookItem(row, covers, { actions.onPlay(row.book) }, { menu(row) })
            }
        }
    }
}
