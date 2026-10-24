package com.audioshelf.app.library

import com.audioshelf.app.data.KindFilter
import com.audioshelf.app.data.SortMode
import com.audioshelf.app.player.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTest {
    private fun row(name: String, kind: Kind, modified: Long, author: String = "", played: Long = 0): BookRow {
        val f = file("", "$name.mp3", modified = modified)
        val book = Book(f.uri, false, name, listOf(f), f.dirUri, listOf(f.dirUri), false, f.size, modified)
        return BookRow(book, Meta(), kind, name, author, "", "", null, ProgressState.New, played)
    }

    private val rows = listOf(
        row("Beta", Kind.Book, 20, author = "Tolstoy"),
        row("alpha 10", Kind.Album, 30, played = 5),
        row("alpha 2", Kind.Book, 10, played = 9),
    )

    private fun titles(kind: KindFilter, sort: SortMode, query: String = "") =
        filterRows(rows, kind, sort, query).map { it.title }

    @Test
    fun dateSortsNewestFirst() {
        assertEquals(listOf("alpha 10", "Beta", "alpha 2"), titles(KindFilter.All, SortMode.Date))
    }

    @Test
    fun nameSortIsNatural() {
        assertEquals(listOf("alpha 2", "alpha 10", "Beta"), titles(KindFilter.All, SortMode.Name))
    }

    @Test
    fun playedSortsRecentFirst() {
        assertEquals(listOf("alpha 2", "alpha 10", "Beta"), titles(KindFilter.All, SortMode.Played))
    }

    @Test
    fun kindFilterSeparatesBooksAndAlbums() {
        assertEquals(listOf("Beta", "alpha 2"), titles(KindFilter.Book, SortMode.Date))
        assertEquals(listOf("alpha 10"), titles(KindFilter.Album, SortMode.Date))
    }

    @Test
    fun searchMatchesTitleAndAuthorIgnoringCase() {
        assertEquals(listOf("Beta"), titles(KindFilter.All, SortMode.Date, "  TOLSTOY "))
        assertEquals(listOf("alpha 10", "alpha 2"), titles(KindFilter.All, SortMode.Name, "ALPHA").reversed())
    }

    @Test
    fun kindDefaultsFollowSizeAndExtension() {
        val m4b = Book("a", false, "a", listOf(file("", "a.m4b")), "", listOf(""), false, 1, 1)
        val small = Book("b", false, "b", listOf(file("", "b.mp3")), "", listOf(""), false, 1, 1)
        val huge = Book("c", false, "c", listOf(file("", "c.mp3", 40L * 1048576)), "", listOf(""), false, 40L * 1048576, 1)
        val long = Book("d", false, "d", listOf(file("", "d.mp3")), "", listOf(""), false, 1, 1)
        assertEquals(Kind.Book, kindOf(m4b, Meta(), 0))
        assertEquals(Kind.Album, kindOf(small, Meta(), 0))
        assertEquals(Kind.Book, kindOf(huge, Meta(), 0))
        assertEquals(Kind.Book, kindOf(long, Meta(), 31L * 60000))
        assertEquals(Kind.Album, kindOf(m4b, Meta(kind = Kind.Album), 0))
    }
}
