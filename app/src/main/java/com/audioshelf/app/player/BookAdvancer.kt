package com.audioshelf.app.player

import com.audioshelf.app.data.Prefs
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.ProgressState
import com.audioshelf.app.library.QueueStore

/** What plays after a book ends: the next one in the queue, or the next unfinished book in the library. */
class BookAdvancer(
    private val queue: QueueStore,
    private val library: LibraryController,
    private val prefs: Prefs,
    private val current: CurrentBook,
    private val loader: BookLoader,
) {
    fun advance(t: PlayBook) {
        var next: Book? = null
        while (next == null) {
            val id = queue.pop() ?: break
            next = library.bookById(id)
        }
        if (next == null && prefs.autoNext.value && current.kindOf(t) == Kind.Book) {
            val rows = library.snapshot().rows
            val at = rows.indexOfFirst { it.book.id == t.id }
            if (at >= 0) next = rows.drop(at + 1).firstOrNull { it.state != ProgressState.Done && it.kind == Kind.Book }?.book
        }
        if (next != null && next.id != t.id) loader.open(next)
    }
}
