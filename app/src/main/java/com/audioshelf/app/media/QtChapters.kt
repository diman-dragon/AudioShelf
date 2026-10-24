package com.audioshelf.app.media

import java.nio.ByteBuffer

private const val MAX_TEXT = 4096

object QtChapters {
    private fun u32(d: ByteArray, at: Int): Long = ByteBuffer.wrap(d, at, 4).int.toLong() and 0xffffffffL

    fun read(src: ByteSource, d: ByteArray, moov: Mp4Box): List<Chapter> {
        var wanted = -1L
        for (trak in moov.all(d, "trak")) {
            val chap = trak.path(d, "tref/chap")
            if (chap != null && chap.end - chap.start >= 4) wanted = u32(d, chap.start)
        }
        if (wanted < 0) return emptyList()
        for (trak in moov.all(d, "trak")) {
            val tkhd = trak.one(d, "tkhd") ?: continue
            if (tkhd.end - tkhd.start < 24) continue
            val idAt = tkhd.start + if (d[tkhd.start].toInt() == 1) 20 else 12
            if (u32(d, idAt) == wanted) return samples(src, d, trak)
        }
        return emptyList()
    }

    private fun samples(src: ByteSource, d: ByteArray, trak: Mp4Box): List<Chapter> {
        val mdhd = trak.path(d, "mdia/mdhd")
        val stbl = trak.path(d, "mdia/minf/stbl")
        if (mdhd == null || stbl == null) return emptyList()
        val scale = u32(d, mdhd.start + if (d[mdhd.start].toInt() == 1) 20 else 12)
        val sizes = QtTables.sizes(d, stbl.one(d, "stsz"))
        val offsets = QtTables.offsets(d, stbl, sizes.size)
        val starts = QtTables.starts(d, stbl.one(d, "stts"), sizes.size, scale)
        if (offsets == null || starts == null) return emptyList()
        val out = ArrayList<Chapter>()
        for (i in sizes.indices) {
            val raw = src.bytes(offsets[i], minOf(sizes[i], MAX_TEXT.toLong()).toInt())
            if (raw != null && raw.size >= 2) out.add(Chapter(text(raw), starts[i]))
        }
        return out
    }

    private fun text(raw: ByteArray): String {
        val len = minOf(((raw[0].toInt() and 0xff) shl 8) or (raw[1].toInt() and 0xff), raw.size - 2)
        val utf16 = len >= 2 && (raw[2].toInt() and 0xff) == 0xFE && (raw[3].toInt() and 0xff) == 0xFF
        return String(raw, 2, len, if (utf16) Charsets.UTF_16 else Charsets.UTF_8).replace("﻿", "")
    }
}
