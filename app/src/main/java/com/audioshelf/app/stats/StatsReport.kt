package com.audioshelf.app.stats

import java.util.TimeZone

const val ACTIVE_DAY_MS = 60_000L
private const val TOP_COUNT = 8
private const val HEAT_WEEKS = 13
private const val ALL_MONTHS = 24

enum class StatsRange(val id: String, val days: Int) {
    Week("week", 7), Month("month", 30), Year("year", 365), All("all", 0);

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: Week
    }
}

class BarPoint(val start: Long, val ms: Long)

class HeatCell(val start: Long, val ms: Long)

class TopBook(val id: String, val ms: Long)

class StatsReport(
    val range: StatsRange,
    val listenedMs: Long,
    val sessions: Int,
    val periodDays: Int,
    val finishedBooks: Int,
    val streak: Int,
    val bestStreak: Int,
    val bars: List<BarPoint>,
    val monthly: Boolean,
    val hours: LongArray,
    val top: List<TopBook>,
    val heat: List<HeatCell>,
    val allTimeMs: Long,
    val weekMs: Long,
    val monthMs: Long,
    val weekdays: LongArray,
    val longestSessionMs: Long,
    val perBook: Map<String, Long>,
) {
    val avgSessionMs: Long get() = if (sessions > 0) listenedMs / sessions else 0

    val avgPerDayMs: Long get() = if (periodDays > 0) listenedMs / periodDays else 0

    val empty: Boolean get() = allTimeMs <= 0L

    companion object {
        fun build(
            days: Map<String, DayStat>,
            books: Map<String, BookStat>,
            dayBooks: Map<String, Map<String, Long>>,
            range: StatsRange,
            now: Long,
            tz: TimeZone = TimeZone.getDefault(),
        ): StatsReport {
            val today = dayStart(now, tz)
            val todayKey = dayKey(today, tz)
            val firstKey = days.keys.minOrNull()
            val from = when (range) {
                StatsRange.All -> firstKey?.let { parseDayKey(it, tz) } ?: today
                else -> addDays(today, -(range.days - 1), tz)
            }
            val fromKey = dayKey(from, tz)
            fun inRange(key: String) = key >= fromKey && key <= todayKey

            var listened = 0L
            var sessions = 0
            val hours = LongArray(24)
            val weekdays = LongArray(7)
            var longest = 0L
            for ((key, d) in days) {
                if (!inRange(key)) continue
                listened += d.listenedMs
                sessions += d.sessions
                for (h in 0 until 24) hours[h] += d.hours[h]
                if (d.longestSessionMs > longest) longest = d.longestSessionMs
                parseDayKey(key, tz)?.let { weekdays[weekdayOf(it, tz)] += d.listenedMs }
            }
            val spanDays = daysBetween(from, today, tz) + 1

            val topMap = HashMap<String, Long>()
            for ((key, per) in dayBooks) {
                if (!inRange(key)) continue
                for ((id, ms) in per) topMap[id] = (topMap[id] ?: 0L) + ms
            }
            val top = topMap.entries.sortedByDescending { it.value }.take(TOP_COUNT).map { TopBook(it.key, it.value) }

            val finished = books.values.count { it.finished in from..(today + 86_400_000L) }

            val monthly = range == StatsRange.Year || range == StatsRange.All
            val bars = if (monthly) monthBars(days, from, today, range, tz) else dayBars(days, from, today, tz)
            val (streak, best) = streaks(days, today, todayKey, tz)

            fun lastDays(n: Int): Long {
                val a = dayKey(addDays(today, -(n - 1), tz), tz)
                return days.filter { it.key >= a && it.key <= todayKey }.values.sumOf { it.listenedMs }
            }

            return StatsReport(
                range, listened, sessions, spanDays.toInt(), finished, streak, best,
                bars, monthly, hours,
                top, heat(days, today, tz), days.values.sumOf { it.listenedMs },
                lastDays(7), lastDays(30), weekdays, longest, topMap,
            )
        }

        private fun dayBars(days: Map<String, DayStat>, from: Long, today: Long, tz: TimeZone): List<BarPoint> {
            val out = ArrayList<BarPoint>()
            var at = from
            while (at <= today) {
                out.add(BarPoint(at, days[dayKey(at, tz)]?.listenedMs ?: 0L))
                at = addDays(at, 1, tz)
            }
            return out
        }

        private fun monthBars(days: Map<String, DayStat>, from: Long, today: Long, range: StatsRange, tz: TimeZone): List<BarPoint> {
            val last = monthStart(today, tz)
            val first = if (range == StatsRange.Year) addMonths(last, -11, tz) else maxOf(monthStart(from, tz), addMonths(last, -(ALL_MONTHS - 1), tz))
            val out = ArrayList<BarPoint>()
            var at = first
            while (at <= last) {
                val next = addMonths(at, 1, tz)
                val a = dayKey(at, tz)
                val b = dayKey(next, tz)
                var sum = 0L
                for ((key, d) in days) if (key >= a && key < b) sum += d.listenedMs
                out.add(BarPoint(at, sum))
                at = next
            }
            return out
        }

        private fun heat(days: Map<String, DayStat>, today: Long, tz: TimeZone): List<HeatCell> {
            val weekday = weekdayOf(today, tz)
            val start = addDays(today, -(weekday + (HEAT_WEEKS - 1) * 7), tz)
            val out = ArrayList<HeatCell>(HEAT_WEEKS * 7)
            var at = start
            repeat(HEAT_WEEKS * 7) {
                out.add(HeatCell(at, if (at > today) -1L else days[dayKey(at, tz)]?.listenedMs ?: 0L))
                at = addDays(at, 1, tz)
            }
            return out
        }

        private fun streaks(days: Map<String, DayStat>, today: Long, todayKey: String, tz: TimeZone): Pair<Int, Int> {
            val activeKeys = days.filter { it.value.listenedMs >= ACTIVE_DAY_MS }.keys.sorted()
            var best = 0
            var run = 0
            var prev: Long? = null
            for (key in activeKeys) {
                val at = parseDayKey(key, tz) ?: continue
                run = if (prev != null && addDays(prev, 1, tz) == at) run + 1 else 1
                if (run > best) best = run
                prev = at
            }
            val set = activeKeys.toHashSet()
            var cursor = today
            if (todayKey !in set) cursor = addDays(today, -1, tz)
            var current = 0
            while (dayKey(cursor, tz) in set) {
                current++
                cursor = addDays(cursor, -1, tz)
            }
            return current to best
        }
    }
}
