package com.audioshelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.CountDownLatch

class KvTest {
    @Test
    fun loadsExistingRows() {
        val kv = testKv(MemoryBackend(listOf(KvRow("kv", "a", "1"))))
        assertEquals("1", kv.get("kv", "a"))
    }

    @Test
    fun writesReachBackendOnFlush() {
        val backend = MemoryBackend()
        val kv = testKv(backend)
        kv.put("s", "k", "v")
        kv.flushNow()
        assertEquals("v", backend.table["s" to "k"])
    }

    @Test
    fun unchangedValuesAreNotQueued() {
        val backend = MemoryBackend()
        val kv = testKv(backend)
        kv.put("s", "k", "v")
        kv.flushNow()
        kv.put("s", "k", "v")
        kv.flushNow()
        assertEquals(1, backend.writes)
    }

    @Test
    fun deleteAndClear() {
        val backend = MemoryBackend()
        val kv = testKv(backend)
        kv.put("s", "a", "1")
        kv.put("s", "b", "2")
        kv.put("t", "c", "3")
        kv.flushNow()
        kv.del("s", "a")
        kv.clear("t")
        kv.flushNow()
        assertNull(backend.table["s" to "a"])
        assertEquals("2", backend.table["s" to "b"])
        assertNull(backend.table["t" to "c"])
        assertEquals(1, kv.size("s"))
    }

    @Test
    fun coalesceKeepsLastOpPerKey() {
        val ops = coalesce(
            listOf(
                KvOp.Put("s", "k", "1"),
                KvOp.Put("s", "k", "2"),
                KvOp.Put("s", "x", "9"),
                KvOp.Clear("s"),
                KvOp.Put("s", "k", "3"),
            ),
        )
        assertEquals(2, ops.size)
        assertEquals(true, ops[0] is KvOp.Clear)
        assertEquals("3", (ops[1] as KvOp.Put).value)
    }

    @Test
    fun failedBatchStaysQueuedAndNewerValueWins() {
        val backend = MemoryBackend()
        val kv = testKv(backend)
        kv.put("s", "k", "old")
        backend.failing = true
        kv.flushNow()
        assertNull(backend.table["s" to "k"])
        kv.put("s", "k", "new")
        backend.failing = false
        kv.flushNow()
        assertEquals("new", backend.table["s" to "k"])
    }

    @Test
    fun flushWithNothingQueuedWritesNothing() {
        val backend = MemoryBackend()
        val kv = testKv(backend)
        kv.flushNow()
        assertEquals(0, backend.writes)
    }

    @Test
    fun batchIsVisibleAndWrittenAsOneTransaction() {
        val backend = MemoryBackend(listOf(KvRow("s", "old", "value"), KvRow("other", "keep", "value")))
        val kv = testKv(backend)
        kv.batch(listOf(KvOp.Clear("s"), KvOp.Put("s", "new", "value")))

        assertEquals(mapOf("new" to "value"), kv.snapshot()["s"])
        kv.flushNow()

        assertEquals(1, backend.writes)
        assertNull(backend.table["s" to "old"])
        assertEquals("value", backend.table["s" to "new"])
        assertEquals("value", backend.table["other" to "keep"])
    }

    @Test
    fun concurrentWritesKeepMemoryAndDiskInTheSameOrder() {
        val backend = MemoryBackend()
        val kv = testKv(backend)
        val start = CountDownLatch(1)
        val writers = List(2) { writer ->
            Thread {
                start.await()
                repeat(2_000) { kv.put("s", "k", "$writer:$it") }
            }.apply { start() }
        }

        start.countDown()
        writers.forEach { it.join() }
        kv.flushNow()

        assertEquals(kv.get("s", "k"), backend.table["s" to "k"])
    }
}
