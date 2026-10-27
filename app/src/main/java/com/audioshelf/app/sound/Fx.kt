package com.audioshelf.app.sound

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asMap

/** Per-book / global playback effects. Volume boost lives in the settings, not here. */
class Fx(val skipSilence: Boolean = false) {
    val neutral: Boolean get() = !skipSilence

    fun withSkipSilence(on: Boolean) = Fx(on)

    override fun equals(other: Any?) = other is Fx && other.skipSilence == skipSilence

    override fun hashCode() = if (skipSilence) 1 else 0

    companion object {
        val Off = Fx()

        /** Old records also carried gain / pitch / noise; those keys are ignored now. */
        fun decode(text: String?): Fx? {
            val map = Json.parse(text).asMap() ?: return null
            return Fx(map["skip"] == true)
        }

        fun encode(fx: Fx): String = Json.write(linkedMapOf("skip" to fx.skipSilence))
    }
}
