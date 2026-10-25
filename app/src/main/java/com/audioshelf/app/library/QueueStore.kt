package com.audioshelf.app.library

import com.audioshelf.app.data.KV_STORE
import com.audioshelf.app.data.Json
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.asList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val QUEUE = "audioshelf.queue"

class QueueStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun list(): List<String> = Json.parse(kv.get(KV_STORE, QUEUE)).asList()?.mapNotNull { it as? String } ?: emptyList()

    private fun write(ids: List<String>) {
        if (ids.isEmpty()) kv.del(KV_STORE, QUEUE) else kv.put(KV_STORE, QUEUE, Json.write(ids))
        counter.value++
    }

    fun add(id: String): Boolean {
        val now = list()
        if (id in now) return false
        write(now + id)
        return true
    }

    fun remove(id: String) = write(list().filter { it != id })

    fun clear() = write(emptyList())

    fun pop(): String? {
        val now = list()
        val first = now.firstOrNull() ?: return null
        write(now.drop(1))
        return first
    }

    fun move(from: String, to: String) {
        if (from == to) return
        write(list().map { if (it == from) to else it })
    }
}
