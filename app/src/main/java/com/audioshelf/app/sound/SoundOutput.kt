@file:androidx.annotation.OptIn(markerClass = [UnstableApi::class])

package com.audioshelf.app.sound

import androidx.media3.common.util.UnstableApi
import com.audioshelf.app.AppLog

class SoundOutput(private val onNeutral: (Boolean) -> Unit = {}) {
    val chain = SoundChain()
    val processor = SoundProcessor(chain)

    private var lastBands: List<Int>? = null
    private var lastBoost = Int.MIN_VALUE

    /** Applies the sound settings; repeated identical calls do nothing. */
    fun apply(bands: List<Int>, boostDb: Int) {
        val normalizedBands = List(BAND_COUNT) { bands.getOrElse(it) { 0 }.coerceIn(MIN_DB, MAX_DB) }
        val normalizedBoost = boostDb.coerceIn(0, 12)
        if (normalizedBands == lastBands && normalizedBoost == lastBoost) return
        lastBands = normalizedBands
        lastBoost = normalizedBoost
        AppLog.i("SoundOutput", "settings: bands=${normalizedBands.joinToString(",")} dB, boost $normalizedBoost dB")
        chain.setSettings(FloatArray(BAND_COUNT) { normalizedBands[it].toFloat() }, normalizedBoost)
        onNeutral(normalizedBands.all { it == 0 } && normalizedBoost == 0)
    }
}
