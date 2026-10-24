package com.audioshelf.app.library

import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.SortMode
import com.audioshelf.app.player.Kind

class BookRow(
    val book: Book,
    val meta: Meta,
    val kind: Kind,
    val title: String,
    val author: String,
    val narrator: String,
    val year: String,
    val percent: Int?,
    val state: ProgressState,
    val lastPlayed: Long,
)

private fun haystack(row: BookRow) =
    listOf(row.title, row.book.name, row.author, row.narrator, row.year).joinToString(" ").lowercase()

private val BY_NAME = Comparator<BookRow> { a, b -> NaturalOrder.compare(a.title, b.title) }
private val BY_DATE = Comparator<BookRow> { a, b -> b.book.modified.compareTo(a.book.modified) }
private val BY_PLAYED = Comparator<BookRow> { a, b ->
    val played = b.lastPlayed.compareTo(a.lastPlayed)
    if (played != 0) played else b.book.modified.compareTo(a.book.modified)
}

fun filterRows(rows: List<BookRow>, kind: KindFilter, sort: SortMode, query: String): List<BookRow> {
    val q = query.trim().lowercase()
    val comparator = when (sort) {
        SortMode.Date -> BY_DATE
        SortMode.Name -> BY_NAME
        SortMode.Played -> BY_PLAYED
    }
    return rows
        .filter { row ->
            val kindOk = kind == KindFilter.All || (kind == KindFilter.Book) == (row.kind == Kind.Book)
            kindOk && (q.isEmpty() || haystack(row).contains(q))
        }
        .sortedWith(comparator)
}

fun buildRow(book: Book, meta: Meta, progress: ProgressStore, durations: DurationStore): BookRow = BookRow(
    book = book,
    meta = meta,
    kind = kindOf(book, meta, durations.get(book.id)[0] ?: 0L),
    title = meta.title.ifEmpty { book.name },
    author = meta.author,
    narrator = meta.narrator,
    year = meta.year,
    percent = progress.percent(book.id),
    state = progress.state(book.id),
    lastPlayed = progress.lastPlayed(book.id),
)
