package com.audioshelf.app.media

const val EBML_UNKNOWN = -1L

class EbmlElement(val id: Long, val dataPos: Long, val size: Long) {
    fun end(limit: Long): Long = if (size == EBML_UNKNOWN) limit else minOf(limit, dataPos + size)
}

object Ebml {
    fun at(src: ByteSource, pos: Long): EbmlElement? {
        val first = src.bytes(pos, 1) ?: return null
        val idLen = ((first[0].toInt() and 0xff) shl 24).countLeadingZeroBits() + 1
        if (idLen > 4) return null
        val idBytes = src.bytes(pos, idLen) ?: return null
        val sizeFirst = src.bytes(pos + idLen, 1) ?: return null
        var id = 0L
        for (x in idBytes) id = (id shl 8) or (x.toLong() and 0xff)
        val sizeLen = ((sizeFirst[0].toInt() and 0xff) shl 24).countLeadingZeroBits() + 1
        if (sizeLen > 8) return null
        val sizeBytes = src.bytes(pos + idLen, sizeLen) ?: return null
        val mask = (1 shl (8 - sizeLen)) - 1
        var size = ((sizeBytes[0].toInt() and 0xff) and mask).toLong()
        var allOnes = size == mask.toLong()
        for (i in 1 until sizeLen) {
            val v = sizeBytes[i].toInt() and 0xff
            size = (size shl 8) or v.toLong()
            allOnes = allOnes && v == 0xff
        }
        return EbmlElement(id, pos + idLen + sizeLen, if (allOnes) EBML_UNKNOWN else size)
    }

    fun uint(src: ByteSource, e: EbmlElement): Long {
        if (e.size > 8 || e.size < 0) return 0
        val b = src.bytes(e.dataPos, e.size.toInt()) ?: return 0
        var v = 0L
        for (x in b) v = (v shl 8) or (x.toLong() and 0xff)
        return v
    }

    fun text(src: ByteSource, e: EbmlElement): String {
        if (e.size < 0 || e.size > 4096) return ""
        val b = src.bytes(e.dataPos, e.size.toInt()) ?: return ""
        return String(b, Charsets.UTF_8).replace("\u0000", "")
    }
}
