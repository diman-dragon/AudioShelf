package com.audioshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapStore(private val kv: Kv, private val store: String) {
    private val cache = HashMap<String, Map<String, Any?>>()
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun get(id: String): Map<String, Any?>? = synchronized(cache) {
        cache[id]?.let { return it }
        val parsed = FlatJson.parse(kv.get(store, id)) ?: return null
        cache[id] = parsed
        parsed
    }

    fun set(id: String, value: Map<String, Any?>) {
        synchronized(cache) {
            kv.put(store, id, FlatJson.write(value))
            cache[id] = value
        }
        counter.update { it + 1 }
    }

    fun del(id: String) {
        synchronized(cache) {
            kv.del(store, id)
            cache.remove(id)
        }
        counter.update { it + 1 }
    }

    fun move(from: String, to: String) {
        if (from == to) return
        synchronized(cache) {
            val value = get(from) ?: return
            kv.batch(listOf(deleteOp(from), putOp(to, value)))
            cache.remove(from)
            cache[to] = value
        }
        counter.update { it + 1 }
    }

    internal fun putOp(id: String, value: Map<String, Any?>) = com.audioshelf.app.data.KvOp.Put(store, id, FlatJson.write(value))

    internal fun deleteOp(id: String) = com.audioshelf.app.data.KvOp.Del(store, id)

    internal fun cachePut(id: String, value: Map<String, Any?>) {
        synchronized(cache) { cache[id] = value }
    }

    internal fun cacheDelete(id: String) {
        synchronized(cache) { cache.remove(id) }
    }
}
