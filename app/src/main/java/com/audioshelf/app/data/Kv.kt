package com.audioshelf.app.data

import com.audioshelf.app.AppLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Kv"
private const val FLUSH_MS = 250L
private const val RETRY_MS = 1000L
private const val LOG_EVERY_FAILURE = 10
private const val SLOW_MS = 50L

private enum class Drain { Empty, Written, Failed }

/**
 * Every row lives in memory; writes are queued and reach the database in batches.
 *
 * Writes are ordered: the timer and [flushNow] both go through [drain], which takes the queue and writes
 * it under one lock. A failed batch goes back to the front of the queue before anyone else can take
 * the next one, so an older value can never land on disk after a newer one.
 */
class Kv(
    private val backend: KvBackend,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val before: suspend () -> Unit = {},
) {
    private val buckets = HashMap<String, MutableMap<String, String>>()
    private val pending = ArrayList<KvOp>()
    private val writeLock = Any()
    private var scheduled = false // guarded by [pending]
    private var failures = 0 // guarded by [writeLock]

    init {
        val rows = backend.loadAll()
        rows.forEach { bucket(it.store)[it.key] = it.value }
        AppLog.i(TAG, "loaded ${rows.size} rows in ${buckets.size} stores")
    }

    private fun bucket(store: String) = buckets.getOrPut(store) { LinkedHashMap() }

    fun get(store: String, key: String): String? = synchronized(buckets) { bucket(store)[key] }

    fun stores(): List<String> = synchronized(buckets) { buckets.keys.toList() }

    fun size(store: String): Int = synchronized(buckets) { bucket(store).size }

    fun all(store: String): Map<String, String> = synchronized(buckets) { LinkedHashMap(bucket(store)) }

    fun put(store: String, key: String, value: String) {
        synchronized(buckets) {
            val rows = bucket(store)
            if (rows[key] == value) return
            rows[key] = value
        }
        queue(KvOp.Put(store, key, value))
    }

    fun del(store: String, key: String) {
        if (synchronized(buckets) { bucket(store).remove(key) } == null) return
        queue(KvOp.Del(store, key))
    }

    fun clear(store: String) {
        synchronized(buckets) { bucket(store).clear() }
        queue(KvOp.Clear(store))
    }

    /** Writes everything queued so far, on the calling thread. A failed batch stays queued and is retried by the timer. */
    fun flushNow() {
        val start = System.nanoTime()
        val result = drain()
        val ms = (System.nanoTime() - start) / 1_000_000
        if (ms >= SLOW_MS) AppLog.w(TAG, "flushNow held ${Thread.currentThread().name} for $ms ms")
        if (result == Drain.Failed) ensureFlusher()
    }

    private fun drain(): Drain {
        synchronized(writeLock) {
            val ops = synchronized(pending) {
                val taken = coalesce(pending)
                pending.clear()
                taken
            }
            if (ops.isEmpty()) return Drain.Empty
            val start = System.nanoTime()
            return try {
                backend.write(ops)
                val ms = (System.nanoTime() - start) / 1_000_000
                if (ms >= SLOW_MS) AppLog.w(TAG, "slow write: ${ops.size} ops took $ms ms") else AppLog.d(TAG, "wrote ${ops.size} ops in $ms ms")
                if (failures > 0) AppLog.i(TAG, "write recovered after $failures failed attempts")
                failures = 0
                Drain.Written
            } catch (e: Exception) {
                failures++
                if (failures == 1 || failures % LOG_EVERY_FAILURE == 0) {
                    AppLog.e(TAG, "write of ${ops.size} ops failed (attempt $failures), will retry", e)
                }
                synchronized(pending) { pending.addAll(0, ops) }
                Drain.Failed
            }
        }
    }

    private fun queue(op: KvOp) {
        synchronized(pending) { pending.add(op) }
        ensureFlusher()
    }

    /** One flusher at a time. It stops only after it has seen the queue empty under the same lock [queue] adds under, so nothing is left behind. */
    private fun ensureFlusher() {
        val start = synchronized(pending) {
            if (scheduled) false else { scheduled = true; true }
        }
        if (start) scope.launch { flushLoop() }
    }

    private suspend fun flushLoop() {
        delay(FLUSH_MS)
        while (true) {
            val idle = synchronized(pending) {
                if (pending.isEmpty()) scheduled = false
                pending.isEmpty()
            }
            if (idle) return
            before()
            if (withContext(io) { drain() } == Drain.Failed) delay(RETRY_MS)
        }
    }
}
