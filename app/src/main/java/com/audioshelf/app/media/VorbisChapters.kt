package com.audioshelf.app.media

private const val MAX_COMMENT = 16 shl 20
private const val MAX_PAGES = 512

object VorbisChapters {
    fun matches(src: ByteSource): Boolean {
        val b = src.bytes(0, 4) ?: return false
        val tag = String(b, Charsets.ISO_8859_1)
        return tag == "OggS" || tag == "fLaC"
    }

    fun read(src: ByteSource): List<Chapter> {
        val head = src.bytes(0, 4) ?: return emptyList()
        val comments = if (String(head, Charsets.ISO_8859_1) == "fLaC") flacComments(src) else oggComments(src)
        return fromComments(comments)
    }

    fun fromComments(comments: List<String>): List<Chapter> {
        val times = HashMap<String, Long>()
        val names = HashMap<String, String>()
        for (line in comments) {
            val eq = line.indexOf('=')
            if (eq < 8) continue
            val key = line.substring(0, eq).uppercase()
            if (!key.startsWith("CHAPTER")) continue
            val rest = key.substring(7)
            val digits = rest.takeWhile { it.isDigit() }
            if (digits.isEmpty()) continue
            val value = line.substring(eq + 1)
            when (rest.substring(digits.length)) {
                "" -> parseTime(value)?.let { times[digits] = it }
                "NAME" -> names[digits] = value
            }
        }
        return Chapter.tidy(times.map { (n, ms) -> Chapter(names[n] ?: "", ms) })
    }

    fun parseTime(text: String): Long? {
        val m = Regex("^\\s*(?:(\\d+):)?(\\d+):(\\d+)(?:[.,](\\d{1,3}))?\\s*$").find(text) ?: return null
        val h = m.groupValues[1].toLongOrNull() ?: 0L
        val min = m.groupValues[2].toLong()
        val sec = m.groupValues[3].toLong()
        val frac = m.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return ((h * 60 + min) * 60 + sec) * 1000 + frac
    }

    private fun flacComments(src: ByteSource): List<String> {
        var p = 4L
        val length = src.length()
        while (p + 4 <= length) {
            val h = src.bytes(p, 4) ?: return emptyList()
            val last = (h[0].toInt() and 0x80) != 0
            val type = h[0].toInt() and 0x7f
            val size = ((h[1].toInt() and 0xff) shl 16) or ((h[2].toInt() and 0xff) shl 8) or (h[3].toInt() and 0xff)
            if (type == 4) {
                if (size > MAX_COMMENT) return emptyList()
                val body = src.bytes(p + 4, size) ?: return emptyList()
                return parseComments(body, 0)
            }
            if (last) break
            p += 4L + size
        }
        return emptyList()
    }

    private fun oggComments(src: ByteSource): List<String> {
        val length = src.length()
        var p = 0L
        var packet = java.io.ByteArrayOutputStream()
        var index = 0
        var pages = 0
        while (p + 27 <= length && pages++ < MAX_PAGES) {
            val h = src.bytes(p, 27) ?: return emptyList()
            if (String(h, 0, 4, Charsets.ISO_8859_1) != "OggS") return emptyList()
            val segments = h[26].toInt() and 0xff
            val table = src.bytes(p + 27, segments) ?: return emptyList()
            var at = p + 27 + segments
            for (len in table) {
                val n = len.toInt() and 0xff
                if (n > 0) {
                    val chunk = src.bytes(at, n) ?: return emptyList()
                    packet.write(chunk)
                    at += n
                }
                if (packet.size() > MAX_COMMENT) return emptyList()
                if (n < 255) {
                    if (index == 1) return fromPacket(packet.toByteArray())
                    index++
                    packet = java.io.ByteArrayOutputStream()
                }
            }
            p = at
        }
        return emptyList()
    }

    private fun fromPacket(d: ByteArray): List<String> {
        if (d.size > 8 && String(d, 0, 8, Charsets.ISO_8859_1) == "OpusTags") return parseComments(d, 8)
        if (d.size > 7 && d[0].toInt() == 3 && String(d, 1, 6, Charsets.ISO_8859_1) == "vorbis") return parseComments(d, 7)
        return emptyList()
    }

    fun parseComments(d: ByteArray, start: Int): List<String> {
        var p = start
        fun int(): Int? {
            if (p + 4 > d.size) return null
            val v = (d[p].toInt() and 0xff) or ((d[p + 1].toInt() and 0xff) shl 8) or
                ((d[p + 2].toInt() and 0xff) shl 16) or ((d[p + 3].toInt() and 0xff) shl 24)
            p += 4
            return v
        }
        val vendor = int() ?: return emptyList()
        if (vendor < 0 || p + vendor > d.size) return emptyList()
        p += vendor
        val count = int() ?: return emptyList()
        val out = ArrayList<String>()
        var i = 0
        while (i++ < count) {
            val len = int() ?: break
            if (len < 0 || p + len > d.size) break
            out.add(String(d, p, len, Charsets.UTF_8))
            p += len
        }
        return out
    }
}
