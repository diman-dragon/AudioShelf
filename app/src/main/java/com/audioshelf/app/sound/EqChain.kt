package com.audioshelf.app.sound

private const val Q = 1.41
private const val NYQUIST_SHARE = 0.45

class EqChain {
    private val filters = arrayOfNulls<Biquad>(BAND_COUNT)
    private var z1 = Array(BAND_COUNT) { FloatArray(0) }
    private var z2 = Array(BAND_COUNT) { FloatArray(0) }

    fun reset(channels: Int) {
        z1 = Array(BAND_COUNT) { FloatArray(channels) }
        z2 = Array(BAND_COUNT) { FloatArray(channels) }
    }

    fun build(db: FloatArray, rate: Int): Float {
        var peak = 0f
        for (i in 0 until BAND_COUNT) {
            val on = db[i] != 0f && BAND_HZ[i] < rate * NYQUIST_SHARE
            filters[i] = if (on) Biquad.peaking(BAND_HZ[i], db[i].toDouble(), Q, rate.toDouble()) else null
            peak = maxOf(peak, db[i])
        }
        return peak
    }

    fun flat(): Boolean = filters.all { it == null }

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
