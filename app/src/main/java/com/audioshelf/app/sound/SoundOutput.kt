@file:OptIn(UnstableApi::class)

package com.audioshelf.app.sound

import androidx.media3.common.util.UnstableApi

class SoundOutput(private val onNeutral: (Boolean) -> Unit = {}) {
    val chain = SoundChain()
    val processor = SoundProcessor(chain)

    fun apply(bands: List<Int>) {
        chain.setBands(FloatArray(BAND_COUNT) { bands.getOrElse(it) { 0 }.toFloat() })
        onNeutral(bands.all { it == 0 })
    }
}
