package com.audioshelf.app.media

private const val SEGMENT = 0x18538067L
private const val ATTACHMENTS = 0x1941A469L
private const val FILE = 0x61A7L
private const val NAME = 0x466EL
private const val MIME = 0x4660L
private const val DATA = 0x465CL
private const val MAX_PICTURE = 8L shl 20
private const val MAX_ELEMENTS = 400_000
private val IMAGE_NAME = Regex(".*\\.(jpe?g|png|webp)$")

class FileInfo(val chapters: List<Chapter> = emptyList(), val cover: ByteArray? = null)

object MkaFile {
    fun matches(src: ByteSource): Boolean {
        val b = src.bytes(0, 4) ?: return false
        return (b[0].toInt() and 0xff) == 0x1A && (b[1].toInt() and 0xff) == 0x45 &&
            (b[2].toInt() and 0xff) == 0xDF && (b[3].toInt() and 0xff) == 0xA3
    }

    fun read(src: ByteSource, wantCover: Boolean): FileInfo {
        val length = src.length()
        val head = Ebml.at(src, 0)
        val start = if (head == null || head.size == EBML_UNKNOWN) length else head.end(length)
        val segment = Ebml.at(src, start)
        if (segment == null || segment.id != SEGMENT) return FileInfo()
        val end = segment.end(length)
        var chapters = emptyList<Chapter>()
        var cover: ByteArray? = null
        var gotChapters = false
        var gotCover = !wantCover
        var p = segment.dataPos
        var guard = 0
        while (p < end && guard++ < MAX_ELEMENTS && !(gotChapters && gotCover)) {
            val e = Ebml.at(src, p) ?: break
            if (e.size == EBML_UNKNOWN) break
            if (e.id == CHAPTERS_ID && !gotChapters) {
                chapters = MkaChapters.parse(src, e)
                gotChapters = true
            } else if (e.id == ATTACHMENTS && !gotCover) {
                cover = pickCover(src, e)
                gotCover = true
            }
            p = e.end(end)
        }
        return FileInfo(chapters, cover)
    }

    private fun pickCover(src: ByteSource, attachments: EbmlElement): ByteArray? {
        var best: ByteArray? = null
        var bestNamed = false
        val end = attachments.end(src.length())
        var p = attachments.dataPos
        while (p < end) {
            val file = Ebml.at(src, p) ?: break
            if (file.size == EBML_UNKNOWN) break
            if (file.id == FILE) {
                val picture = picture(src, file, end)
                if (picture != null && (best == null || (picture.named && !bestNamed))) {
                    best = picture.bytes
                    bestNamed = picture.named
                }
            }
            p = file.end(end)
        }
        return best
    }

    private class Picture(val bytes: ByteArray, val named: Boolean)

    private fun picture(src: ByteSource, file: EbmlElement, limit: Long): Picture? {
        var name = ""
        var mime = ""
        var data: EbmlElement? = null
        val end = file.end(limit)
        var q = file.dataPos
        while (q < end) {
            val e = Ebml.at(src, q) ?: break
            if (e.size == EBML_UNKNOWN) break
            when (e.id) {
                NAME -> name = Ebml.text(src, e).lowercase()
                MIME -> mime = Ebml.text(src, e).lowercase()
                DATA -> data = e
            }
            q = e.end(end)
        }
        val image = mime.startsWith("image/") || IMAGE_NAME.matches(name)
        val d = data ?: return null
        if (!image || d.size <= 0 || d.size > MAX_PICTURE) return null
        val bytes = src.bytes(d.dataPos, d.size.toInt()) ?: return null
        val named = "cover" in name || "folder" in name || "front" in name
        return Picture(bytes, named)
    }
}
