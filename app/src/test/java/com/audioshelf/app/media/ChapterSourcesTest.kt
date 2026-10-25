package com.audioshelf.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterSourcesTest {
    private fun le32(v: Int) = ByteArray(4) { (v shr (8 * it)).toByte() }

    private fun comments(vararg lines: String): ByteArray {
        val vendor = text("test")
        val parts = arrayListOf(le32(vendor.size), vendor, le32(lines.size))
        lines.forEach {
            val b = text(it)
            parts.add(le32(b.size))
            parts.add(b)
        }
        return cat(*parts.toTypedArray())
    }

    private val lines = arrayOf(
        "TITLE=Book", "CHAPTER001=00:00:00.000", "CHAPTER001NAME=Intro",
        "CHAPTER002=00:01:30.500", "CHAPTER002NAME=Part two", "CHAPTER003=01:00:00.000",
    )

    private fun page(payload: ByteArray, seq: Int): ByteArray {
        val head = cat(text("OggS"), byteArrayOf(0, if (seq == 0) 2 else 0), ByteArray(8), ByteArray(4), le32(seq), ByteArray(4), byteArrayOf(1, payload.size.toByte()))
        return cat(head, payload)
    }

    private fun check(chapters: List<Chapter>) {
        assertEquals(3, chapters.size)
        assertEquals("Intro", chapters[0].title)
        assertEquals(90_500L, chapters[1].startMs)
        assertEquals(3_600_000L, chapters[2].startMs)
        assertEquals("3", chapters[2].title)
    }

    @Test
    fun flacVorbisComment() {
        val body = comments(*lines)
        val header = byteArrayOf(0x84.toByte(), (body.size shr 16).toByte(), (body.size shr 8).toByte(), body.size.toByte())
        val src = ArraySource(cat(text("fLaC"), header, body))
        assertTrue(VorbisChapters.matches(src))
        check(VorbisChapters.read(src))
    }

    @Test
    fun oggOpusTags() {
        val packet = cat(text("OpusTags"), comments(*lines))
        val src = ArraySource(cat(page(text("OpusHead-----------"), 0), page(packet, 1)))
        check(VorbisChapters.read(src))
    }

    @Test
    fun oggVorbisComment() {
        val packet = cat(byteArrayOf(3), text("vorbis"), comments(*lines), byteArrayOf(1))
        val src = ArraySource(cat(page(cat(byteArrayOf(1), text("vorbis")), 0), page(packet, 1)))
        check(VorbisChapters.read(src))
    }

    @Test
    fun timeParsing() {
        assertEquals(3_723_004L, VorbisChapters.parseTime("01:02:03.004"))
        assertEquals(62_000L, VorbisChapters.parseTime("1:02"))
        assertEquals(null, VorbisChapters.parseTime("abc"))
    }

    @Test
    fun cueSheetSingleFile() {
        val cue = """
            PERFORMER "A"
            FILE "book.wav" WAVE
              TRACK 01 AUDIO
                TITLE "Начало"
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                TITLE "Глава 2"
                INDEX 01 10:30:37
        """.trimIndent()
        val map = CueSheet.parse(cue)
        val list = map["book.wav"]!!
        assertEquals(2, list.size)
        assertEquals("Глава 2", list[1].title)
        assertEquals(630_000L + 37 * 1000 / 75, list[1].startMs)
    }

    @Test
    fun cueDecodesWindows1251() {
        val bytes = "TITLE \"Привет\"".toByteArray(charset("windows-1251"))
        assertTrue(CueSheet.decode(bytes).contains("Привет"))
    }

    @Test
    fun openBookToc() {
        val json = """{"nav":{"toc":[
            {"title":"One","path":"Part1.mp3?cid=1#0"},
            {"title":"Two","path":"Part1.mp3#120.5","contents":[{"title":"Two b","path":"Part2.mp3#0"}]}]}}"""
        val map = OpenBook.parse(json)
        assertEquals(2, map["part1.mp3"]!!.size)
        assertEquals(120_500L, map["part1.mp3"]!![1].startMs)
        assertTrue(map["part2.mp3"]!!.isEmpty())
    }

    @Test
    fun overdriveMarkers() {
        val xml = "<Markers><Marker><Name>Chapter 1</Name><Time>0:00.000</Time></Marker>" +
            "<Marker><Name>Chapter 2</Name><Time>12:30.250</Time></Marker></Markers>"
        val list = OverdriveMarkers.parse(xml)
        assertEquals(2, list.size)
        assertEquals(750_250L, list[1].startMs)
    }
}
