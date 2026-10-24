package com.audioshelf.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MemoryBackend(rows: List<KvRow> = emptyList()) : KvBackend {
    val table = LinkedHashMap<Pair<String, String>, String>()
    var writes = 0

    init {
        rows.forEach { table[it.store to it.key] = it.value }
    }

    override fun loadAll(): List<KvRow> = table.map { (k, v) -> KvRow(k.first, k.second, v) }

    override fun write(ops: List<KvOp>) {
        writes++
        for (op in ops) {
            when (op) {
                is KvOp.Put -> table[op.store to op.key] = op.value
                is KvOp.Del -> table.remove(op.store to op.key)
                is KvOp.Clear -> table.keys.removeAll { it.first == op.store }
            }
        }
    }
}

fun testKv(backend: MemoryBackend = MemoryBackend()) = Kv(backend, CoroutineScope(Dispatchers.Unconfined))
