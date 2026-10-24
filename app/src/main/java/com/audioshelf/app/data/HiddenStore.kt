package com.audioshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val STORE = "audioshelf.hidden"

class HiddenStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun isHidden(id: String) = kv.get(STORE, id) != null

    fun count() = kv.size(STORE)

    fun hide(id: String) {
        kv.put(STORE, id, "1")
        counter.value++
    }

    fun forget(id: String) {
        kv.del(STORE, id)
        counter.value++
    }

    fun move(from: String, to: String) {
        if (from == to || !isHidden(from)) return
        kv.del(STORE, from)
        kv.put(STORE, to, "1")
        counter.value++
    }

    fun restoreAll() {
        kv.clear(STORE)
        counter.value++
    }
}
