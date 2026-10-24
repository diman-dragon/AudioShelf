package com.audioshelf.app.sound

import kotlin.math.pow

class SoundChain {
    @Volatile private var bandsDb = FloatArray(BAND_COUNT)

    private val eq = EqChain()
    private var rate = 44100
    private var channels = 2
    private var builtFor: FloatArray? = null
    private var gain = 1f

    fun setBands(db: FloatArray) {
        bandsDb = db
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        rate = sampleRate
        channels = channelCount
        eq.reset(channelCount)
        builtFor = null
    }

    fun refresh() {
        val db = bandsDb
        if (db === builtFor) return
        val peak = eq.build(db, rate)
        gain = 10.0.pow((-peak / 20).toDouble()).toFloat()
        builtFor = db
    }

    val passthrough: Boolean get() = gain == 1f && eq.flat()

    fun channelCount(): Int = channels

    fun process(sample: Float, channel: Int): Float = eq.process(sample, channel) * gain
}
