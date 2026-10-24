package com.audioshelf.app.player

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import com.audioshelf.app.data.testKv
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayLogicTest {
    private fun file(name: String, uri: String = "u/$name") = LibFile(uri, name, "", "d", 1000, 5)

    private fun book(vararg files: LibFile) = Book("id", files.size > 1, "Book", files.toList(), "d", listOf("d"), false, 0, 0)

    @Test
    fun jsonHandlesNesting() {
        val text = Json.write(linkedMapOf("a" to listOf(1, 2, linkedMapOf("b" to "x\"y")), "c" to null))
        val back = Json.parse(text).asMap()!!
        assertEquals(3, back["a"].asList()!!.size)
        assertEquals("x\"y", back["a"].asList()!![2].asMap()!!["b"])
        assertNull(Json.parse("[1,"))
    }

    @Test
    fun expandSplitsFilesWithChapters() {
        val a = file("a.m4b")
        val b = file("b.mp3")
        val chapters = mapOf(a.uri to listOf(ChapterRec("I", 0, 1000), ChapterRec("II", 1000, 5000), ChapterRec("III", 5000, -1)))
        val expanded = expandBook(book(a, b)) { chapters[it.uri] }
        assertTrue(expanded.expanded)
        assertEquals(listOf("I", "II", "III", "b"), expanded.items.map { it.title })
        assertEquals(4000L, expanded.items[1].lengthMs)
        assertEquals(-1L, expanded.items[2].lengthMs)
        assertEquals(mapOf(0 to 1000L, 1 to 4000L), knownLengths(expanded))
        val plain = expandBook(book(a, b)) { null }
        assertFalse(plain.expanded)
        assertEquals(2, plain.items.size)
    }

    @Test
    fun remapMovesPositionIntoChapter() {
        val a = file("a.m4b")
        val b = file("b.mp3")
        val base = book(a, b)
        val expanded = expandBook(base) { if (it.uri == a.uri) listOf(ChapterRec("I", 0, 1000), ChapterRec("II", 1000, -1)) else null }
        val map = remapper(base, expanded)
        assertEquals(1 to 500L, map(0, 1500))
        assertEquals(0 to 200L, map(0, 200))
        assertEquals(2 to 70L, map(1, 70))
    }

    @Test
    fun bookProgressUsesAverageForUnknown() {
        val p = bookProgress(mapOf(0 to 1000L, 1 to 1000L), 4, 1, 500)!!
        assertEquals(37, p.percent)
        assertFalse(p.exact)
        assertEquals(2500L, p.leftMs)
        assertTrue(bookProgress(mapOf(0 to 10L, 1 to 10L), 2, 0, 0)!!.exact)
        assertNull(bookProgress(emptyMap(), 3, 0, 0))
    }

    @Test
    fun marksKeepOrderAndIgnoreNeighbours() {
        val marks = MarkStore(testKv())
        assertTrue(marks.add("b", 1, 5000, false, now = 1))
        assertTrue(marks.add("b", 0, 9000, false, now = 2))
        assertFalse(marks.add("b", 0, 9500, false, now = 3))
        assertEquals(listOf(0, 1), marks.list("b").map { it.index })
        marks.remap("b") { i, p -> (i + 10) to p }
        assertEquals(listOf(10, 11), marks.list("b").map { it.index })
        assertTrue(marks.list("b").all { it.expanded })
        marks.remove("b", 1)
        assertEquals(1, marks.count("b"))
    }

    @Test
    fun lastBookSurvivesJson() {
        val base = book(file("a.mp3"), file("b.mp3"))
        val back = BookJson.decode(BookJson.encode(base))
        assertNotNull(back)
        assertEquals(base, back)
        assertNull(BookJson.decode("{}"))
    }

    @Test
    fun chapterStoreDropsStaleSignature() {
        val store = ChapterStore(testKv())
        val f = file("a.m4b")
        store.set(f, listOf(ChapterRec("x", 0, 10)))
        assertEquals(1, store.of(f)!!.size)
        assertNull(store.of(f.copy(size = 2000)))
    }

    @Test
    fun profileMapsModes() {
        assertTrue(profileFor(Kind.Book, PlayMode.Chapter).pauseAfter)
        val shuffle = profileFor(Kind.Album, PlayMode.Shuffle)
        assertTrue(shuffle.shuffle)
        assertTrue(shuffle.fadeOutMs > 0)
        assertEquals(0, profileFor(Kind.Book, PlayMode.Continue).fadeInMs)
    }
}
