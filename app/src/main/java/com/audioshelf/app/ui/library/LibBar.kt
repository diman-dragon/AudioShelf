package com.audioshelf.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.data.LibView
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.LibraryUi
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.IconBtn
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented

@Composable
fun LibBar(ui: LibraryUi, actions: LibraryActions) {
    val items = listOf(
        SegItem(LibView.Rows.id, icon = LibIcons.Rows, description = stringResource(R.string.lib_list)),
        SegItem(LibView.Cards.id, icon = LibIcons.Cards, description = stringResource(R.string.lib_cards)),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Segmented(items, ui.view.id, { actions.onView(LibView.of(it)) })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(
                icon = LibIcons.Refresh,
                description = stringResource(R.string.lib_rescan),
                onClick = actions::onRescan,
                enabled = !ui.scanning,
                spinning = ui.scanning,
            )
            Btn(stringResource(R.string.lib_add), actions::onAdd)
        }
    }
}
