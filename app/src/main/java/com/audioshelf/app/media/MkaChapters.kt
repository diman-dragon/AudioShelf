package com.audioshelf.app.media

private const val EDITION = 0x45B9L
private const val ATOM = 0xB6L
private const val START = 0x91L
private const val HIDDEN = 0x98L
private const val ENABLED = 0x4598L
private const val DISPLAY = 0x80L
private const val STRING = 0x85L

const val CHAPTERS_ID = 0x1043A770L

object MkaChapters {
    fun parse(src: ByteSource, chapters: EbmlElement): List<Chapter> {
        val end = chapters.end(src.length())
        var p = chapters.dataPos
        while (p < end) {
            val e = Ebml.at(src, p) ?: break
            if (e.size == EBML_UNKNOWN) break
            if (e.id == EDITION) {
                val list = ArrayList<Chapter>()
                atoms(src, e.dataPos, e.end(end), list)
                val tidy = Chapter.tidy(list)
                if (tidy.isNotEmpty()) return tidy
            }
            p = e.end(end)
        }
        return emptyList()
    }

    private fun atoms(src: ByteSource, from: Long, to: Long, out: MutableList<Chapter>) {
        var p = from
        while (p < to) {
            val e = Ebml.at(src, p) ?: break
            if (e.size == EBML_UNKNOWN) break
            if (e.id == ATOM) atom(src, e, out)
            p = e.end(to)
        }
    }

    private fun atom(src: ByteSource, atom: EbmlElement, out: MutableList<Chapter>) {
        var start = -1L
        var title = ""
        var shown = true
        var leaf = true
        val end = atom.end(src.length())
        var p = atom.dataPos
        while (p < end) {
            val e = Ebml.at(src, p) ?: break
            if (e.size == EBML_UNKNOWN) break
            when (e.id) {
                START -> start = Ebml.uint(src, e) / 1_000_000L
                HIDDEN -> shown = shown && Ebml.uint(src, e) == 0L
                ENABLED -> shown = shown && Ebml.uint(src, e) != 0L
                DISPLAY -> if (title.isEmpty()) title = display(src, e)
                ATOM -> {
                    leaf = false
                    atom(src, e, out)
                }
            }
            p = e.end(end)
        }
        if (leaf && shown && start >= 0) out.add(Chapter(title, start))
    }

    private fun display(src: ByteSource, display: EbmlElement): String {
        val end = display.end(src.length())
        var p = display.dataPos
        while (p < end) {
            val e = Ebml.at(src, p) ?: break
            if (e.size == EBML_UNKNOWN) break
            if (e.id == STRING) return Ebml.text(src, e)
            p = e.end(end)
        }
        return ""
    }
}
