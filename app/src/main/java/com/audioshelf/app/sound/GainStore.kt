package com.audioshelf.app.sound

import com.audioshelf.app.data.Kv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val STORE = "audioshelf.gain"

class GainStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun has(uri: String): Boolean = kv.get(STORE, uri) != null

    fun get(uri: String): Float = kv.get(STORE, uri)?.toFloatOrNull() ?: 0f

    fun save(uri: String, db: Float) {
        kv.put(STORE, uri, db.toString())
        counter.value++
    }

    fun move(from: String, to: String) {
        if (from == to) return
        val value = kv.get(STORE, from) ?: return
        kv.del(STORE, from)
        kv.put(STORE, to, value)
        counter.value++
    }

    fun forget(uri: String) {
        kv.del(STORE, uri)
        counter.value++
    }
}
