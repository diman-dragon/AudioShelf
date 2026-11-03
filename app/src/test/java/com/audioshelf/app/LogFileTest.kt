package com.audioshelf.app

import java.io.File
import java.util.Calendar
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LogFileTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private var now = at(2026, 11, 2, 23, 59)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis

    private fun log(keepDays: Int = 14, maxDayBytes: Long = 1_000_000L) =
        LogFile(File(tmp.root, "logs"), keepDays, maxDayBytes) { now }

    private fun zipText(zip: File, name: String) = ZipFile(zip).use { z -> z.getInputStream(z.getEntry(name)).reader().readText() }

    @Test
    fun eachDayGetsItsOwnFile() {
        val log = log()
        log.append(4, "Play", "late on the 2nd")
        now = at(2026, 11, 3, 0, 1)
        log.append(5, "Stutter", "early on the 3rd")
        now = at(2026, 11, 4, 8, 0)
        log.append(4, "Play", "on the 4th")
        assertEquals(listOf("2026-11-02", "2026-11-03", "2026-11-04"), log.days())
        val files = File(tmp.root, "logs")
        assertTrue("late on the 2nd" in File(files, "audioshelf-2026-11-02.log").readText())
        assertFalse("early on the 3rd" in File(files, "audioshelf-2026-11-02.log").readText())
        assertTrue("early on the 3rd" in File(files, "audioshelf-2026-11-03.log").readText())
        assertTrue("on the 4th" in File(files, "audioshelf-2026-11-04.log").readText())
    }

    @Test
    fun aLineHasTimeLevelTagAndThread() {
        val log = log()
        log.append(5, "Stutter", "UNDERRUN")
        val line = File(tmp.root, "logs/audioshelf-2026-11-02.log").also { log.flush() }.readText()
        assertTrue(Regex("^23:59:00\\.000 W/Stutter \\[.+] UNDERRUN\n$").matches(line))
    }

    @Test
    fun exportIsAZipWithTheHeaderAndEveryDay() {
        val log = log()
        log.append(4, "T", "first day")
        now = at(2026, 11, 3, 12, 0)
        log.append(4, "T", "second day")
        val zip = File(tmp.root, "out.zip")
        assertTrue(log.export(zip, "HEADER"))
        assertEquals("HEADER", zipText(zip, "device.txt"))
        assertTrue("first day" in zipText(zip, "audioshelf-2026-11-02.log"))
        assertTrue("second day" in zipText(zip, "audioshelf-2026-11-03.log"))
    }

    @Test
    fun onlyTheLastDaysAreKept() {
        val log = log(keepDays = 2)
        for (day in 1..4) {
            now = at(2026, 11, day, 10, 0)
            log.append(4, "T", "day $day")
        }
        assertEquals(listOf("2026-11-03", "2026-11-04"), log.days())
    }

    @Test
    fun aDayStopsGrowingAtTheLimit() {
        val log = log(maxDayBytes = 300)
        repeat(50) { log.append(4, "T", "line number $it with some padding") }
        log.flush()
        val text = File(tmp.root, "logs/audioshelf-2026-11-02.log").readText()
        assertTrue("size limit" in text)
        assertFalse("line number 49" in text)
    }

    @Test
    fun clearRemovesEveryDay() {
        val log = log()
        log.append(4, "T", "something")
        now = at(2026, 11, 3, 1, 0)
        log.append(4, "T", "more")
        log.clear()
        assertEquals(emptyList<String>(), log.days())
        log.append(4, "T", "after")
        assertEquals(listOf("2026-11-03"), log.days())
    }

    @Test
    fun errorsCarryTheirStackTrace() {
        val seen = ArrayList<String>()
        val original = AppLog.sink
        AppLog.sink = AppLog.Sink { _, _, message -> seen.add(message) }
        try {
            AppLog.e("T", "failed", IllegalStateException("boom"))
        } finally {
            AppLog.sink = original
        }
        assertTrue("IllegalStateException: boom" in seen.single())
    }
}
