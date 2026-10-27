@file:OptIn(UnstableApi::class)

package com.audioshelf.app.sound

import androidx.media3.common.util.UnstableApi

class SoundOutput(private val onNeutral: (Boolean) -> Unit = {}) {
    val chain = SoundChain()
    val processor = SoundProcessor(chain)

    private var lastBands: List<Int>? = null
    private var lastBoost = Int.MIN_VALUE
    private var lastSkip: Boolean? = null

    /** Applies the sound settings; repeated identical calls do nothing. */
    fun apply(bands: List<Int>, boostDb: Int, skipSilence: Boolean) {
        if (bands == lastBands && boostDb == lastBoost && skipSilence == lastSkip) return
        lastBands = bands
        lastBoost = boostDb
        lastSkip = skipSilence
        chain.setBands(FloatArray(BAND_COUNT) { bands.getOrElse(it) { 0 }.toFloat() })
        chain.setBoost(boostDb)
        onNeutral(bands.all { it == 0 } && boostDb == 0 && !skipSilence)
    }
}
