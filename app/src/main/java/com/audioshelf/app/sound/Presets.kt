package com.audioshelf.app.sound

class Preset(val id: String, val bands: List<Int>)

val PRESETS = listOf(
    Preset("flat", FLAT_BANDS),
    Preset("voice", listOf(-6, -5, -3, -1, 1, 3, 4, 4, 2, 0)),
    Preset("rock", listOf(5, 4, 3, 1, -1, -1, 1, 3, 4, 5)),
    Preset("pop", listOf(-1, 1, 3, 4, 3, 0, -1, -1, 1, 2)),
    Preset("jazz", listOf(3, 2, 1, 2, -1, -1, 0, 1, 2, 3)),
    Preset("classical", listOf(3, 2, 1, 0, 0, 0, -1, -1, 2, 3)),
    Preset("electronic", listOf(5, 4, 1, 0, -2, 2, 1, 3, 4, 5)),
    Preset("bass", listOf(6, 5, 4, 2, 1, 0, 0, 0, 0, 0)),
    Preset("treble", listOf(0, 0, 0, 0, 0, 1, 3, 4, 5, 6)),
)

fun presetBands(id: String): List<Int> = PRESETS.firstOrNull { it.id == id }?.bands ?: FLAT_BANDS
