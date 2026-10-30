package com.audioshelf.app.player

import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibraryController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The book the player is on, and the questions every part of the session asks about it. */
class CurrentBook(
    private val engine: PlaybackEngine,
    private val library: LibraryController,
    private val chapters: ChapterStore,
) {
    private val flow = MutableStateFlow<PlayBook?>(null)

    val track: StateFlow<PlayBook?> = flow.asStateFlow()

    fun get(): PlayBook? = flow.value

    fun set(book: PlayBook?) {
        flow.value = book
    }

    fun expand(base: Book): PlayBook = expandBook(base, chapters::of)

    fun isLoaded(t: PlayBook, s: PlayState): Boolean =
        s.loaded && s.count == t.items.size && t.items.any { it.uri == s.mediaId }

    fun loadedNow(t: PlayBook): Boolean = isLoaded(t, engine.state.value)

    fun kindOf(t: PlayBook): Kind = library.kindOfBook(t.base)
}
