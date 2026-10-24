package com.audioshelf.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.SortMode
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.LibraryUi
import com.audioshelf.app.ui.components.IconBtn
import com.audioshelf.app.ui.components.LocalOverlay
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.components.SheetAction
import com.audioshelf.app.ui.components.TextInput
import com.audioshelf.app.ui.components.sheet
import com.audioshelf.app.ui.theme.AppTheme
import kotlinx.coroutines.delay

private const val SEARCH_DELAY_MS = 150L

private fun sortLabel(sort: SortMode) = when (sort) {
    SortMode.Date -> R.string.sort_date
    SortMode.Name -> R.string.sort_name
    SortMode.Played -> R.string.sort_played
}

@Composable
fun LibFilter(ui: LibraryUi, actions: LibraryActions) {
    val overlay = LocalOverlay.current
    val sortTitle = stringResource(R.string.lib_sort)
    val sortLabels = SortMode.entries.map { it to stringResource(sortLabel(it)) }
    val kinds = listOf(
        SegItem(KindFilter.All.id, stringResource(R.string.filter_all)),
        SegItem(KindFilter.Book.id, stringResource(R.string.filter_books)),
        SegItem(KindFilter.Album.id, stringResource(R.string.filter_albums)),
    )
    var text by remember { mutableStateOf(ui.query) }
    LaunchedEffect(text) {
        if (text != ui.query) {
            delay(SEARCH_DELAY_MS)
            actions.onQuery(text)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Segmented(kinds, ui.kind.id, { actions.onKind(KindFilter.of(it)) }, fill = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextInput(
                value = text,
                onChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.lib_search),
                shape = RoundedCornerShape(percent = 50),
                background = AppTheme.colors.surface,
                horizontal = 14,
                imeAction = ImeAction.Search,
            )
            IconBtn(LibIcons.Sort, sortTitle, {
                overlay.sheet(
                    sortTitle,
                    sortLabels.map { (mode, label) ->
                        SheetAction("${if (mode == ui.sort) "✓" else " "} $label") { actions.onSort(mode) }
                    },
                )
            })
        }
    }
}
