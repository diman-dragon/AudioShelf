package com.audioshelf.app.data

object FlatJson {
    fun parse(text: String?): Map<String, Any?>? {
        if (text == null) return null
        val reader = Reader(text)
        val result = reader.obj() ?: return null
        reader.skipSpace()
        return if (reader.done()) result else null
    }

    fun write(map: Map<String, Any?>): String {
        val out = StringBuilder("{")
        var first = true
        for ((key, value) in map) {
            if (!first) out.append(',')
            first = false
            quote(out, key)
            out.append(':')
            when (value) {
                null -> out.append("null")
                is String -> quote(out, value)
                else -> out.append(value.toString())
            }
        }
        return out.append('}').toString()
    }

    private fun quote(out: StringBuilder, text: String) {
        out.append('"')
        for (c in text) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u").append(String.format("%04x", c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private object Bad

    private class Reader(private val s: String) {
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

        fun obj(): Map<String, Any?>? {
            skipSpace()
            if (!eat('{')) return null
            val out = LinkedHashMap<String, Any?>()
            skipSpace()
            if (eat('}')) return out
            while (true) {
                skipSpace()
                val key = string() ?: return null
                skipSpace()
                if (!eat(':')) return null
                skipSpace()
                val value = value()
                if (value === Bad) return null
                out[key] = value
                skipSpace()
                if (eat(',')) continue
                return if (eat('}')) out else null
            }
        }

        private fun value(): Any? {
            if (i >= s.length) return Bad
            return when (s[i]) {
                '"' -> string() ?: Bad
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> number()
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
    }
}

fun Map<String, Any?>.str(key: String): String? = this[key] as? String

fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()

fun Map<String, Any?>.int(key: String): Int? = (this[key] as? Number)?.toInt()

fun Map<String, Any?>.flag(key: String): Boolean = when (val v = this[key]) {
    is Boolean -> v
    is Number -> v.toInt() != 0
    else -> false
}
