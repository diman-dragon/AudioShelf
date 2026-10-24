package com.audioshelf.app.sound

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoudnessStatsTest {
    private fun tone(amplitude: Double, seconds: Int = 3, rate: Int = 8000): ByteBuffer {
        val buf = ByteBuffer.allocate(rate * seconds * 2).order(ByteOrder.nativeOrder())
        for (i in 0 until rate * seconds) buf.putShort((sin(2 * PI * 440 * i / rate) * amplitude * 32767).toInt().toShort())
        buf.flip()
        return buf
    }

    @Test
    fun silenceNeedsNoGain() {
        val stats = LoudnessStats(8000, 1)
        stats.add(ByteBuffer.allocate(16000).order(ByteOrder.nativeOrder()), false)
        assertEquals(0.0, stats.gainDb(), 0.0)
    }

    @Test
    fun quietTrackIsBoostedLoudOneIsCut() {
        val quiet = LoudnessStats(8000, 1).also { it.add(tone(0.02), false) }
        val loud = LoudnessStats(8000, 1).also { it.add(tone(0.9), false) }
        assertTrue(quiet.gainDb() > 0)
        assertTrue(loud.gainDb() < 0)
    }

    @Test
    fun gainStaysInRangeAndKeepsHeadroom() {
        val quiet = LoudnessStats(8000, 1).also { it.add(tone(0.001), false) }
        assertTrue(quiet.gainDb() <= MAX_GAIN_DB)
        val peaky = LoudnessStats(8000, 1).also { it.add(tone(0.5), false) }
        assertTrue(peaky.gainDb() + 20 * Math.log10(0.5) <= -1.0 + 0.2)
    }
}
