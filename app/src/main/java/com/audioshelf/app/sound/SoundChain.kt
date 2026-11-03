package com.audioshelf.app.sound

import com.audioshelf.app.AppLog
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

private const val KNEE = 24000f
private const val HEADROOM = 32767f - KNEE

class SoundChain {
    private data class Settings(val bandsDb: FloatArray, val boostDb: Int)

    @Volatile private var settings = Settings(FloatArray(BAND_COUNT), 0)

    private val eq = EqChain()
    private var rate = 44100
    private var channels = 2
    private var builtSettings: Settings? = null
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

    fun setSettings(bandsDb: FloatArray, boostDb: Int) {
        settings = Settings(bandsDb.copyOf(BAND_COUNT), boostDb)
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        rate = sampleRate
        channels = channelCount
        eq.reset(channelCount)
        builtSettings = null
    }

    fun refresh() {
        val wanted = settings
        if (wanted !== builtSettings) {
            val peakDb = eq.build(wanted.bandsDb, rate)
            val headroomDb = if (peakDb > 0f) peakDb + HEADROOM_DB else 0f
            gain = 10.0.pow(-(headroomDb / 20.0)).toFloat()
            boost = 10.0.pow(wanted.boostDb / 20.0).toFloat()
            builtSettings = wanted
            AppLog.i(
                "SoundChain",
                "DSP rebuilt: bands=${wanted.bandsDb.joinToString(",") { it.toInt().toString() }} dB, " +
                    "EQ peak=${"%.2f".format(peakDb)} dB, headroom=${"%.2f".format(headroomDb)} dB, " +
                    "boost=${wanted.boostDb} dB (x${"%.2f".format(boost)}) at $rate Hz",
            )
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

    private companion object {
        const val HEADROOM_DB = 0.5f
    }
}
