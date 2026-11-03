package com.audioshelf.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val STORE = "audioshelf.hidden"

class HiddenStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun isHidden(id: String) = kv.get(STORE, id) != null

    fun count() = kv.size(STORE)

    fun hide(id: String) {
        kv.put(STORE, id, "1")
        counter.update { it + 1 }
    }

    fun forget(id: String) {
        kv.del(STORE, id)
        counter.update { it + 1 }
    }

    fun move(from: String, to: String) {
        if (from == to || !isHidden(from)) return
        kv.batch(listOf(KvOp.Del(STORE, from), KvOp.Put(STORE, to, "1")))
        counter.update { it + 1 }
    }

    fun restoreAll() {
        kv.clear(STORE)
        counter.update { it + 1 }
    }
}
