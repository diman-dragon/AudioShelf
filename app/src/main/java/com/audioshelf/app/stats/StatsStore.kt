package com.audioshelf.app.stats

import com.audioshelf.app.data.FlatJson
import com.audioshelf.app.data.Kv
import com.audioshelf.app.data.long
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DAYS = "audioshelf.stats.day"
private const val BOOKS = "audioshelf.stats.book"
private const val DAY_BOOKS = "audioshelf.stats.daybook"
private const val SEP = '\u0001'

class DayStat(
    val listenedMs: Long,
    val sessions: Int,
    val hours: LongArray,
    val longestSessionMs: Long = 0L,
)

class BookStat(val listenedMs: Long, val sessions: Int, val first: Long, val last: Long, val finished: Long)

class Slice(
    val bookId: String,
    val dayKey: String,
    val hour: Int,
    val listenedMs: Long,
    val newSession: Boolean,
    val now: Long,
    val sessionMs: Long = 0L,
)

/**
 * Listening time per day, per book and per day-and-book. Rows saved by older versions may carry extra
 * fields (content time, speeds); they are kept as they are and ignored.
 */
class StatsStore(private val kv: Kv) {
    private val counter = MutableStateFlow(0)

    val version: StateFlow<Int> = counter.asStateFlow()

    @Synchronized
    fun add(slice: Slice) {
        val day = LinkedHashMap<String, Any?>(FlatJson.parse(kv.get(DAYS, slice.dayKey)) ?: emptyMap())
        day.bump("l", slice.listenedMs)
        if (slice.newSession) day.bump("n", 1)
        day.bump("h" + slice.hour, slice.listenedMs)
        if (slice.sessionMs > (day.long("m") ?: 0L)) day["m"] = slice.sessionMs
        kv.put(DAYS, slice.dayKey, FlatJson.write(day))

        val book = LinkedHashMap<String, Any?>(FlatJson.parse(kv.get(BOOKS, slice.bookId)) ?: emptyMap())
        book.bump("l", slice.listenedMs)
        if (slice.newSession) book.bump("n", 1)
        if (book.long("f") == null) book["f"] = slice.now
        book["t"] = slice.now
        kv.put(BOOKS, slice.bookId, FlatJson.write(book))

        val dayBookKey = slice.dayKey + SEP + slice.bookId
        val dayBook = LinkedHashMap<String, Any?>(FlatJson.parse(kv.get(DAY_BOOKS, dayBookKey)) ?: emptyMap())
        dayBook.bump("l", slice.listenedMs)
        kv.put(DAY_BOOKS, dayBookKey, FlatJson.write(dayBook))
        counter.value++
    }

    @Synchronized
    fun finished(bookId: String, now: Long) {
        val book = LinkedHashMap<String, Any?>(FlatJson.parse(kv.get(BOOKS, bookId)) ?: emptyMap())
        book["d"] = now
        kv.put(BOOKS, bookId, FlatJson.write(book))
        counter.value++
    }

    fun days(): Map<String, DayStat> {
        val out = HashMap<String, DayStat>()
        for ((key, text) in kv.all(DAYS)) {
            val m = FlatJson.parse(text) ?: continue
            val hours = LongArray(24) { m.long("h$it") ?: 0L }
            out[key] = DayStat(m.long("l") ?: 0L, (m.long("n") ?: 0L).toInt(), hours, m.long("m") ?: 0L)
        }
        return out
    }

    fun books(): Map<String, BookStat> {
        val out = HashMap<String, BookStat>()
        for ((id, text) in kv.all(BOOKS)) {
            val m = FlatJson.parse(text) ?: continue
            out[id] = BookStat(m.long("l") ?: 0L, (m.long("n") ?: 0L).toInt(), m.long("f") ?: 0L, m.long("t") ?: 0L, m.long("d") ?: 0L)
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

    private fun MutableMap<String, Any?>.bump(key: String, by: Long) {
        this[key] = (long(key) ?: 0L) + by
    }
}
