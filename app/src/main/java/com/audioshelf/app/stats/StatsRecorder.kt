package com.audioshelf.app.stats

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

private const val MAX_GAP_MS = 5000L
private const val FLUSH_MS = 10_000L
private const val SESSION_GAP_MS = 5 * 60_000L

class StatsRecorder(
    private val store: StatsStore,
    private val tz: () -> TimeZone = { TimeZone.getDefault() },
) {
    private var book: String? = null
    private var lastAt = 0L
    private var lastPosition = 0L
    private var playing = false
    private var lastEnd = 0L
    private var pendingListened = 0L
    private var pendingContent = 0L
    private var pendingSession = false
    private var pendingSpeed = 100
    private var pendingAt = 0L
    private var sessionOpen = false
    private var sessionMs = 0L

    /** Feed with every player state change; [bookId] is null when nothing of ours is loaded. */
    fun onState(bookId: String?, isPlaying: Boolean, positionMs: Long, speed: Float, now: Long) {
        val active = bookId != null && isPlaying
        if (bookId != book) {
            flush(now)
            book = bookId
            sessionOpen = false
            playing = false
        }
        if (playing && active) {
            val dt = now - lastAt
            if (dt in 1..MAX_GAP_MS) {
                val advance = positionMs - lastPosition
                val expected = (dt * speed).toLong()
                val content = if (advance in 0..(expected * 3 / 2 + 300)) advance else expected
                add(bookId!!, dt, content, speed, now)
            }
        }
        if (!playing && active) {
            if (!sessionOpen || now - lastEnd > SESSION_GAP_MS) {
                pendingSession = true
                sessionOpen = true
                sessionMs = 0L
            }
        }
        if (playing && !active) {
            lastEnd = now
            flush(now)
        }
        playing = active
        lastAt = now
        lastPosition = positionMs
    }

    fun flush(now: Long = System.currentTimeMillis()) {
        val id = book
        if (id == null || (pendingListened <= 0L && !pendingSession)) {
            pendingListened = 0
            pendingContent = 0
            return
        }
        val zone = tz()
        val at = if (pendingAt > 0) pendingAt else now
        val cal = Calendar.getInstance(zone)
        cal.timeInMillis = at
        store.add(
            Slice(id, dayKey(at, zone), cal.get(Calendar.HOUR_OF_DAY), pendingSpeed, pendingListened, pendingContent, pendingSession, now, sessionMs),
        )
        pendingListened = 0
        pendingContent = 0
        pendingSession = false
        pendingAt = 0
    }

    private fun add(id: String, listened: Long, content: Long, speed: Float, now: Long) {
        val key = (speed * 100).roundToInt()
        val zone = tz()
        if (pendingListened > 0 && (key != pendingSpeed || dayKey(pendingAt, zone) != dayKey(now, zone) || hourOf(pendingAt, zone) != hourOf(now, zone))) {
            flush(now)
            book = id
        }
        if (pendingListened == 0L) {
            pendingAt = now
            pendingSpeed = key
        }
        pendingListened += listened
        sessionMs += listened
        pendingContent += content
        if (pendingListened >= FLUSH_MS) flush(now)
    }

    private fun hourOf(ms: Long, zone: TimeZone): Int {
        val c = Calendar.getInstance(zone)
        c.timeInMillis = ms
        return c.get(Calendar.HOUR_OF_DAY)
    }
}
