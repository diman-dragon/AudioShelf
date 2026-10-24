package com.audioshelf.app.sound

import kotlin.math.pow

class SoundChain {
    @Volatile private var bandsDb = FloatArray(BAND_COUNT)
    @Volatile private var trimDb = 0f
    @Volatile private var boost = 1f

    private val eq = EqChain()
    private var rate = 44100
    private var channels = 2
    private var builtFor: FloatArray? = null
    private var builtTrim = Float.NaN
    private var builtBoost = Float.NaN
    private var limit = false
    private var gain = 1f

    fun setBands(db: FloatArray) {
        bandsDb = db
    }

    fun setTrimDb(db: Float) {
        trimDb = db
    }

    fun setBoost(factor: Float) {
        boost = factor.coerceIn(1f, 4f)
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        rate = sampleRate
        channels = channelCount
        eq.reset(channelCount)
        builtFor = null
    }

    fun refresh() {
        val db = bandsDb
        val trim = trimDb
        val factor = boost
        if (db === builtFor && trim == builtTrim && factor == builtBoost) return
        val peak = eq.build(db, rate)
        gain = 10.0.pow(((trim - peak) / 20).toDouble()).toFloat() * factor
        limit = factor > 1f
        builtFor = db
        builtTrim = trim
        builtBoost = factor
    }

    val passthrough: Boolean get() = gain == 1f && eq.flat()

    fun channelCount(): Int = channels

    fun process(sample: Float, channel: Int): Float {
        val x = eq.process(sample, channel) * gain
        return if (limit) SoftLimiter.soften(x) else x
    }
}
