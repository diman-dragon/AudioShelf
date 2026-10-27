package com.audioshelf.app.sound

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asMap
import com.audioshelf.app.data.asNumber
import kotlin.math.pow

val GAIN_STEPS = listOf(0, 3, 6, 9, 12)

/** Per-book / global playback effects: volume boost and skip silence. */
class Fx(
    val gainDb: Int = 0,
    val skipSilence: Boolean = false,
) {
    val gain: Float get() = 10.0.pow(gainDb / 20.0).toFloat()

    val neutral: Boolean get() = gainDb == 0 && !skipSilence

    fun withGain(db: Int) = Fx(db.coerceIn(0, GAIN_STEPS.last()), skipSilence)

    fun withSkipSilence(on: Boolean) = Fx(gainDb, on)

    override fun equals(other: Any?) = other is Fx && other.gainDb == gainDb && other.skipSilence == skipSilence

    override fun hashCode() = gainDb * 31 + if (skipSilence) 1 else 0

    companion object {
        val Off = Fx()

        /** Old records also carried pitch / noise; those keys are ignored now. */
        fun decode(text: String?): Fx? {
            val map = Json.parse(text).asMap() ?: return null
            val gain = map["gain"].asNumber()?.toInt() ?: 0
            return Fx(gain.coerceIn(0, GAIN_STEPS.last()), map["skip"] == true)
        }

        fun encode(fx: Fx): String = Json.write(linkedMapOf("gain" to fx.gainDb, "skip" to fx.skipSilence))
    }
}
