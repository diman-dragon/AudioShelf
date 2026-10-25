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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.player.PlayerViewModel
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.TextInput
import com.audioshelf.app.ui.components.tap
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun NotesDialog(player: PlayerViewModel, onClose: () -> Unit) {
    val c = AppTheme.colors
    val id = remember { player.bookId().orEmpty() }
    var text by remember { mutableStateOf(player.notes.note(id)) }
    val description by produceState<String?>(null, id) { value = player.description() }
    ModalView(stringResource(R.string.notes_title), onClose) {
        description?.let {
            AText(stringResource(R.string.notes_description), 13f, c.muted, weight = FontWeight.SemiBold)
            AText(it, 14f, c.text, Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()))
        }
        TextInput(
            value = text,
            onChange = { text = it },
            modifier = Modifier.heightIn(min = 120.dp),
            placeholder = stringResource(R.string.notes_hint),
            singleLine = false,
            imeAction = ImeAction.Default,
        )
        Actions {
            Btn(stringResource(R.string.marks_close), onClose, kind = BtnKind.Ghost)
            Btn(stringResource(R.string.notes_save), {
                if (id.isNotEmpty()) player.notes.setNote(id, text)
                onClose()
            })
        }
    }
}

@Composable
fun PeopleDialog(player: PlayerViewModel, onClose: () -> Unit) {
    val c = AppTheme.colors
    val id = remember { player.bookId().orEmpty() }
    var people by remember { mutableStateOf(player.notes.people(id)) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    ModalView(stringResource(R.string.people_title), onClose) {
        if (people.isEmpty()) {
            AText(stringResource(R.string.people_empty), 14f, c.muted)
        } else {
            Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                people.forEachIndexed { index, person ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            AText(person.name, 15f, c.text, weight = FontWeight.SemiBold)
                            if (person.note.isNotEmpty()) AText(person.note, 13f, c.muted)
                        }
                        AText(
                            "✕", 16f, c.muted,
                            Modifier.tap(description = stringResource(R.string.marks_remove)) {
                                player.notes.removePerson(id, index)
                                people = player.notes.people(id)
                            }.padding(10.dp),
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(name, { name = it }, placeholder = stringResource(R.string.people_name), maxLength = 60)
            TextInput(note, { note = it }, placeholder = stringResource(R.string.people_note), maxLength = 200)
        }
        Actions {
            Btn(stringResource(R.string.marks_close), onClose, kind = BtnKind.Ghost)
            Btn(stringResource(R.string.people_add), {
                if (player.notes.addPerson(id, name, note)) {
                    name = ""
                    note = ""
                    people = player.notes.people(id)
                }
            })
        }
    }
}
