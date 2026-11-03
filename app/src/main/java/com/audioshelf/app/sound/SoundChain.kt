package com.audioshelf.app.sound

import com.audioshelf.app.AppLog
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

private const val KNEE = 24000f
private const val HEADROOM = 32767f - KNEE

class SoundChain {
    @Volatile private var bandsDb = FloatArray(BAND_COUNT)
    @Volatile private var boostDb = 0

    private val eq = EqChain()
    private var rate = 44100
    private var channels = 2
    private var builtFor: FloatArray? = null
    private var builtBoost = Int.MIN_VALUE
    private var gain = 1f
    private var boost = 1f

    /** How often [limit] bent the signal, and how often it would have gone past full scale; read and reset by the one audio thread. */
    var limiterHits = 0
        private set
    var overHits = 0
        private set

    fun resetHits() {
        limiterHits = 0
        overHits = 0
    }

    fun setBands(db: FloatArray) {
        bandsDb = db
    }

    fun setBoost(db: Int) {
        boostDb = db
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        rate = sampleRate
        channels = channelCount
        eq.reset(channelCount)
        builtFor = null
        builtBoost = Int.MIN_VALUE
    }

    fun refresh() {
        val db = bandsDb
        if (db !== builtFor) {
            val peak = eq.build(db, rate)
            gain = 10.0.pow((-peak / 20).toDouble()).toFloat()
            builtFor = db
            AppLog.i("SoundChain", "equalizer rebuilt: bands=${db.joinToString(",") { it.toInt().toString() }} dB, headroom gain=${"%.3f".format(gain)} at $rate Hz")
        }
        val wanted = boostDb
        if (wanted != builtBoost) {
            boost = 10.0.pow(wanted / 20.0).toFloat()
            builtBoost = wanted
            AppLog.i("SoundChain", "volume boost $wanted dB (x${"%.2f".format(boost)})")
        }
    }

    val passthrough: Boolean get() = gain == 1f && boost == 1f && eq.flat()

    fun channelCount(): Int = channels

    fun process(sample: Float, channel: Int): Float = limit(eq.process(sample, channel) * gain * boost)

    private fun limit(x: Float): Float {
        val a = abs(x)
        if (a <= KNEE) return x
        limiterHits++
        if (a > 32767f) overHits++
        val y = KNEE + HEADROOM * tanh((a - KNEE) / HEADROOM)
        return if (x < 0) -y else y
    }
}
