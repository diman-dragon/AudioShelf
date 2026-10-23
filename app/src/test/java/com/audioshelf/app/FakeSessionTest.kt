package com.audioshelf.app

import com.audioshelf.app.player.FakeSession
import com.audioshelf.app.player.PlayMode
import com.audioshelf.app.player.Placeholder
import com.audioshelf.app.player.SAMPLE_ALBUM
import com.audioshelf.app.player.SAMPLE_BOOK
import com.audioshelf.app.player.SleepChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSessionTest {
    @Test
    fun bookProgressMatchesReferenceScreen() {
        val ui = FakeSession(SAMPLE_BOOK, positionMs = 1500, playing = true).toUi()
        assertEquals(0, ui.progress!!.percent)
        assertEquals(2_398_500L, ui.progress!!.leftMs)
        assertEquals(Placeholder.OpenBook, ui.placeholder)
    }

    @Test
    fun freshBookShowsClosedCover() {
        assertEquals(Placeholder.ClosedBook, FakeSession(SAMPLE_BOOK).toUi().placeholder)
    }

    @Test
    fun albumHasDiscAndNoBookProgress() {
        val ui = FakeSession(SAMPLE_ALBUM).toUi()
        assertEquals(Placeholder.Disc, ui.placeholder)
        assertNull(ui.progress)
    }

    @Test
    fun skipsAreClampedToChapter() {
        val s = FakeSession(SAMPLE_BOOK, positionMs = 5_000)
        s.skip(false)
        assertEquals(0L, s.positionMs)
        s.seek(595_000)
        s.skip(true)
        assertEquals(600_000L, s.positionMs)
    }

    @Test
    fun continuousModeAdvancesChapter() {
        val s = FakeSession(SAMPLE_BOOK, positionMs = 599_900, playing = true)
        s.tick(250)
        assertEquals(1, s.index)
        assertTrue(s.playing)
    }

    @Test
    fun pauseAfterChapterModeStops() {
        val s = FakeSession(SAMPLE_BOOK, positionMs = 599_900, playing = true, mode = PlayMode.Chapter)
        s.tick(250)
        assertEquals(1, s.index)
        assertFalse(s.playing)
    }

    @Test
    fun lastChapterEndsPlayback() {
        val s = FakeSession(SAMPLE_BOOK, index = 3, positionMs = 599_900, playing = true)
        s.tick(250)
        assertFalse(s.playing)
        assertEquals(3, s.index)
    }

    @Test
    fun repeatAllWrapsAround() {
        val s = FakeSession(SAMPLE_ALBUM, index = 2, positionMs = 440_900, playing = true, mode = PlayMode.All)
        s.tick(250)
        assertEquals(0, s.index)
        assertTrue(s.playing)
    }

    @Test
    fun sleepTimerCountsOnlyWhilePlaying() {
        val s = FakeSession(SAMPLE_BOOK)
        s.cycleSleep()
        assertEquals(SleepChoice.Min15, s.sleep)
        s.tick(10_000)
        assertEquals(15 * 60_000L, s.sleepLeftMs)
        s.playing = true
        s.tick(10_000)
        assertEquals(15 * 60_000L - 10_000, s.sleepLeftMs)
    }

    @Test
    fun speedCyclesThroughAllValues() {
        val s = FakeSession(SAMPLE_BOOK)
        val seen = List(6) { s.cycleSpeed(); s.speed }
        assertEquals(listOf(1.25f, 1.5f, 1.75f, 2f, 0.75f, 1f), seen)
    }
}
