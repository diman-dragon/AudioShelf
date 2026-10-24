package com.audioshelf.app.library

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class SiblingArtTest {
    private val images = listOf(
        ArtFile("00.jpg", "u0"),
        ArtFile("01. АЙЗЕК АЗИМОВ - Последний вопрос.jpg", "u1"),
        ArtFile("02. ГРЕГ БИР - Музыка, звучащая в крови.JPG", "u2"),
    )
    private val tracks = listOf("01. АЙЗЕК АЗИМОВ - Последний вопрос.mp3", "02. ГРЕГ БИР - Музыка, звучащая в крови.mp3")

    @Test
    fun trackImageMatchesByBaseName() {
        assertEquals("u1", SiblingArt.forTrack(tracks[0], images)?.uri)
        assertEquals("u2", SiblingArt.forTrack(tracks[1], images)?.uri)
        assertNull(SiblingArt.forTrack("03. ФИЛИП ДИК - Колония.mp3", images))
    }

    @Test
    fun generalCoverIsZeroOrNamed() {
        assertEquals("u0", SiblingArt.general(images, tracks)?.uri)
        assertEquals("c", SiblingArt.general(listOf(ArtFile("Cover.png", "c")), tracks)?.uri)
    }

    @Test
    fun singleUnmatchedImageIsGeneral() {
        val list = listOf(ArtFile("book.jpg", "b"), ArtFile("a.jpg", "a"))
        assertEquals("b", SiblingArt.general(list.take(1), listOf("a.mp3"))?.uri)
        assertNull(SiblingArt.general(list, listOf("x.mp3")))
    }

    @Test
    fun imagesAreRecognizedByExtension() {
        org.junit.Assert.assertTrue(SiblingArt.isImage("a.JPEG"))
        org.junit.Assert.assertFalse(SiblingArt.isImage("a.mp3"))
    }
}
