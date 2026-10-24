package com.audioshelf.app.sound

const val BAND_COUNT = 10
const val MIN_DB = -12
const val MAX_DB = 12

val BAND_HZ = doubleArrayOf(31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0)
val BAND_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

val FLAT_BANDS: List<Int> = List(BAND_COUNT) { 0 }
