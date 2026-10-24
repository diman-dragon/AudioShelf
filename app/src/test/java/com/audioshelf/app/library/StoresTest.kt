package com.audioshelf.app.library

import com.audioshelf.app.data.HiddenStore
import com.audioshelf.app.data.MemoryBackend
import com.audioshelf.app.data.testKv
import com.audioshelf.app.player.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoresTest {
    @Test
    fun metaRoundTripAndMove() {
        val store = MetaStore(testKv())
        store.set("a", Meta(Kind.Album, "T", "A", "N", "1999"))
        assertEquals(Meta(Kind.Album, "T", "A", "N", "1999"), store.get("a"))
        store.move("a", "b")
        assertEquals("T", store.get("b").title)
        assertEquals(Meta(), store.get("a"))
    }

    @Test
    fun metaReadsOldAppRows() {
        val backend = MemoryBackend()
        backend.table["audioshelf.meta" to "x"] = """{"kind":"book","title":"T","author":"","narrator":"N","year":""}"""
        val meta = MetaStore(testKv(backend)).get("x")
        assertEquals(Kind.Book, meta.kind)
        assertEquals("N", meta.narrator)
    }

    @Test
    fun progressStates() {
        val p = ProgressStore(testKv())
        assertEquals(ProgressState.New, p.state("b"))
        assertNull(p.percent("b"))
        p.save("b", 0, 5000, false, 12)
        assertEquals(ProgressState.Started, p.state("b"))
        assertEquals(12, p.percent("b"))
        assertEquals(0 to 0L, p.resume("b"))
        p.save("b", 2, 60_000, false, 100)
        assertEquals(99, p.percent("b"))
        assertEquals(2 to 55_000L, p.resume("b"))
        p.save("b", 3, 100, true, 100)
        assertEquals(ProgressState.Done, p.state("b"))
        assertEquals(100, p.percent("b"))
        assertEquals(0 to 0L, p.resume("b"))
    }

    @Test
    fun progressReadsOldAppRows() {
        val backend = MemoryBackend()
        backend.table["pos" to "x"] = """{"i":2,"p":90000,"t":1700000000000}"""
        backend.table["pinfo" to "x"] = """{"done":false,"x":1,"pc":40}"""
        val p = ProgressStore(testKv(backend))
        assertEquals(40, p.percent("x"))
        assertEquals(1700000000000L, p.lastPlayed("x"))
        assertEquals(2 to 85_000L, p.resume("x"))
    }

    @Test
    fun progressMoveAndForget() {
        val p = ProgressStore(testKv())
        p.save("a", 1, 10_000, false, 5)
        p.move("a", "b")
        assertEquals(ProgressState.New, p.state("a"))
        assertEquals(ProgressState.Started, p.state("b"))
        p.forget("b")
        assertEquals(ProgressState.New, p.state("b"))
    }

    @Test
    fun durationsPersistAndMove() {
        val kv = testKv()
        val d = DurationStore(kv)
        d.note("a", 0, 1000)
        d.note("a", 1, 2000)
        assertEquals(mapOf(0 to 1000L, 1 to 2000L), d.get("a"))
        assertEquals(mapOf(0 to 1000L, 1 to 2000L), DurationStore(kv).get("a"))
        d.move("a", "b")
        assertTrue(d.get("a").isEmpty())
        assertEquals(2000L, d.get("b")[1])
        d.forget("b")
        assertTrue(d.get("b").isEmpty())
    }

    @Test
    fun hiddenStore() {
        val h = HiddenStore(testKv())
        h.hide("a")
        h.hide("b")
        assertTrue(h.isHidden("a"))
        assertEquals(2, h.count())
        h.move("a", "c")
        assertFalse(h.isHidden("a"))
        assertTrue(h.isHidden("c"))
        h.restoreAll()
        assertEquals(0, h.count())
    }

    @Test
    fun libraryCacheKeepsOrderAndDropsStaleRows() {
        val cache = LibraryCache(testKv())
        val files = listOf(file("B", "2.mp3"), file("A", "1.mp3"), file("", "z.mp3"))
        cache.save("tree", files)
        assertEquals(files, cache.load("tree"))
        assertNull(cache.load("other"))
        cache.save("tree", files.take(1))
        assertEquals(files.take(1), cache.load("tree"))
    }
}
