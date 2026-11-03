package com.audioshelf.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    @Test
    fun roundTripSkipsCachesAndDeviceKeys() {
        val a = testKv()
        a.put(KV_STORE, "audioshelf.speed", "1.25")
        a.put(KV_STORE, "audioshelf.folder", "{\"uri\":\"x\"}")
        a.put("lib", "content://x", "cache")
        a.put("audioshelf.notes", "book \"1\"", "line\nbreak ✓")
        val text = Backup.encode(Backup.rows(a), 1L)
        val rows = Backup.decode(text)!!
        assertEquals(2, rows.size)
        val b = testKv()
        Backup.apply(b, rows)
        assertEquals("1.25", b.get(KV_STORE, "audioshelf.speed"))
        assertEquals("line\nbreak ✓", b.get("audioshelf.notes", "book \"1\""))
        assertNull(b.get(KV_STORE, "audioshelf.folder"))
        assertNull(b.get("lib", "content://x"))
    }

    @Test
    fun rejectsForeignFiles() {
        assertNull(Backup.decode("{\"app\":\"other\",\"rows\":[]}"))
        assertNull(Backup.decode("garbage"))
        assertNull(Backup.decode(null))
    }

    @Test
    fun restoreReplacesTheStoresOfTheBackupAndKeepsDeviceKeys() {
        val source = testKv()
        source.put("audioshelf.stats.day", "2023-11-13", "{\"l\":100}")
        val rows = Backup.decode(Backup.encode(Backup.rows(source), 1L))!!

        val target = testKv()
        target.put("audioshelf.stats.day", "2023-11-13", "{\"l\":999}")
        target.put("audioshelf.stats.day", "2023-11-14", "{\"l\":5}")
        target.put(KV_STORE, "audioshelf.folder", "{\"uri\":\"mine\"}")
        Backup.apply(target, rows)

        assertEquals("{\"l\":100}", target.get("audioshelf.stats.day", "2023-11-13"))
        assertNull(target.get("audioshelf.stats.day", "2023-11-14"))
        assertEquals("{\"uri\":\"mine\"}", target.get(KV_STORE, "audioshelf.folder"))
    }
}
