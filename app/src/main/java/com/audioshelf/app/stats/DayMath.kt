package com.audioshelf.app.stats

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Calendar days as the statistics see them: a `yyyy-MM-dd` key in the given zone. The only place this arithmetic lives. */
fun dayKey(ms: Long, tz: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    return String.format(Locale.ROOT, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

fun dayStart(ms: Long, tz: TimeZone = TimeZone.getDefault()): Long {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun addDays(ms: Long, days: Int, tz: TimeZone = TimeZone.getDefault()): Long {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    c.add(Calendar.DAY_OF_YEAR, days)
    return c.timeInMillis
}

fun addMonths(ms: Long, months: Int, tz: TimeZone = TimeZone.getDefault()): Long {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    c.add(Calendar.MONTH, months)
    return c.timeInMillis
}

fun monthStart(ms: Long, tz: TimeZone = TimeZone.getDefault()): Long {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = dayStart(ms, tz)
    c.set(Calendar.DAY_OF_MONTH, 1)
    return c.timeInMillis
}

fun hourOf(ms: Long, tz: TimeZone = TimeZone.getDefault()): Int {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    return c.get(Calendar.HOUR_OF_DAY)
}

/** Monday = 0 … Sunday = 6. */
fun weekdayOf(ms: Long, tz: TimeZone = TimeZone.getDefault()): Int {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
}

fun parseDayKey(key: String, tz: TimeZone = TimeZone.getDefault()): Long? {
    val p = key.split('-')
    if (p.size != 3) return null
    val y = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    val d = p[2].toIntOrNull() ?: return null
    val c = Calendar.getInstance(tz)
    c.clear()
    c.set(y, m - 1, d, 0, 0, 0)
    return c.timeInMillis
}

fun daysBetween(from: Long, to: Long, tz: TimeZone = TimeZone.getDefault()): Long {
    var n = 0L
    var at = dayStart(from, tz)
    val end = dayStart(to, tz)
    while (at < end) {
        at = addDays(at, 1, tz)
        n++
    }
    return n
}
