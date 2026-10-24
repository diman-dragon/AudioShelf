package com.audioshelf.app.sound

import com.audioshelf.app.data.Json
import com.audioshelf.app.data.asList
import com.audioshelf.app.data.asMap
import com.audioshelf.app.data.asNumber
import kotlin.math.roundToInt

class Eq(val preset: String, val bands: List<Int>) {
    fun withBand(index: Int, db: Int): Eq = Eq("custom", bands.toMutableList().also { it[index] = db.coerceIn(MIN_DB, MAX_DB) })

    override fun equals(other: Any?) = other is Eq && other.preset == preset && other.bands == bands

    override fun hashCode() = preset.hashCode() * 31 + bands.hashCode()

    companion object {
        val Flat = Eq("flat", FLAT_BANDS)

        fun of(presetId: String) = Eq(presetId, presetBands(presetId))

        fun decode(text: String?): Eq? {
            val map = Json.parse(text).asMap() ?: return null
            val list = map["bands"].asList() ?: return null
            if (list.size != BAND_COUNT) return null
            val bands = list.map { (it.asNumber() ?: return null).toDouble().roundToInt().coerceIn(MIN_DB, MAX_DB) }
            return Eq(map["preset"] as? String ?: "custom", bands)
        }

        fun encode(eq: Eq): String = Json.write(linkedMapOf("preset" to eq.preset, "bands" to eq.bands))
    }
}
