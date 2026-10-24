package com.audioshelf.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.library.BookRow
import com.audioshelf.app.library.LibraryActions
import com.audioshelf.app.library.Meta
import com.audioshelf.app.library.splitName
import com.audioshelf.app.player.Kind
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.Field
import com.audioshelf.app.ui.components.FieldBox
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.SegItem
import com.audioshelf.app.ui.components.Segmented
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun EditDialog(row: BookRow, actions: LibraryActions, onClose: () -> Unit) {
    val c = AppTheme.colors
    val book = row.book
    val (base, ext) = remember(book.id) { splitName(book.files[0].name) }
    var kind by remember { mutableStateOf(row.kind) }
    var title by remember { mutableStateOf(row.meta.title) }
    var author by remember { mutableStateOf(row.meta.author) }
    var narrator by remember { mutableStateOf(row.meta.narrator) }
    var year by remember { mutableStateOf(row.meta.year) }
    var name by remember { mutableStateOf(base) }
    val kinds = listOf(
        SegItem("book", stringResource(R.string.kind_book)),
        SegItem("album", stringResource(R.string.kind_album)),
    )
    ModalView(stringResource(R.string.edit_heading), onClose) {
        FieldBox(stringResource(R.string.edit_type), stringResource(R.string.edit_type_hint)) {
            Segmented(kinds, if (kind == Kind.Book) "book" else "album", { kind = if (it == "book") Kind.Book else Kind.Album })
        }
        Field(stringResource(R.string.edit_title), stringResource(R.string.edit_title_hint), title, { title = it }, placeholder = book.name)
        Field(
            stringResource(if (kind == Kind.Book) R.string.edit_author_book else R.string.edit_author_album),
            stringResource(R.string.edit_author_hint),
            author,
            { author = it },
        )
        if (kind == Kind.Book) {
            Field(stringResource(R.string.edit_narrator), stringResource(R.string.edit_narrator_hint), narrator, { narrator = it })
        }
        Field(
            stringResource(R.string.edit_year),
            stringResource(R.string.edit_year_hint),
            year,
            { year = it },
            keyboardType = KeyboardType.Number,
            maxLength = 4,
        )
        if (!book.folder) {
            Field(stringResource(R.string.edit_file), stringResource(R.string.edit_file_hint, ext), name, { name = it })
        }
        AText(stringResource(R.string.edit_note), 12f, c.muted)
        Actions {
            Btn(stringResource(R.string.lib_cancel), onClose, kind = BtnKind.Ghost)
            Btn(stringResource(R.string.edit_save), {
                val meta = Meta(kind, title.trim(), author.trim(), if (kind == Kind.Book) narrator.trim() else "", year.trim())
                actions.onEdit(book, meta, name.trim())
                onClose()
            })
        }
    }
}

@Composable
private fun Choice(label: String, hint: String, kind: BtnKind, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Btn(label, onClick, kind = kind, wide = true)
        AText(hint, 12f, AppTheme.colors.muted)
    }
}

@Composable
fun DeleteDialog(row: BookRow, actions: LibraryActions, onClose: () -> Unit) {
    val book = row.book
    ModalView(stringResource(R.string.del_heading), onClose) {
        AText(row.title, 16f, AppTheme.colors.text, weight = FontWeight.SemiBold)
        Choice(stringResource(R.string.del_hide), stringResource(R.string.del_hide_hint), BtnKind.Ghost) {
            actions.onHide(book)
            onClose()
        }
        Choice(
            stringResource(R.string.del_disk),
            stringResource(if (book.folder) R.string.del_disk_hint_folder else R.string.del_disk_hint),
            BtnKind.Danger,
        ) {
            actions.onDelete(book)
            onClose()
        }
        Actions { Btn(stringResource(R.string.lib_cancel), onClose, kind = BtnKind.Ghost) }
    }
}
