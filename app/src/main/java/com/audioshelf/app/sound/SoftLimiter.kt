package com.audioshelf.app.sound

import kotlin.math.abs
import kotlin.math.tanh

private const val KNEE = 0.8f * 32768f
private const val CEILING = 32767f

object SoftLimiter {
    fun soften(x: Float): Float {
        val a = abs(x)
        if (a <= KNEE) return x
        val room = CEILING - KNEE
        val y = KNEE + room * tanh((a - KNEE) / room)
        return if (x < 0) -y else y
    }
}
