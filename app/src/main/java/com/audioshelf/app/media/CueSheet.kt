package com.audioshelf.app.media

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

object CueSheet {
    fun decode(bytes: ByteArray): String {
        val body = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) bytes.copyOfRange(3, bytes.size) else bytes
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(body)).toString()
        } catch (e: CharacterCodingException) {
            String(body, charset("windows-1251"))
        }
    }

    /** Chapters per FILE entry, keyed by lower-cased file name. */
    fun parse(text: String): Map<String, List<Chapter>> {
        val out = LinkedHashMap<String, List<Chapter>>()
        var file: String? = null
        var title: String? = null
        var track = false
        var list = ArrayList<Chapter>()
        fun close() {
            val f = file ?: return
            out[f] = Chapter.tidy(list)
            list = ArrayList()
        }
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("FILE ", ignoreCase = true) -> {
                    close()
                    file = unquote(line.substring(5)).substringAfterLast('/').substringAfterLast('\\').lowercase()
                    track = false
                    title = null
                }
                line.startsWith("TRACK ", ignoreCase = true) -> {
                    track = true
                    title = null
                }
                line.startsWith("TITLE ", ignoreCase = true) && track -> title = unquoteTitle(line.substring(6))
                line.startsWith("INDEX 01", ignoreCase = true) && track -> {
                    val ms = time(line.substring(8).trim()) ?: continue
                    list.add(Chapter(title ?: "", ms))
                }
            }
        }
        close()
        return out
    }

    private fun unquoteTitle(s: String): String {
        val t = s.trim()
        if (t.startsWith("\"")) {
            val end = t.lastIndexOf('"')
            if (end > 0) return t.substring(1, end)
        }
        return t
    }

    private fun unquote(s: String): String {
        val t = s.trim()
        if (t.startsWith("\"")) {
            val end = t.indexOf('"', 1)
            if (end > 0) return t.substring(1, end)
        }
        return t.substringBeforeLast(' ').ifEmpty { t }.trim('"')
    }

    private fun time(s: String): Long? {
        val m = Regex("^(\\d+):(\\d+):(\\d+)$").find(s) ?: return null
        val min = m.groupValues[1].toLong()
        val sec = m.groupValues[2].toLong()
        val frames = m.groupValues[3].toLong()
        return (min * 60 + sec) * 1000 + frames * 1000 / 75
    }
}
