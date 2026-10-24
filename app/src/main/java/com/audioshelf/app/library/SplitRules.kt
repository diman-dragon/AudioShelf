package com.audioshelf.app.library

private val DISC = Regex("^(cd|disc|disk|диск|part|часть|том|vol(?:ume)?\\.?)[\\s_-]*\\d+$", RegexOption.IGNORE_CASE)
private val STANDALONE = Regex("\\.m4b$", RegexOption.IGNORE_CASE)
private const val BIG = 40L * 1048576
private const val NUMBERED = 0.8

fun groupDir(dir: String): String {
    val parts = dir.split('/')
    return if (parts.size > 1 && DISC.matches(parts.last())) parts.dropLast(1).joinToString("/") else dir
}

fun splitGroup(files: List<LibFile>): List<List<LibFile>> {
    val whole = files.filter { STANDALONE.containsMatchIn(it.name) }
    val rest = files.filterNot { STANDALONE.containsMatchIn(it.name) }
    val numbered = rest.count { f -> splitName(f.name).first.any { it in '0'..'9' } }
    val avg = if (rest.isEmpty()) 0.0 else rest.sumOf { it.size }.toDouble() / rest.size
    val separate = rest.size > 1 && avg >= BIG && numbered.toDouble() / rest.size < NUMBERED
    val tail = when {
        separate -> rest.map { listOf(it) }
        rest.isNotEmpty() -> listOf(rest)
        else -> emptyList()
    }
    return whole.map { listOf(it) } + tail
}
