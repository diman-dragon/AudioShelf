package com.audioshelf.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FLUSH_MS = 250L
private const val RETRY_MS = 1000L

class Kv(
    private val backend: KvBackend,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val buckets = HashMap<String, MutableMap<String, String>>()
    private val pending = ArrayList<KvOp>()
    private var timer: Job? = null

    init {
        backend.loadAll().forEach { bucket(it.store)[it.key] = it.value }
    }

    private fun bucket(store: String) = buckets.getOrPut(store) { LinkedHashMap() }

    fun get(store: String, key: String): String? = bucket(store)[key]

    fun stores(): List<String> = buckets.keys.toList()

    fun size(store: String): Int = bucket(store).size

    fun all(store: String): Map<String, String> = LinkedHashMap(bucket(store))

    fun put(store: String, key: String, value: String) {
        if (bucket(store)[key] == value) return
        bucket(store)[key] = value
        queue(KvOp.Put(store, key, value))
    }

    fun del(store: String, key: String) {
        if (bucket(store).remove(key) == null) return
        queue(KvOp.Del(store, key))
    }

    fun clear(store: String) {
        bucket(store).clear()
        queue(KvOp.Clear(store))
    }

    fun flushNow() {
        timer?.cancel()
        val ops = take()
        if (ops.isEmpty()) return
        runCatching { backend.write(ops) }.onFailure { requeue(ops) }
    }

    private fun take(): List<KvOp> = synchronized(pending) {
        val ops = coalesce(pending)
        pending.clear()
        ops
    }

    private fun requeue(ops: List<KvOp>) = synchronized(pending) { pending.addAll(0, ops) }

    private fun queue(op: KvOp) {
        synchronized(pending) { pending.add(op) }
        if (timer?.isActive == true) return
        timer = scope.launch {
            delay(FLUSH_MS)
            while (true) {
                val ops = take()
                if (ops.isEmpty()) break
                val ok = withContext(io) { runCatching { backend.write(ops) }.isSuccess }
                if (!ok) {
                    requeue(ops)
                    delay(RETRY_MS)
                }
            }
        }
    }
}
