package com.audioshelf.app.media

import java.nio.ByteBuffer

class Mp4Box(val start: Int, val end: Int) {
    fun all(d: ByteArray, type: String): List<Mp4Box> {
        val out = ArrayList<Mp4Box>()
        var p = start
        while (p + 8 <= end) {
            var size = ((d[p].toLong() and 0xff) shl 24) or ((d[p + 1].toLong() and 0xff) shl 16) or
                ((d[p + 2].toLong() and 0xff) shl 8) or (d[p + 3].toLong() and 0xff)
            var head = 8
            if (size == 1L && p + 16 <= end) {
                size = ByteBuffer.wrap(d, p + 8, 8).long
                head = 16
            } else if (size == 0L) {
                size = (end - p).toLong()
            }
            if (size < head || p + size > end) break
            if (String(d, p + 4, 4, Charsets.ISO_8859_1) == type) out.add(Mp4Box(p + head, (p + size).toInt()))
            p += size.toInt()
        }
        return out
    }

    fun one(d: ByteArray, type: String): Mp4Box? = all(d, type).firstOrNull()

    fun path(d: ByteArray, path: String): Mp4Box? {
        var box: Mp4Box? = this
        for (part in path.split("/")) box = box?.one(d, part)
        return box
    }

    companion object {
        fun whole(d: ByteArray) = Mp4Box(0, d.size)
    }
}
