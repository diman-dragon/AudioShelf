package com.audioshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapStore(private val kv: Kv, private val store: String) {
    private val cache = HashMap<String, Map<String, Any?>>()
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun get(id: String): Map<String, Any?>? {
        cache[id]?.let { return it }
        val parsed = FlatJson.parse(kv.get(store, id)) ?: return null
        cache[id] = parsed
        return parsed
    }

    fun set(id: String, value: Map<String, Any?>) {
        cache[id] = value
        kv.put(store, id, FlatJson.write(value))
        counter.value++
    }

    fun del(id: String) {
        cache.remove(id)
        kv.del(store, id)
        counter.value++
    }

    fun move(from: String, to: String) {
        if (from == to) return
        val value = get(from) ?: return
        del(from)
        set(to, value)
    }
}
