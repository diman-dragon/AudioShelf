package com.audioshelf.app.stats

import com.audioshelf.app.data.testKv
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val t0 = 1_700_000_000_000L

    private fun recorder(store: StatsStore) = StatsRecorder(store) { utc }

    private fun play(r: StatsRecorder, speed: Float, ticks: Int, step: Long = 250, id: String = "a", jump: Boolean = false) {
        var pos = 0L
        for (i in 0..ticks) {
            if (jump && i == ticks / 2) pos += 600_000
            r.onState(id, true, pos, speed, t0 + i * step)
            pos += (step * speed).toLong()
        }
        r.onState(id, false, pos, speed, t0 + (ticks + 1) * step)
    }

    @Test
    fun countsListenedAndContentTime() {
        val store = StatsStore(testKv())
        play(recorder(store), 1.5f, 80)
        val day = store.days().values.single()
        assertEquals(20_000L, day.listenedMs)
        assertEquals(30_000L, day.contentMs)
        assertEquals(1, day.sessions)
        assertEquals(20_000L, day.speeds[150])
        assertEquals(20_000L, day.hours.sum())
        assertEquals(20_000L, store.book("a")!!.listenedMs)
    }

    @Test
    fun seekJumpIsNotListening() {
        val store = StatsStore(testKv())
        play(recorder(store), 1f, 40, jump = true)
        val day = store.days().values.single()
        assertEquals(10_000L, day.listenedMs)
        assertTrue(day.contentMs < 12_000L)
    }

    @Test
    fun pausedTimeIsNotCounted() {
        val store = StatsStore(testKv())
        val r = recorder(store)
        r.onState("a", true, 0, 1f, t0)
        r.onState("a", true, 250, 1f, t0 + 250)
        r.onState("a", false, 250, 1f, t0 + 500)
        r.onState("a", false, 250, 1f, t0 + 3_600_000)
        r.onState("a", true, 250, 1f, t0 + 3_600_250)
        r.onState("a", true, 500, 1f, t0 + 3_600_500)
        r.onState("a", false, 500, 1f, t0 + 3_600_750)
        val day = store.days().values.single()
        assertEquals(500L, day.listenedMs)
    }

    @Test
    fun switchingBooksSplitsTime() {
        val store = StatsStore(testKv())
        val r = recorder(store)
        r.onState("a", true, 0, 1f, t0)
        r.onState("a", true, 250, 1f, t0 + 250)
        r.onState("b", true, 0, 1f, t0 + 500)
        r.onState("b", true, 250, 1f, t0 + 750)
        r.onState("b", false, 250, 1f, t0 + 1000)
        assertEquals(250L, store.book("a")!!.listenedMs)
        assertEquals(250L, store.book("b")!!.listenedMs)
    }

    private fun slice(store: StatsStore, daysAgo: Int, id: String = "a", ms: Long = 120_000) {
        val at = t0 - daysAgo * 86_400_000L
        store.add(Slice(id, dayKey(at, utc), 20, 100, ms, ms, true, at))
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
}
