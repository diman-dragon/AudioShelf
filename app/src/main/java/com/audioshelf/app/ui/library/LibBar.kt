package com.audioshelf.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.ProgressFilter
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.LibraryUi
import com.audioshelf.app.ui.components.Cycler
import com.audioshelf.app.ui.components.IconBtn

private fun kindLabel(kind: KindFilter) = when (kind) {
    KindFilter.All -> R.string.filter_all
    KindFilter.Book -> R.string.filter_books
    KindFilter.Album -> R.string.filter_albums
}

private fun stateLabel(state: ProgressFilter) = when (state) {
    ProgressFilter.All -> R.string.filter_any
    ProgressFilter.New -> R.string.filter_new
    ProgressFilter.Started -> R.string.filter_started
    ProgressFilter.Done -> R.string.filter_done
}

private fun <T> next(all: List<T>, current: T): T = all[(all.indexOf(current) + 1) % all.size]

/** Second row: type and status filters (tap to cycle), history, refresh. */
@Composable
fun LibBar(ui: LibraryUi, actions: LibraryActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ui.hasBooks) {
            Cycler(
                text = stringResource(kindLabel(ui.kind)),
                onNext = { actions.onKind(next(KindFilter.entries, ui.kind)) },
                description = stringResource(R.string.filter_kind),
                modifier = Modifier.weight(1f),
                active = ui.kind != KindFilter.All,
                horizontal = 10.dp,
            )
            Cycler(
                text = stringResource(stateLabel(ui.progressFilter)),
                onNext = { actions.onProgress(next(ProgressFilter.entries, ui.progressFilter)) },
                description = stringResource(R.string.filter_state),
                modifier = Modifier.weight(1f),
                active = ui.progressFilter != ProgressFilter.All,
                horizontal = 10.dp,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        IconBtn(
            icon = LibIcons.History,
            description = stringResource(R.string.history_title),
            onClick = actions::onOpenHistory,
        )
        IconBtn(
            icon = LibIcons.Refresh,
            description = stringResource(R.string.lib_rescan),
            onClick = actions::onRescan,
            enabled = !ui.scanning,
            spinning = ui.scanning,
        )
    }
}
