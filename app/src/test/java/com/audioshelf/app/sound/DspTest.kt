package com.audioshelf.app.sound

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DspTest {
    private fun rms(chain: SoundChain, hz: Double, rate: Int = 44100): Double {
        chain.configure(rate, 1)
        chain.refresh()
        var sum = 0.0
        val n = rate
        for (i in 0 until n) {
            val y = chain.process((sin(2 * PI * hz * i / rate) * 8000).toFloat(), 0)
            if (i > n / 2) sum += y * y
        }
        return Math.sqrt(sum / (n / 2))
    }

    @Test
    fun flatChainPassesThrough() {
        val chain = SoundChain()
        chain.configure(44100, 2)
        chain.refresh()
        assertTrue(chain.passthrough)
    }

    @Test
    fun boostedBandStaysAndOthersDropByHeadroom() {
        val bands = FloatArray(BAND_COUNT).also { it[5] = 6f }
        val flat = rms(SoundChain(), 1000.0)
        val chain = SoundChain().also { it.setSettings(bands, 0) }
        val boosted = rms(chain, 1000.0)
        val far = rms(SoundChain().also { it.setSettings(bands, 0) }, 62.5)
        assertEquals(1.0, boosted / flat, 0.1)
        assertTrue(far / boosted < 0.6)
    }

    @Test
    fun loudSpeechWithBoostReachesTheLimiterAndIsCounted() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 12) }
        chain.configure(44100, 1)
        chain.refresh()
        chain.resetHits()
        for (i in 0 until 4410) chain.process((sin(2 * PI * 200.0 * i / 44100) * 20000).toFloat(), 0)
        assertTrue("a +12 dB boost on a loud signal must hit the limiter", chain.limiterHits > 0)
        assertTrue(chain.overHits > 0)
    }

    @Test
    fun belowTheKneeNothingIsCounted() {
        val chain = SoundChain()
        chain.configure(44100, 1)
        chain.refresh()
        chain.resetHits()
        for (i in 0 until 4410) chain.process((sin(2 * PI * 200.0 * i / 44100) * 20000).toFloat(), 0)
        assertEquals(0, chain.limiterHits)
    }

    @Test
    fun equalizerHeadroomUsesTheCombinedFilterResponse() {
        val bands = floatArrayOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 5f)
        val peakDb = EqChain().build(bands, 44100)
        assertTrue("combined filter response should exceed the strongest single band", peakDb > 5f)

        val chain = SoundChain().also { it.setSettings(bands, 0) }
        chain.configure(44100, 1)
        chain.refresh()
        chain.resetHits()
        repeat(44100) { i ->
            chain.process((sin(2 * PI * 1000.0 * i / 44100) * 32760).toFloat(), 0)
        }
        assertEquals("EQ headroom must prevent full-scale overdrive", 0, chain.overHits)
    }
}
