package com.audioshelf.app.sound

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asMap
import com.audioshelf.app.data.asNumber
import kotlin.math.pow

val GAIN_STEPS = listOf(0, 3, 6, 9, 12)

/** Per-book / global volume boost. */
class Fx(val gainDb: Int = 0) {
    val gain: Float get() = 10.0.pow(gainDb / 20.0).toFloat()

    val neutral: Boolean get() = gainDb == 0

    fun withGain(db: Int) = Fx(db.coerceIn(0, GAIN_STEPS.last()))

    /** The next step in the cycle 0 -> +3 -> +6 -> +9 -> +12 -> 0. */
    fun nextGain() = Fx(GAIN_STEPS[(GAIN_STEPS.indexOf(gainDb).coerceAtLeast(0) + 1) % GAIN_STEPS.size])

    override fun equals(other: Any?) = other is Fx && other.gainDb == gainDb

    override fun hashCode() = gainDb

    companion object {
        val Off = Fx()

        /** Old records also carried pitch / noise / skip; those keys are ignored now. */
        fun decode(text: String?): Fx? {
            val map = Json.parse(text).asMap() ?: return null
            val gain = map["gain"].asNumber()?.toInt() ?: 0
            return Fx(gain.coerceIn(0, GAIN_STEPS.last()))
        }

        fun encode(fx: Fx): String = Json.write(linkedMapOf("gain" to fx.gainDb))
    }
}
