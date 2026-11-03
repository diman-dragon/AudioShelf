package com.audioshelf.app.sound

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundStatsTest {
    @Test
    fun drainReturnsTheTotalsAndStartsOver() {
        val stats = SoundStats()
        stats.record(1000, 2, 10, 1, 16384, 20000)
        stats.record(500, 1, 5, 0, 30000, 18000)
        val first = stats.drain()
        assertEquals(1500L, first.samples)
        assertEquals(3L, first.sourceClipped)
        assertEquals(15L, first.limited)
        assertEquals(30000, first.peakIn)
        assertEquals(20000, first.peakOut)
        val second = stats.drain()
        assertEquals(0L, second.samples)
        assertEquals(0, second.peakIn)
    }

    @Test
    fun descriptionNamesTheNumbersAUserCanReport() {
        val stats = SoundStats()
        stats.record(100, 4, 7, 2, 32768, 32000)
        val text = stats.drain().describe()
        assertTrue("source-clipped=4" in text)
        assertTrue("limiter-hits=7" in text)
        assertTrue("in-peak=100%" in text)
    }
}
