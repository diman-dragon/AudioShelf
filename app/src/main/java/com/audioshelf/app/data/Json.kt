package com.audioshelf.app.data

object Json {
    fun parse(text: String?): Any? {
        if (text == null) return null
        val reader = JsonReader(text)
        val value = reader.value()
        reader.skipSpace()
        return if (value === JsonReader.Bad || !reader.done()) null else value
    }

    fun write(value: Any?): String = StringBuilder().also { put(it, value) }.toString()

    private fun put(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> quote(out, value)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) out.append(',')
                    first = false
                    quote(out, k.toString())
                    out.append(':')
                    put(out, v)
                }
                out.append('}')
            }
            is List<*> -> {
                out.append('[')
                value.forEachIndexed { i, v ->
                    if (i > 0) out.append(',')
                    put(out, v)
                }
                out.append(']')
            }
            else -> out.append(value.toString())
        }
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
}

fun Any?.asMap(): Map<String, Any?>? {
    if (this !is Map<*, *>) return null
    @Suppress("UNCHECKED_CAST")
    return this as Map<String, Any?>
}

fun Any?.asList(): List<Any?>? = this as? List<Any?>

fun Any?.asNumber(): Number? = this as? Number
