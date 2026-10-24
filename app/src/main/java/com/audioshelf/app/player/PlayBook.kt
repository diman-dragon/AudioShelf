package com.audioshelf.app.player

import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibFile
import com.audioshelf.app.library.splitName

class PlayItem(val file: LibFile, val title: String, val startMs: Long = 0, val endMs: Long = -1, val clipped: Boolean = false) {
    val uri: String get() = file.uri
    val lengthMs: Long get() = if (clipped && endMs > 0) endMs - startMs else -1
}

class PlayBook(val base: Book, val items: List<PlayItem>, val expanded: Boolean) {
    val id: String get() = base.id
}

fun expandBook(book: Book, chaptersOf: (LibFile) -> List<ChapterRec>?): PlayBook {
    var expanded = false
    val items = book.files.flatMap { file ->
        val list = chaptersOf(file)
        if (list == null || list.size < 2) {
            listOf(PlayItem(file, splitName(file.name).first))
        } else {
            expanded = true
            list.map { PlayItem(file, it.title, it.start, it.end, clipped = true) }
        }
    }
    return PlayBook(book, items, expanded)
}

fun remapper(book: Book, expanded: PlayBook): (Int, Long) -> Pair<Int, Long> = { index, position ->
    val file = book.files.getOrNull(index)
    val parts: List<IndexedValue<PlayItem>> = if (file == null) emptyList() else expanded.items.withIndex().filter { it.value.uri == file.uri }
    when {
        parts.isEmpty() -> 0 to position
        !parts[0].value.clipped -> parts[0].index to position
        else -> {
            val hit = parts.lastOrNull { it.value.startMs <= position } ?: parts[0]
            hit.index to maxOf(0L, position - hit.value.startMs)
        }
    }
}

fun knownLengths(expanded: PlayBook): Map<Int, Long> =
    expanded.items.withIndex().filter { it.value.lengthMs > 0 }.associate { it.index to it.value.lengthMs }
