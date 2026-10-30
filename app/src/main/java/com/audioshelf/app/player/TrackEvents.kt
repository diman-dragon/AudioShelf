package com.audioshelf.app.player

import com.audioshelf.app.library.LibraryEvent
import com.audioshelf.app.library.NoteStore

/** What happens to the current book when the library changes under it: it is renamed, deleted or its folder is forgotten. */
class TrackEvents(
    private val engine: PlaybackEngine,
    private val current: CurrentBook,
    private val marks: MarkStore,
    private val notes: NoteStore,
    private val playPrefs: PlayPrefs,
    private val publisher: PlayerUiPublisher,
) {
    fun onLibraryEvent(event: LibraryEvent) {
        val t = current.get() ?: return
        when (event) {
            is LibraryEvent.Gone -> if (event.id == t.id) drop()
            is LibraryEvent.Moved -> if (event.from == t.id) retarget(t, event)
            LibraryEvent.FolderCleared -> drop()
        }
    }

    private fun retarget(t: PlayBook, event: LibraryEvent.Moved) {
        val next = t.base.copy(id = event.to, files = listOf(event.file))
        marks.move(event.from, event.to)
        notes.move(event.from, event.to)
        playPrefs.setLast(BookJson.encode(next))
        current.set(current.expand(next))
        publisher.publish()
    }

    private fun drop() {
        engine.stop()
        playPrefs.setLast(null)
        current.set(null)
        publisher.publish()
    }
}
