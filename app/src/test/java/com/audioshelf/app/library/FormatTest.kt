package com.audioshelf.app.library

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    private val units = SizeUnits("KB", "MB", "GB")

    @Test
    fun sizes() {
        assertEquals("1 KB", fmtSize(10, units, Locale.ENGLISH))
        assertEquals("2 KB", fmtSize(1536, units, Locale.ENGLISH))
        assertEquals("1.5 MB", fmtSize(1_572_864, units, Locale.ENGLISH))
        assertEquals("2 GB", fmtSize(2L * 1073741824, units, Locale.ENGLISH))
    }

    @Test
    fun namesAndExtensions() {
        assertEquals("a" to ".mp3", splitName("a.mp3"))
        assertEquals(".hidden" to "", splitName(".hidden"))
        assertEquals("noext" to "", splitName("noext"))
        assertEquals("ab", cleanFileName(" a/b:* "))
    }

    @Test
    fun audioDetection() {
        assertEquals(true, isAudio("x.MP3", null))
        assertEquals(true, isAudio("x.bin", "audio/mpeg"))
        assertEquals(false, isAudio("x.wma", "audio/x-ms-wma"))
        assertEquals(false, isAudio("x.txt", "text/plain"))
    }

    @Test
    fun libFileSurvivesEncoding() {
        val f = file("A/B", "ç \"q\".mp3", 5, 7)
        val (n, back) = decodeFile(encodeFile(f, 4))!!
        assertEquals(4, n)
        assertEquals(f, back)
    }
}
