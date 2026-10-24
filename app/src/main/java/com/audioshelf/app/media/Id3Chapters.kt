package com.audioshelf.app.media

import java.nio.charset.Charset

private const val MAX_TAG = 32 shl 20

object Id3Chapters {
    private val CHARSETS: List<Charset> = listOf(Charsets.ISO_8859_1, Charsets.UTF_16, Charsets.UTF_16BE, Charsets.UTF_8)

    fun matches(src: ByteSource): Boolean {
        val b = src.bytes(0, 3) ?: return false
        return b[0] == 'I'.code.toByte() && b[1] == 'D'.code.toByte() && b[2] == '3'.code.toByte()
    }

    fun read(src: ByteSource): List<Chapter> {
        val h = src.bytes(0, 10) ?: return emptyList()
        val version = h[3].toInt() and 0xff
        val size = syncsafe(h, 6)
        if ((version != 3 && version != 4) || size <= 0 || size > MAX_TAG) return emptyList()
        val d = src.bytes(10, size) ?: return emptyList()
        var p = 0
        if ((h[5].toInt() and 0x40) != 0 && d.size >= 4) p = if (version == 4) syncsafe(d, 0) else 4 + beInt(d, 0)
        val out = ArrayList<Chapter>()
        while (p >= 0 && p + 10 <= d.size && d[p].toInt() != 0) {
            val len = if (version == 4) syncsafe(d, p + 4) else beInt(d, p + 4)
            if (len < 0 || p + 10 + len > d.size) break
            if (id(d, p) == "CHAP") chapter(d, p + 10, p + 10 + len, version, out)
            p += 10 + len
        }
        return Chapter.tidy(out)
    }

    private fun id(d: ByteArray, at: Int) = String(d, at, 4, Charsets.ISO_8859_1)

    private fun chapter(d: ByteArray, from: Int, to: Int, version: Int, out: MutableList<Chapter>) {
        var p = from
        while (p < to && d[p].toInt() != 0) p++
        p++
        if (p + 16 > to) return
        val start = beInt(d, p).toLong() and 0xffffffffL
        p += 16
        var title = ""
        while (p + 10 <= to) {
            val len = if (version == 4) syncsafe(d, p + 4) else beInt(d, p + 4)
            if (len < 1 || p + 10 + len > to) break
            if (id(d, p) == "TIT2") title = text(d, p + 10, p + 10 + len)
            p += 10 + len
        }
        out.add(Chapter(title, start))
    }

    private fun text(d: ByteArray, from: Int, to: Int): String {
        val cs = CHARSETS[minOf(3, d[from].toInt() and 0xff)]
        return String(d, from + 1, to - from - 1, cs).replace("﻿", "").replace("\u0000", "")
    }

    private fun syncsafe(d: ByteArray, at: Int): Int =
        ((d[at].toInt() and 0x7f) shl 21) or ((d[at + 1].toInt() and 0x7f) shl 14) or
            ((d[at + 2].toInt() and 0x7f) shl 7) or (d[at + 3].toInt() and 0x7f)

    private fun beInt(d: ByteArray, at: Int): Int =
        ((d[at].toInt() and 0xff) shl 24) or ((d[at + 1].toInt() and 0xff) shl 16) or
            ((d[at + 2].toInt() and 0xff) shl 8) or (d[at + 3].toInt() and 0xff)
}
