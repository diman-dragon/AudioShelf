@file:OptIn(UnstableApi::class)

package com.audioshelf.app.sound

import androidx.media3.common.util.UnstableApi
import java.util.concurrent.ConcurrentHashMap

class SoundOutput(private val onNeutral: (Boolean) -> Unit = {}) {
    val chain = SoundChain()
    val processor = SoundProcessor(chain)
    private val trims = ConcurrentHashMap<String, Float>()

    fun apply(bands: List<Int>, trimsByUri: Map<String, Float>, boost: Float) {
        val flat = trimsByUri.isEmpty() && boost <= 1f && bands.all { it == 0 }
        chain.setBands(FloatArray(BAND_COUNT) { bands.getOrElse(it) { 0 }.toFloat() })
        chain.setBoost(boost)
        trims.clear()
        trims.putAll(trimsByUri)
        onNeutral(flat)
    }

    fun useTrimOf(mediaId: String?) {
        chain.setTrimDb(mediaId?.let { trims[it] } ?: 0f)
    }
}
