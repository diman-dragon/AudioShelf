package com.audioshelf.app.library

import com.audioshelf.app.player.Kind
import com.audioshelf.app.player.Placeholder

private const val BOOK_SIZE = 30L * 1048576
private const val BOOK_MS = 30L * 60000

fun looksLikeBook(book: Book, firstDurationMs: Long): Boolean {
    if (book.folder) return true
    val file = book.files[0]
    return file.name.lowercase().endsWith(".m4b") || file.size >= BOOK_SIZE || firstDurationMs >= BOOK_MS
}

fun kindOf(book: Book, meta: Meta, firstDurationMs: Long): Kind =
    meta.kind ?: if (looksLikeBook(book, firstDurationMs)) Kind.Book else Kind.Album

fun placeholderOf(kind: Kind, state: ProgressState): Pair<Placeholder, Boolean> = when {
    kind == Kind.Album -> Placeholder.Disc to false
    state == ProgressState.Started -> Placeholder.OpenBook to false
    else -> Placeholder.ClosedBook to (state == ProgressState.Done)
}
