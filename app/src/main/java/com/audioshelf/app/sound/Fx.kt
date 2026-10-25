package com.audioshelf.app.sound

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asMap
import com.audioshelf.app.data.asNumber
import kotlin.math.pow

val GAIN_STEPS = listOf(0, 3, 6, 9, 12)
const val PITCH_MIN = -6
const val PITCH_MAX = 6
const val NOISE_OFF = 0
const val NOISE_MAX = 3

class Fx(
    val gainDb: Int = 0,
    val pitch: Int = 0,
    val noise: Int = NOISE_OFF,
    val skipSilence: Boolean = false,
) {
    val gain: Float get() = 10.0.pow(gainDb / 20.0).toFloat()

    val pitchFactor: Float get() = 2.0.pow(pitch / 12.0).toFloat()

    val neutral: Boolean get() = gainDb == 0 && pitch == 0 && noise == NOISE_OFF && !skipSilence

    val needsProcessor: Boolean get() = gainDb != 0 || noise != NOISE_OFF

    fun withGain(db: Int) = Fx(db.coerceIn(0, GAIN_STEPS.last()), pitch, noise, skipSilence)

    fun withPitch(semitones: Int) = Fx(gainDb, semitones.coerceIn(PITCH_MIN, PITCH_MAX), noise, skipSilence)

    fun withNoise(level: Int) = Fx(gainDb, pitch, level.coerceIn(NOISE_OFF, NOISE_MAX), skipSilence)

    fun withSkipSilence(on: Boolean) = Fx(gainDb, pitch, noise, on)

    override fun equals(other: Any?) =
        other is Fx && other.gainDb == gainDb && other.pitch == pitch && other.noise == noise && other.skipSilence == skipSilence

    override fun hashCode() = ((gainDb * 31 + pitch) * 31 + noise) * 31 + if (skipSilence) 1 else 0

    companion object {
        val Off = Fx()

        fun decode(text: String?): Fx? {
            val map = Json.parse(text).asMap() ?: return null
            fun int(key: String) = map[key].asNumber()?.toInt() ?: 0
            return Fx(
                int("gain").coerceIn(0, GAIN_STEPS.last()),
                int("pitch").coerceIn(PITCH_MIN, PITCH_MAX),
                int("noise").coerceIn(NOISE_OFF, NOISE_MAX),
                map["skip"] == true,
            )
        }

        fun encode(fx: Fx): String =
            Json.write(linkedMapOf("gain" to fx.gainDb, "pitch" to fx.pitch, "noise" to fx.noise, "skip" to fx.skipSilence))
    }
}
