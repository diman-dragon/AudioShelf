package com.audioshelf.app.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MkaFileTest {
    private fun atom(startMs: Long, title: String) = ebml(
        0xB6,
        cat(ebml(0x91, uint(startMs * 1_000_000)), ebml(0x80, ebml(0x85, text(title)))),
    )

    private fun attachment(name: String, mime: String, data: ByteArray) =
        ebml(0x61A7, cat(ebml(0x466E, text(name)), ebml(0x4660, text(mime)), ebml(0x465C, data)))

    private fun file(vararg segment: ByteArray) = cat(ebml(0x1A45DFA3, ByteArray(0)), ebml(0x18538067, cat(*segment)))

    @Test
    fun readsChaptersAndNamedCover() {
        val jpg = byteArrayOf(1, 2, 3, 4)
        val other = byteArrayOf(9, 9)
        val data = file(
            ebml(0x1043A770, ebml(0x45B9, cat(atom(0, "Intro"), atom(60_000, "Part two"), atom(120_000, "")))),
            ebml(
                0x1941A469,
                cat(attachment("back.png", "image/png", other), attachment("cover.jpg", "image/jpeg", jpg), attachment("font.ttf", "font/ttf", other)),
            ),
        )
        val src = ArraySource(data)
        assertTrue(MkaFile.matches(src))
        val info = FileTags.read(src, true)
        assertEquals(listOf("Intro", "Part two", "3"), info.chapters.map { it.title })
        assertEquals(listOf(0L, 60_000L, 120_000L), info.chapters.map { it.startMs })
        assertEquals(listOf(60_000L, 120_000L, -1L), info.chapters.map { it.endMs })
        assertArrayEquals(jpg, info.cover)
    }

    @Test
    fun coverIsSkippedUnlessWanted() {
        val data = file(ebml(0x1941A469, attachment("cover.jpg", "image/jpeg", byteArrayOf(1))))
        assertNull(FileTags.read(ArraySource(data), false).cover)
    }

    @Test
    fun oneChapterIsNotAList() {
        val data = file(ebml(0x1043A770, ebml(0x45B9, atom(0, "Only"))))
        assertTrue(FileTags.read(ArraySource(data), false).chapters.isEmpty())
    }

    @Test
    fun garbageIsIgnored() {
        val src = ArraySource(byteArrayOf(0, 1, 2, 3, 4, 5))
        assertFalse(MkaFile.matches(src))
        val info = FileTags.read(src, true)
        assertTrue(info.chapters.isEmpty())
        assertNull(info.cover)
    }
}
