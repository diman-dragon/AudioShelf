package com.audioshelf.app.data

class KvRow(val store: String, val key: String, val value: String)

sealed interface KvOp {
    class Put(val store: String, val key: String, val value: String) : KvOp
    class Del(val store: String, val key: String) : KvOp
    class Clear(val store: String) : KvOp
}

interface KvBackend {
    fun loadAll(): List<KvRow>
    fun write(ops: List<KvOp>)
}
