package com.audioshelf.app.data

internal class JsonReader(private val s: String) {
    private var i = 0

    fun done() = i >= s.length

    fun skipSpace() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun eat(c: Char): Boolean {
        if (i < s.length && s[i] == c) {
            i++
            return true
        }
        return false
    }

    fun value(): Any? {
        skipSpace()
        if (i >= s.length) return Bad
        return when (s[i]) {
            '{' -> obj()
            '[' -> list()
            '"' -> string() ?: Bad
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> number()
        }
    }

    private fun obj(): Any? {
        i++
        val out = LinkedHashMap<String, Any?>()
        skipSpace()
        if (eat('}')) return out
        while (true) {
            skipSpace()
            val key = string() ?: return Bad
            skipSpace()
            if (!eat(':')) return Bad
            val v = value()
            if (v === Bad) return Bad
            out[key] = v
            skipSpace()
            if (eat(',')) continue
            return if (eat('}')) out else Bad
        }
    }

    private fun list(): Any? {
        i++
        val out = ArrayList<Any?>()
        skipSpace()
        if (eat(']')) return out
        while (true) {
            val v = value()
            if (v === Bad) return Bad
            out.add(v)
            skipSpace()
            if (eat(',')) continue
            return if (eat(']')) out else Bad
        }
    }

    private fun literal(word: String, value: Any?): Any? {
        if (!s.startsWith(word, i)) return Bad
        i += word.length
        return value
    }

    private fun number(): Any? {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        val text = s.substring(start, i)
        if (text.isEmpty()) return Bad
        return text.toLongOrNull() ?: text.toDoubleOrNull() ?: Bad
    }

    private fun string(): String? {
        if (!eat('"')) return null
        val out = StringBuilder()
        while (i < s.length) {
            val c = s[i++]
            if (c == '"') return out.toString()
            if (c != '\\') {
                out.append(c)
                continue
            }
            if (i >= s.length) return null
            when (val e = s[i++]) {
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'b' -> out.append('\b')
                'f' -> out.append('\u000C')
                'u' -> {
                    if (i + 4 > s.length) return null
                    val code = s.substring(i, i + 4).toIntOrNull(16) ?: return null
                    out.append(code.toChar())
                    i += 4
                }
                else -> out.append(e)
            }
        }
        return null
    }

    object Bad
}
