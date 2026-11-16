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

class AdaptiveBoostTest {
    private fun run(chain: SoundChain, amplitude: Double, seconds: Int, hz: Double = 200.0, rate: Int = 44100): Double {
        chain.configure(rate, 1)
        chain.refresh()
        var peakLast = 0.0
        val n = rate * seconds
        for (i in 0 until n) {
            val y = chain.process((sin(2 * PI * hz * i / rate) * amplitude).toFloat(), 0)
            if (i > n - rate / 2) peakLast = maxOf(peakLast, Math.abs(y.toDouble()))
        }
        return peakLast
    }

    @Test
    fun loudMaterialMakesTheBoostGiveWay() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 9) }
        run(chain, 20000.0, 20)
        assertTrue("the boost must be cut on loud material, was ${chain.boostCutDb}", chain.boostCutDb >= 5f)
        chain.resetHits()
        run(chain, 20000.0, 1)
        assertTrue("after the cut the limiter should be mostly idle, hits=${chain.limiterHits}", chain.limiterHits < 200)
    }

    @Test
    fun quietSpeechKeepsAllOfTheBoost() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 6) }
        run(chain, 3000.0, 10)
        assertEquals(0f, chain.boostCutDb, 0.001f)
    }

    @Test
    fun theCutGoesBackWhenTheSignalIsQuietAgain() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 9) }
        run(chain, 30000.0, 10)
        val cut = chain.boostCutDb
        assertTrue(cut > 0f)
        run(chain, 2000.0, 20)
        assertTrue("the cut should shrink, $cut -> ${chain.boostCutDb}", chain.boostCutDb < cut)
    }

    @Test
    fun withoutBoostNothingIsCut() {
        val chain = SoundChain()
        run(chain, 30000.0, 5)
        assertEquals(0f, chain.boostCutDb, 0.001f)
    }
}

class AdaptiveBoostEdgeTest {
    @Test
    fun stereoFramesCountAsTwoSamplesEach() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 9) }
        chain.configure(44100, 2)
        chain.refresh()
        // 0.25 s of stereo is 22050 calls; a cut must appear after about that many, not after twice as many.
        for (i in 0 until 22050 + 10) {
            val v = (sin(2 * PI * 200.0 * (i / 2) / 44100) * 20000).toFloat()
            chain.process(v, i % 2)
        }
        assertTrue("the first window of a stereo signal must already cut, was ${chain.boostCutDb}", chain.boostCutDb > 0f)
    }

    @Test
    fun loweringTheBoostLowersTheCutAtOnce() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 9) }
        chain.configure(44100, 1)
        chain.refresh()
        for (i in 0 until 44100 * 5) chain.process((sin(2 * PI * 200.0 * i / 44100) * 20000).toFloat(), 0)
        assertTrue(chain.boostCutDb > 3f)
        chain.setSettings(FloatArray(BAND_COUNT), 3)
        chain.refresh()
        assertTrue("the cut cannot exceed the new boost, was ${chain.boostCutDb}", chain.boostCutDb <= 3f)
    }

    @Test
    fun theCutSurvivesANewTrack() {
        val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 9) }
        chain.configure(44100, 1)
        chain.refresh()
        for (i in 0 until 44100 * 5) chain.process((sin(2 * PI * 200.0 * i / 44100) * 20000).toFloat(), 0)
        val cut = chain.boostCutDb
        chain.configure(44100, 1)
        chain.refresh()
        assertEquals(cut, chain.boostCutDb, 0.001f)
    }
}

class AdaptiveBoostLogTest {
    @Test
    fun speechPeaksDoNotFloodTheLog() {
        val lines = mutableListOf<String>()
        val old = com.audioshelf.app.AppLog.sink
        com.audioshelf.app.AppLog.sink = com.audioshelf.app.AppLog.Sink { _, _, m -> if (m.startsWith("boost follows")) lines += m }
        try {
            val chain = SoundChain().also { it.setSettings(FloatArray(BAND_COUNT), 6) }
            chain.configure(44100, 1)
            chain.refresh()
            // quiet speech with a short loud burst once a second: the cut moves by under a dB each time
            for (i in 0 until 44100 * 60) {
                val loud = (i % 44100) < 300
                chain.process((sin(2 * PI * 200.0 * i / 44100) * if (loud) 15000 else 3000).toFloat(), 0)
            }
            assertTrue("too many lines: ${lines.size}", lines.size < 20)
        } finally {
            com.audioshelf.app.AppLog.sink = old
        }
    }
}
