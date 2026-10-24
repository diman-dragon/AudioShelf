package com.audioshelf.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp4Id3Test {
    private fun chplEntry(startMs: Long, title: String): ByteArray {
        val t = text(title)
        return cat(uint(startMs * 10_000), byteArrayOf(t.size.toByte()), t)
    }

    @Test
    fun readsNeroChapters() {
        val chpl = box("chpl", cat(ByteArray(8), byteArrayOf(2), chplEntry(0, "One"), chplEntry(90_000, "Two")))
        val file = cat(box("ftyp", text("M4B ")), box("moov", box("udta", chpl)))
        val src = ArraySource(file)
        assertTrue(Mp4Chapters.matches(src))
        val list = FileTags.read(src, false).chapters
        assertEquals(listOf("One", "Two"), list.map { it.title })
        assertEquals(listOf(0L, 90_000L), list.map { it.startMs })
        assertEquals(90_000L, list[0].endMs)
    }

    private fun syncsafe(v: Int) = byteArrayOf((v shr 21 and 0x7f).toByte(), (v shr 14 and 0x7f).toByte(), (v shr 7 and 0x7f).toByte(), (v and 0x7f).toByte())

    private fun frame(id: String, body: ByteArray) = cat(text(id), syncsafe(body.size), byteArrayOf(0, 0), body)

    private fun chap(start: Long, title: String): ByteArray {
        val tit = frame("TIT2", cat(byteArrayOf(3), text(title)))
        return frame("CHAP", cat(text("c"), byteArrayOf(0), be32(start), be32(start + 1), be32(0), be32(0), tit))
    }

    @Test
    fun readsId3Chapters() {
        val frames = cat(chap(0, "Start"), chap(45_000, "Middle"))
        val file = cat(text("ID3"), byteArrayOf(4, 0, 0), syncsafe(frames.size), frames)
        val src = ArraySource(file)
        assertTrue(Id3Chapters.matches(src))
        val list = FileTags.read(src, false).chapters
        assertEquals(listOf("Start", "Middle"), list.map { it.title })
        assertEquals(45_000L, list[1].startMs)
    }

    @Test
    fun tidyDropsDuplicatesAndSorts() {
        val raw = listOf(Chapter("b", 500), Chapter("a", 100), Chapter("dup", 100), Chapter("", 900))
        val list = Chapter.tidy(raw)
        assertEquals(listOf("a", "b", "3"), list.map { it.title })
        assertEquals(0L, list[0].startMs)
    }
}
