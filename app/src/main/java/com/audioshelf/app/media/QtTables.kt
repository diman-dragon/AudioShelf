package com.audioshelf.app.media

import java.nio.ByteBuffer

private const val MAX_SAMPLES = 100_000

object QtTables {
    private fun u32(b: ByteBuffer, at: Int): Long = b.getInt(at).toLong() and 0xffffffffL

    private fun view(d: ByteArray, box: Mp4Box): ByteBuffer = ByteBuffer.wrap(d, box.start, box.end - box.start).slice()

    fun sizes(d: ByteArray, stsz: Mp4Box?): LongArray {
        if (stsz == null) return LongArray(0)
        val b = view(d, stsz)
        val fixed = u32(b, 4)
        val count = minOf(u32(b, 8), MAX_SAMPLES.toLong()).toInt()
        return LongArray(count) { i ->
            when {
                fixed != 0L -> fixed
                12 + 4 * i + 4 <= b.limit() -> u32(b, 12 + 4 * i)
                else -> 0L
            }
        }
    }

    fun offsets(d: ByteArray, stbl: Mp4Box, samples: Int): LongArray? {
        val stsc = stbl.one(d, "stsc")
        val stco = stbl.one(d, "stco")
        val co = stco ?: stbl.one(d, "co64")
        if (stsc == null || co == null) return null
        val c = view(d, co)
        val chunks = c.getInt(4)
        val s = view(d, stsc)
        val entries = s.getInt(4)
        val sizes = sizes(d, stbl.one(d, "stsz"))
        val out = LongArray(samples)
        var sample = 0
        var chunk = 1
        while (chunk <= chunks && sample < samples) {
            var per = 1
            for (e in 0 until entries) if (u32(s, 8 + 12 * e) <= chunk) per = s.getInt(12 + 12 * e)
            var at = if (stco != null) u32(c, 8 + 4 * (chunk - 1)) else c.getLong(8 + 8 * (chunk - 1))
            var k = 0
            while (k < per && sample < samples) {
                out[sample] = at
                at += sizes[sample++]
                k++
            }
            chunk++
        }
        return out
    }

    fun starts(d: ByteArray, stts: Mp4Box?, samples: Int, scale: Long): LongArray? {
        if (stts == null || scale <= 0) return null
        val b = view(d, stts)
        val entries = b.getInt(4)
        val out = LongArray(samples)
        var time = 0L
        var i = 0
        var e = 0
        while (e < entries && i < samples) {
            val count = b.getInt(8 + 8 * e)
            val delta = u32(b, 12 + 8 * e)
            var k = 0
            while (k < count && i < samples) {
                out[i++] = time * 1000 / scale
                time += delta
                k++
            }
            e++
        }
        return out
    }
}
