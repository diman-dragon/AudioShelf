package com.audioshelf.app.player

import java.util.Locale

fun fmtTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
}

fun fmtSpeed(speed: Float): String = "${speed.toString().removeSuffix(".0")}×"
