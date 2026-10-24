package com.audioshelf.app.library

import java.text.Collator

object NaturalOrder : Comparator<String> {
    private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    private val chunks = Regex("[0-9]+|[^0-9]+")

    override fun compare(a: String, b: String): Int {
        val x = chunks.findAll(a).map { it.value }.toList()
        val y = chunks.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(x.size, y.size)) {
            val c = compareChunk(x[i], y[i])
            if (c != 0) return c
        }
        return x.size - y.size
    }

    private fun compareChunk(p: String, q: String): Int {
        if (p[0] in '0'..'9' && q[0] in '0'..'9') {
            val a = p.trimStart('0')
            val b = q.trimStart('0')
            return if (a.length != b.length) a.length - b.length else a.compareTo(b)
        }
        return collator.compare(p, q)
    }
}
