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
        val chain = SoundChain().also { it.setBands(bands) }
        val boosted = rms(chain, 1000.0)
        val far = rms(SoundChain().also { it.setBands(bands) }, 62.5)
        assertEquals(1.0, boosted / flat, 0.1)
        assertTrue(far / boosted < 0.6)
    }

    @Test
    fun loudSpeechWithBoostReachesTheLimiterAndIsCounted() {
        val chain = SoundChain().also { it.setBoost(12) }
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
}
