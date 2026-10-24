package com.audioshelf.app.media

import java.nio.ByteBuffer

private const val MAX_MOOV = 64 shl 20

object Mp4Chapters {
    fun matches(src: ByteSource): Boolean {
        val b = src.bytes(4, 4) ?: return false
        return String(b, Charsets.ISO_8859_1) == "ftyp"
    }

    fun read(src: ByteSource): List<Chapter> {
        val length = src.length()
        var p = 0L
        while (p + 8 <= length) {
            val head = src.bytes(p, minOf(16L, length - p).toInt()) ?: break
            if (head.size < 8) break
            var size = ByteBuffer.wrap(head, 0, 4).int.toLong() and 0xffffffffL
            var hlen = 8
            if (size == 1L && head.size >= 16) {
                size = ByteBuffer.wrap(head, 8, 8).long
                hlen = 16
            } else if (size == 0L) {
                size = length - p
            }
            if (size < hlen) break
            if (String(head, 4, 4, Charsets.ISO_8859_1) == "moov") {
                if (size - hlen > MAX_MOOV) break
                val moov = src.bytes(p + hlen, (size - hlen).toInt()) ?: return emptyList()
                return fromMoov(src, moov)
            }
            p += size
        }
        return emptyList()
    }

    private fun fromMoov(src: ByteSource, d: ByteArray): List<Chapter> {
        val moov = Mp4Box.whole(d)
        val list = Chapter.tidy(chpl(d, moov.path(d, "udta/chpl")))
        return list.ifEmpty { Chapter.tidy(QtChapters.read(src, d, moov)) }
    }

    private fun chpl(d: ByteArray, box: Mp4Box?): List<Chapter> {
        if (box == null || box.end - box.start < 9) return emptyList()
        val out = ArrayList<Chapter>()
        val b = ByteBuffer.wrap(d, box.start, box.end - box.start)
        b.position(box.start + 8)
        val count = b.get().toInt() and 0xff
        var i = 0
        while (i < count && b.remaining() >= 9) {
            val start = b.long / 10_000L
            val len = b.get().toInt() and 0xff
            if (len > b.remaining()) break
            val t = ByteArray(len)
            b.get(t)
            out.add(Chapter(String(t, Charsets.UTF_8), start))
            i++
        }
        return out
    }
}
