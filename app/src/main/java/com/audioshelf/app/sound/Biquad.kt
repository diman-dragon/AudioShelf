package com.audioshelf.app.sound

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class Biquad private constructor(b0: Double, b1: Double, b2: Double, a1: Double, a2: Double) {
    val b0 = b0.toFloat()
    val b1 = b1.toFloat()
    val b2 = b2.toFloat()
    val a1 = a1.toFloat()
    val a2 = a2.toFloat()

    companion object {
        fun peaking(freq: Double, gainDb: Double, q: Double, sampleRate: Double): Biquad {
            val a = 10.0.pow(gainDb / 40)
            val w = 2 * PI * freq / sampleRate
            val alpha = sin(w) / (2 * q)
            val c = cos(w)
            val a0 = 1 + alpha / a
            return Biquad((1 + alpha * a) / a0, -2 * c / a0, (1 - alpha * a) / a0, -2 * c / a0, (1 - alpha / a) / a0)
        }
    }
}
