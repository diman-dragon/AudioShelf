package com.audioshelf.app.stats

import com.audioshelf.app.AppLog
import com.audioshelf.app.data.testKv
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val t0 = 1_700_000_000_000L
    private val logged = ArrayList<String>()
    private val original = AppLog.sink

    @Before
    fun captureLog() {
        AppLog.sink = AppLog.Sink { _, _, message -> logged.add(message) }
    }

    @After
    fun restoreLog() {
        AppLog.sink = original
    }

    private fun recorder(store: StatsStore) = StatsRecorder(store) { utc }

    private fun play(r: StatsRecorder, ticks: Int, step: Long = 250, id: String = "a") {
        for (i in 0..ticks) r.onState(id, true, t0 + i * step)
        r.onState(id, false, t0 + (ticks + 1) * step)
    }

    @Test
    fun countsListenedTimeOfOneSession() {
        val store = StatsStore(testKv())
        play(recorder(store), 80)
        val day = store.days().values.single()
        assertEquals(20_000L, day.listenedMs)
        assertEquals(1, day.sessions)
        assertEquals(20_000L, day.longestSessionMs)
        assertEquals(20_000L, day.hours.sum())
        assertEquals(20_000L, store.book("a")!!.listenedMs)
    }

    @Test
    fun pausedTimeIsNotCounted() {
        val store = StatsStore(testKv())
        val r = recorder(store)
        r.onState("a", true, t0)
        r.onState("a", true, t0 + 250)
        r.onState("a", false, t0 + 500)
        r.onState("a", false, t0 + 3_600_000)
        r.onState("a", true, t0 + 3_600_250)
        r.onState("a", true, t0 + 3_600_500)
        r.onState("a", false, t0 + 3_600_750)
        assertEquals(500L, store.days().values.single().listenedMs)
    }

    @Test
    fun slowDarkScreenTicksAreCountedDespiteJitter() {
        val store = StatsStore(testKv())
        val r = recorder(store)
        var at = t0
        r.onState("a", true, at)
        // A dark screen ticks every 4 s; Doze and load make some of them late.
        for (late in listOf(0L, 1_500L, 3_000L, 0L, 2_000L)) {
            at += 4_000L + late
            r.onState("a", true, at)
        }
        r.onState("a", false, at)
        assertEquals(at - t0, store.days().values.single().listenedMs)
    }

    @Test
    fun aHugeGapIsNotCountedAndIsLogged() {
        val store = StatsStore(testKv())
        val r = recorder(store)
        r.onState("a", true, t0)
        r.onState("a", true, t0 + 250)
        r.onState("a", true, t0 + 250 + 10 * MAX_GAP_MS)
        r.onState("a", false, t0 + 250 + 10 * MAX_GAP_MS + 250)
        assertEquals(500L, store.days().values.single().listenedMs)
        assertTrue(logged.any { "not counted" in it })
    }

    @Test
    fun switchingBooksSplitsTime() {
        val store = StatsStore(testKv())
        val r = recorder(store)
        r.onState("a", true, t0)
        r.onState("a", true, t0 + 250)
        r.onState("b", true, t0 + 500)
        r.onState("b", true, t0 + 750)
        r.onState("b", false, t0 + 1000)
        assertEquals(250L, store.book("a")!!.listenedMs)
        assertEquals(250L, store.book("b")!!.listenedMs)
    }

    private fun slice(store: StatsStore, daysAgo: Int, id: String = "a", ms: Long = 120_000) {
        val at = t0 - daysAgo * 86_400_000L
        store.add(Slice(id, dayKey(at, utc), 20, ms, true, at))
    }

    @Test
    fun reportStreaksBarsAndTop() {
        val store = StatsStore(testKv())
        listOf(0, 1, 2, 5, 6, 7, 8, 9).forEach { slice(store, it) }
        slice(store, 1, "b", 700_000)
        store.finished("b", t0 - 86_400_000L)
        val week = StatsReport.build(store.days(), store.books(), store.dayBooks(), StatsRange.Week, t0, utc)
        assertEquals(3, week.streak)
        assertEquals(5, week.bestStreak)
        assertEquals(7, week.bars.size)
        assertEquals(91, week.heat.size)
        assertEquals("b", week.top.first().id)
        assertEquals(1, week.finishedBooks)
        assertEquals(5 * 120_000L + 700_000L, week.listenedMs)
        val year = StatsReport.build(store.days(), store.books(), store.dayBooks(), StatsRange.Year, t0, utc)
        assertEquals(12, year.bars.size)
        assertTrue(year.monthly)
    }

    @Test
    fun emptyReportIsEmpty() {
        val store = StatsStore(testKv())
        val r = StatsReport.build(store.days(), store.books(), store.dayBooks(), StatsRange.All, t0, utc)
        assertTrue(r.empty)
        assertEquals(0, r.streak)
    }

    @Test
    fun weekdayIndexStartsOnMonday() {
        val monday = parseDayKey("2023-11-13", utc)!!
        assertEquals(0, weekdayOf(monday, utc))
        assertEquals(6, weekdayOf(addDays(monday, 6, utc), utc))
    }
}
