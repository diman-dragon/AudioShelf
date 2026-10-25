package com.audioshelf.app.sound

import com.audioshelf.app.data.testKv
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FxTest {
    @Test
    fun codecRoundTripsAndClamps() {
        val fx = Fx(6, -3, 2, true)
        assertEquals(fx, Fx.decode(Fx.encode(fx)))
        val wild = Fx.decode("{\"gain\":99,\"pitch\":-99,\"noise\":9,\"skip\":false}")!!
        assertEquals(12, wild.gainDb)
        assertEquals(PITCH_MIN, wild.pitch)
        assertEquals(NOISE_MAX, wild.noise)
        assertNull(Fx.decode("nope"))
    }

    @Test
    fun neutralMeansNothingToDo() {
        assertTrue(Fx.Off.neutral)
        assertFalse(Fx.Off.withGain(3).neutral)
        assertFalse(Fx.Off.withSkipSilence(true).neutral)
        assertEquals(1f, Fx.Off.pitchFactor, 1e-6f)
        assertEquals(2f, Fx.Off.withPitch(6).pitchFactor * Fx.Off.withPitch(6).pitchFactor, 0.01f)
    }

    @Test
    fun trackOverridesGlobal() {
        val store = FxStore(testKv())
        assertEquals(Fx.Off, store.effective("a"))
        store.setGlobal(Fx.Off.withGain(6))
        assertEquals(6, store.effective("a").gainDb)
        store.setTrack("a", Fx.Off.withNoise(2))
        assertEquals(2, store.effective("a").noise)
        assertEquals(6, store.effective("b").gainDb)
        store.clearTrack("a")
        assertEquals(6, store.effective("a").gainDb)
    }

    private fun rms(a: FloatArray, from: Int, to: Int): Double {
        var sum = 0.0
        for (i in from until to) sum += a[i] * a[i]
        return sqrt(sum / (to - from))
    }

    private fun run(input: FloatArray, level: Int): FloatArray {
        val d = Denoiser()
        d.level = level
        return FloatArray(input.size) { d.process(input[it]) }
    }

    @Test
    fun denoiserCutsSteadyNoise() {
        val rate = 44100
        val rnd = Random(1)
        val x = FloatArray(3 * rate) { (rnd.nextGaussian() * 500).toFloat() }
        val y = run(x, 2)
        val ratio = rms(y, 2 * rate, 3 * rate) / rms(x, 2 * rate, 3 * rate)
        assertTrue("ratio $ratio", ratio < 0.6)
    }

    @Test
    fun denoiserKeepsSpeechLikeBursts() {
        val rate = 44100
        val rnd = Random(2)
        val x = FloatArray(4 * rate) { (rnd.nextGaussian() * 300).toFloat() }
        for (range in listOf(1.5 to 2.0, 2.5 to 3.0)) {
            for (i in (range.first * rate).toInt() until (range.second * rate).toInt()) {
                x[i] += (6000 * sin(2 * PI * 1000 * i / rate)).toFloat()
            }
        }
        val y = run(x, 2)
        val ratio = rms(y, (2.6 * rate).toInt(), (2.9 * rate).toInt()) / (6000 / sqrt(2.0))
        assertTrue("ratio $ratio", ratio > 0.85)
    }

    @Test
    fun levelZeroIsTransparentAfterLatency() {
        val x = FloatArray(4096) { (it % 200 - 100).toFloat() }
        val y = run(x, 0)
        val lag = Denoiser.LATENCY
        for (i in lag + 600 until 4000) assertEquals(x[i - lag], y[i], 1.0f)
    }

    @Test
    fun chainBoostsAndLimits() {
        val chain = SoundChain()
        chain.configure(44100, 1)
        chain.setFx(Fx.Off.withGain(12))
        chain.refresh()
        assertFalse(chain.passthrough)
        val y = chain.process(1000f, 0)
        assertEquals(3981f, y, 40f)
        assertTrue(chain.process(30000f, 0) <= 32767f)
    }
}
