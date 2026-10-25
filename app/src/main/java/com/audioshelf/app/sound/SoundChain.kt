package com.audioshelf.app.sound

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

private const val KNEE = 24000f
private const val HEADROOM = 32767f - KNEE

class SoundChain {
    @Volatile private var bandsDb = FloatArray(BAND_COUNT)
    @Volatile private var fx = Fx.Off

    private val eq = EqChain()
    private var rate = 44100
    private var channels = 2
    private var builtFor: FloatArray? = null
    private var builtFx: Fx? = null
    private var gain = 1f
    private var boost = 1f
    private var noise = 0
    private var denoisers: Array<Denoiser> = emptyArray()

    fun setBands(db: FloatArray) {
        bandsDb = db
    }

    fun setFx(next: Fx) {
        fx = next
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        rate = sampleRate
        channels = channelCount
        eq.reset(channelCount)
        denoisers = Array(channelCount) { Denoiser() }
        builtFor = null
        builtFx = null
    }

    fun refresh() {
        val db = bandsDb
        if (db !== builtFor) {
            val peak = eq.build(db, rate)
            gain = 10.0.pow((-peak / 20).toDouble()).toFloat()
            builtFor = db
        }
        val current = fx
        if (current !== builtFx) {
            boost = current.gain
            noise = current.noise
            denoisers.forEach { it.level = noise }
            builtFx = current
        }
    }

    val passthrough: Boolean get() = gain == 1f && boost == 1f && noise == 0 && eq.flat()

    fun channelCount(): Int = channels

    fun process(sample: Float, channel: Int): Float {
        val clean = if (noise != 0 && channel < denoisers.size) denoisers[channel].process(sample) else sample
        return limit(eq.process(clean, channel) * gain * boost)
    }

    private fun limit(x: Float): Float {
        val a = abs(x)
        if (a <= KNEE) return x
        val y = KNEE + HEADROOM * tanh((a - KNEE) / HEADROOM)
        return if (x < 0) -y else y
    }
}
