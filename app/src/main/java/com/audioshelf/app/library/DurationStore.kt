package com.audioshelf.app.library

import com.audioshelf.app.data.Kv

private const val STORE = "durs"
private const val SEP = '\u0001'

class DurationStore(private val kv: Kv) {
    private var byBook: HashMap<String, MutableMap<Int, Long>>? = null

    private fun load(): HashMap<String, MutableMap<Int, Long>> {
        byBook?.let { return it }
        val loaded = HashMap<String, MutableMap<Int, Long>>()
        for ((key, value) in kv.all(STORE)) {
            val at = key.lastIndexOf(SEP)
            val index = key.substring(at + 1).toIntOrNull() ?: continue
            val ms = value.toLongOrNull() ?: continue
            loaded.getOrPut(key.substring(0, at)) { HashMap() }[index] = ms
        }
        byBook = loaded
        return loaded
    }

    fun get(id: String): Map<Int, Long> = load()[id] ?: emptyMap()

    fun note(id: String, index: Int, ms: Long) {
        val book = load().getOrPut(id) { HashMap() }
        if (book[index] == ms) return
        book[index] = ms
        kv.put(STORE, "$id$SEP$index", ms.toString())
    }

    fun forget(id: String) {
        get(id).keys.forEach { kv.del(STORE, "$id$SEP$it") }
        load().remove(id)
    }

    fun move(from: String, to: String) {
        if (from == to) return
        val known = HashMap(get(from))
        forget(from)
        known.forEach { (index, ms) -> note(to, index, ms) }
    }
}
