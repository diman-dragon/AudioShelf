package com.audioshelf.app.player

fun bookProgress(known: Map<Int, Long>, count: Int, index: Int, positionMs: Long): BookProgress? {
    if (known.isEmpty() || count <= 0) return null
    val average = known.values.sum().toDouble() / known.size
    var before = 0.0
    var total = 0.0
    for (i in 0 until count) {
        val length: Double = known[i]?.toDouble() ?: average
        total += length
        if (i < index) before += length
    }
    if (total <= 0) return null
    val elapsed = before + positionMs
    val percent = minOf(100, (elapsed / total * 100).toInt())
    return BookProgress(percent, maxOf(0.0, total - elapsed).toLong(), known.size >= count)
}
