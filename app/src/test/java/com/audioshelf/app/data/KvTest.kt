package com.audioshelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
