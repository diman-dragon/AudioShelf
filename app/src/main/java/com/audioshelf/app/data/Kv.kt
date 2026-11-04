package com.audioshelf.app.data

import com.audioshelf.app.AppLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Kv"
private const val FLUSH_MS = 250L
private const val RETRY_MS = 1000L
private const val LOG_EVERY_FAILURE = 10
private const val SLOW_MS = 50L
private const val SUMMARY_NS = 60_000_000_000L

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

    fun snapshot(): Map<String, Map<String, String>> = synchronized(buckets) {
        buckets.mapValues { (_, rows) -> LinkedHashMap(rows) }
    }

    fun put(store: String, key: String, value: String) = batch(listOf(KvOp.Put(store, key, value)))

    fun del(store: String, key: String) = batch(listOf(KvOp.Del(store, key)))

    fun clear(store: String) = batch(listOf(KvOp.Clear(store)))

    /**
     * Applies related changes to the in-memory view and write queue under the same locks.
     * The backend writes the resulting batch in one transaction.
     */
    fun batch(ops: List<KvOp>) {
        if (ops.isEmpty()) return
        val changed = ArrayList<KvOp>(ops.size)
        synchronized(pending) {
            synchronized(buckets) {
                for (op in ops) {
                    when (op) {
                        is KvOp.Put -> {
                            val rows = bucket(op.store)
                            if (rows[op.key] != op.value) {
                                rows[op.key] = op.value
                                changed.add(op)
                            }
                        }
                        is KvOp.Del -> {
                            if (bucket(op.store).remove(op.key) != null) changed.add(op)
                        }
                        is KvOp.Clear -> {
                            bucket(op.store).clear()
                            changed.add(op)
                        }
                    }
                }
            }
            pending.addAll(changed)
        }
        if (changed.isNotEmpty()) ensureFlusher()
    }

    /** Writes everything queued so far, on the calling thread. A failed batch stays queued and is retried by the timer. */
    fun flushNow() {
        val start = System.nanoTime()
        val result = drain()
        val ms = (System.nanoTime() - start) / 1_000_000
        if (ms >= SLOW_MS) AppLog.w(TAG, "flushNow held ${Thread.currentThread().name} for $ms ms")
        if (result == Drain.Failed) ensureFlusher()
    }

    // The quick writes (the position, every few seconds) are counted and logged as one line a minute; the slow ones are logged each.
    private var statSince = 0L
    private var statBatches = 0
    private var statOps = 0
    private var statMaxMs = 0L

    /** Called under [writeLock], so the counters need no lock of their own. */
    private fun noteWrite(opCount: Int, ms: Long) {
        val now = System.nanoTime()
        if (statBatches == 0) statSince = now
        statBatches++
        statOps += opCount
        if (ms > statMaxMs) statMaxMs = ms
        if (now - statSince >= SUMMARY_NS) {
            AppLog.d(TAG, "wrote $statOps ops in $statBatches batches over ${(now - statSince) / 1_000_000_000L} s, slowest $statMaxMs ms")
            statBatches = 0
            statOps = 0
            statMaxMs = 0
        }
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
                if (ms >= SLOW_MS) AppLog.w(TAG, "slow write: ${ops.size} ops took $ms ms") else noteWrite(ops.size, ms)
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
