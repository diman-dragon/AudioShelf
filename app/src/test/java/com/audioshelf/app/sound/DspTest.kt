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
}
