package com.audioshelf.app.stats

import com.audioshelf.app.data.FlatJson
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.long
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DAYS = "audioshelf.stats.day"
private const val BOOKS = "audioshelf.stats.book"
private const val DAY_BOOKS = "audioshelf.stats.daybook"
private const val SEP = '\u0001'

class DayStat(
    val listenedMs: Long,
    val contentMs: Long,
    val sessions: Int,
    val hours: LongArray,
    val speeds: Map<Int, Long>,
    val longestSessionMs: Long = 0L,
)

class BookStat(val listenedMs: Long, val contentMs: Long, val sessions: Int, val first: Long, val last: Long, val finished: Long)

class Slice(
    val bookId: String,
    val dayKey: String,
    val hour: Int,
    val speedKey: Int,
    val listenedMs: Long,
    val contentMs: Long,
    val newSession: Boolean,
    val now: Long,
    val sessionMs: Long = 0L,
)

fun dayKey(ms: Long, tz: TimeZone = TimeZone.getDefault()): String {
    val c = Calendar.getInstance(tz)
    c.timeInMillis = ms
    return String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
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

class StatsStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    fun add(slice: Slice) {
        val day = FlatJson.parse(kv.get(DAYS, slice.dayKey)) ?: emptyMap()
        val next = LinkedHashMap<String, Any?>(day)
        next.bump("l", slice.listenedMs)
        next.bump("c", slice.contentMs)
        if (slice.newSession) next.bump("n", 1)
        next.bump("h" + slice.hour, slice.listenedMs)
        if (slice.sessionMs > ((next["m"] as? Number)?.toLong() ?: 0L)) next["m"] = slice.sessionMs
        kv.put(DAYS, slice.dayKey, FlatJson.write(next))

        val book = FlatJson.parse(kv.get(BOOKS, slice.bookId)) ?: emptyMap()
        val b = LinkedHashMap<String, Any?>(book)
        b.bump("l", slice.listenedMs)
        b.bump("c", slice.contentMs)
        if (slice.newSession) b.bump("n", 1)
        if ((b["f"] as? Number) == null) b["f"] = slice.now
        b["t"] = slice.now
        kv.put(BOOKS, slice.bookId, FlatJson.write(b))

        val dbKey = slice.dayKey + SEP + slice.bookId
        val db = FlatJson.parse(kv.get(DAY_BOOKS, dbKey)) ?: emptyMap()
        val d = LinkedHashMap<String, Any?>(db)
        d.bump("l", slice.listenedMs)
        kv.put(DAY_BOOKS, dbKey, FlatJson.write(d))
        counter.value++
    }

    fun finished(bookId: String, now: Long) {
        val book = FlatJson.parse(kv.get(BOOKS, bookId)) ?: emptyMap()
        val b = LinkedHashMap<String, Any?>(book)
        b["d"] = now
        kv.put(BOOKS, bookId, FlatJson.write(b))
        counter.value++
    }

    fun days(): Map<String, DayStat> {
        val out = HashMap<String, DayStat>()
        for ((key, text) in kv.all(DAYS)) {
            val m = FlatJson.parse(text) ?: continue
            val hours = LongArray(24) { m.long("h$it") ?: 0L }
            val speeds = HashMap<Int, Long>()
            for ((k, v) in m) {
                if (k.length > 1 && k[0] == 'v') {
                    val speed = k.substring(1).toIntOrNull() ?: continue
                    speeds[speed] = (v as? Number)?.toLong() ?: continue
                }
            }
            out[key] = DayStat(m.long("l") ?: 0L, m.long("c") ?: 0L, (m.long("n") ?: 0L).toInt(), hours, speeds, m.long("m") ?: 0L)
        }
        return out
    }

    fun books(): Map<String, BookStat> {
        val out = HashMap<String, BookStat>()
        for ((id, text) in kv.all(BOOKS)) {
            val m = FlatJson.parse(text) ?: continue
            out[id] = BookStat(
                m.long("l") ?: 0L, m.long("c") ?: 0L, (m.long("n") ?: 0L).toInt(),
                m.long("f") ?: 0L, m.long("t") ?: 0L, m.long("d") ?: 0L,
            )
        }
        return out
    }

    fun book(id: String): BookStat? = books()[id]

    fun dayBooks(): Map<String, Map<String, Long>> {
        val out = HashMap<String, MutableMap<String, Long>>()
        for ((key, text) in kv.all(DAY_BOOKS)) {
            val at = key.indexOf(SEP)
            if (at < 0) continue
            val ms = FlatJson.parse(text)?.long("l") ?: continue
            out.getOrPut(key.substring(0, at)) { HashMap() }[key.substring(at + 1)] = ms
        }
        return out
    }

    fun forget(bookId: String) {
        kv.del(BOOKS, bookId)
        counter.value++
    }

    fun clear() {
        kv.clear(DAYS)
        kv.clear(BOOKS)
        kv.clear(DAY_BOOKS)
        counter.value++
    }

    private fun MutableMap<String, Any?>.bump(key: String, by: Long) {
        this[key] = ((this[key] as? Number)?.toLong() ?: 0L) + by
    }
}
