package com.audioshelf.app.sound

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

private const val Q = 1.41
private const val NYQUIST_SHARE = 0.45
private const val RESPONSE_STEPS = 4096

class EqChain {
    private val filters = arrayOfNulls<Biquad>(BAND_COUNT)
    private var z1 = Array(BAND_COUNT) { FloatArray(0) }
    private var z2 = Array(BAND_COUNT) { FloatArray(0) }

    fun reset(channels: Int) {
        z1 = Array(BAND_COUNT) { FloatArray(channels) }
        z2 = Array(BAND_COUNT) { FloatArray(channels) }
    }

    fun build(db: FloatArray, rate: Int): Float {
        for (i in 0 until BAND_COUNT) {
            val on = db[i] != 0f && BAND_HZ[i] < rate * NYQUIST_SHARE
            filters[i] = if (on) Biquad.peaking(BAND_HZ[i], db[i].toDouble(), Q, rate.toDouble()) else null
        }
        return peakGainDb(rate)
    }

    fun flat(): Boolean = filters.all { it == null }

    private fun peakGainDb(rate: Int): Float {
        var peak = 1.0
        for (step in 0..RESPONSE_STEPS) {
            val omega = Math.PI * step / RESPONSE_STEPS
            val cos1 = cos(omega)
            val sin1 = sin(omega)
            val cos2 = cos(omega * 2)
            val sin2 = sin(omega * 2)
            var response = 1.0
            for (filter in filters) {
                val f = filter ?: continue
                val numerator = hypot(
                    f.b0 + f.b1 * cos1 + f.b2 * cos2,
                    -f.b1 * sin1 - f.b2 * sin2,
                )
                val denominator = hypot(
                    1.0 + f.a1 * cos1 + f.a2 * cos2,
                    -f.a1 * sin1 - f.a2 * sin2,
                )
                response *= numerator / denominator
            }
            if (response > peak) peak = response
        }
        return (20.0 * ln(peak) / ln(10.0)).toFloat()
    }

    fun process(sample: Float, channel: Int): Float {
        var x = sample
        for (b in 0 until BAND_COUNT) {
            val f = filters[b] ?: continue
            val y = f.b0 * x + z1[b][channel]
            z1[b][channel] = f.b1 * x - f.a1 * y + z2[b][channel]
            z2[b][channel] = f.b2 * x - f.a2 * y
            x = y
        }
        return x
    }
}
