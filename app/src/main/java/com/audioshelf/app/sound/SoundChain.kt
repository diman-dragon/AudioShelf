package com.audioshelf.app.sound

import com.audioshelf.app.AppLog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

private const val KNEE = 24000f
private const val HEADROOM = 32767f - KNEE

// The boost is a loudness gain, so it cannot be cancelled by the preamp (the book would simply not get louder).
// Instead it gives way when the signal keeps reaching the limiter: loud, already compressed music (or a clipped
// file) loses a few dB of boost, quiet speech keeps all of it. Every quarter of a second the share of samples above
// the knee is looked at; the cut grows quickly and goes back slowly, and the gain itself is smoothed, so it is not heard.
private const val WINDOW_SECONDS = 0.25
private const val HIT_RATIO = 0.002f
private const val CUT_STEP_DB = 0.5f
private const val RESTORE_STEP_DB = 0.05f
private const val GAIN_SMOOTHING = 0.0004f

class SoundChain {
    private data class Settings(val bandsDb: FloatArray, val boostDb: Int)

    @Volatile private var settings = Settings(FloatArray(BAND_COUNT), 0)

    private val eq = EqChain()
    private var rate = 44100
    private var channels = 2
    private var builtSettings: Settings? = null
    private var gain = 1f
    private var boost = 1f
    private var boostDb = 0f
    private var boostNow = 1f
    private var boostTarget = 1f
    private var cutDb = 0f
    private var shownCutDb = 0
    private var windowLength = 1
    private var windowSamples = 0
    private var windowHits = 0

    /** How many dB of the boost the limiter has taken back for now; read from other threads for the log. */
    @Volatile var boostCutDb = 0f
        private set

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
        windowLength = max(1, (sampleRate * channelCount * WINDOW_SECONDS).toInt())
        windowSamples = 0
        windowHits = 0
    }

    fun refresh() {
        val wanted = settings
        if (wanted !== builtSettings) {
            val peakDb = eq.build(wanted.bandsDb, rate)
            val headroomDb = if (peakDb > 0f) peakDb + HEADROOM_DB else 0f
            gain = 10.0.pow(-(headroomDb / 20.0)).toFloat()
            boost = 10.0.pow(wanted.boostDb / 20.0).toFloat()
            boostDb = wanted.boostDb.toFloat()
            cutDb = min(cutDb, boostDb)
            boostTarget = targetBoost()
            if (builtSettings == null) boostNow = boostTarget
            builtSettings = wanted
            AppLog.i(
                "SoundChain",
                "DSP rebuilt: bands=${wanted.bandsDb.joinToString(",") { it.toInt().toString() }} dB, " +
                    "EQ peak=${"%.2f".format(peakDb)} dB, headroom=${"%.2f".format(headroomDb)} dB, " +
                    "boost=${wanted.boostDb} dB (x${"%.2f".format(boost)}, now -${"%.1f".format(cutDb)} dB) at $rate Hz",
            )
        }
    }

    val passthrough: Boolean get() = gain == 1f && boost == 1f && eq.flat()

    fun channelCount(): Int = channels

    fun process(sample: Float, channel: Int): Float {
        boostNow += (boostTarget - boostNow) * GAIN_SMOOTHING
        val y = limit(eq.process(sample, channel) * gain * boostNow)
        if (++windowSamples >= windowLength) adaptBoost()
        return y
    }

    private fun targetBoost(): Float = 10.0.pow(((boostDb - cutDb) / 20f).toDouble()).toFloat()

    private fun adaptBoost() {
        val ratio = windowHits.toFloat() / windowSamples
        val quiet = windowHits == 0
        windowSamples = 0
        windowHits = 0
        if (boostDb <= 0f) return
        cutDb = when {
            ratio > HIT_RATIO -> min(boostDb, cutDb + CUT_STEP_DB)
            quiet -> max(0f, cutDb - RESTORE_STEP_DB)
            else -> cutDb
        }
        boostTarget = targetBoost()
        boostCutDb = cutDb
        val shown = cutDb.roundToInt()
        if (shown != shownCutDb) {
            shownCutDb = shown
            AppLog.i("SoundChain", "boost follows the limiter: -$shown dB of ${boostDb.toInt()} dB (hit ratio ${"%.2f".format(ratio * 100)}%)")
        }
    }

    private fun limit(x: Float): Float {
        val a = abs(x)
        if (a <= KNEE) return x
        limiterHits++
        windowHits++
        if (a > 32767f) overHits++
        val y = KNEE + HEADROOM * tanh((a - KNEE) / HEADROOM)
        return if (x < 0) -y else y
    }

    private companion object {
        const val HEADROOM_DB = 0.5f
    }
}
