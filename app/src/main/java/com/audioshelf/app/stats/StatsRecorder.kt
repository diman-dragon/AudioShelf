package com.audioshelf.app.stats

import com.audioshelf.app.AppLog
import java.util.TimeZone

private const val TAG = "StatsRecorder"

/**
 * The longest stretch between two states that still counts as listening. A dark screen ticks every
 * 4 s (see PlaybackEngine), so the limit sits well above that: a late tick must not silently drop time.
 */
const val MAX_GAP_MS = 10_000L
private const val FLUSH_MS = 10_000L
private const val SESSION_GAP_MS = 5 * 60_000L

/** Turns player states into listening time: wall-clock time spent playing, per book, per day, per hour. */
class StatsRecorder(
    private val store: StatsStore,
    private val tz: () -> TimeZone = { TimeZone.getDefault() },
) {
    private var book: String? = null
    private var lastAt = 0L
    private var playing = false
    private var lastEnd = 0L
    private var pendingListened = 0L
    private var pendingSession = false
    private var pendingAt = 0L
    private var sessionOpen = false
    private var sessionMs = 0L

    /** Feed with every player state change; [bookId] is null when nothing of ours is loaded. */
    fun onState(bookId: String?, isPlaying: Boolean, now: Long) {
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
                add(dt, now)
            } else if (dt > MAX_GAP_MS) {
                AppLog.w(TAG, "gap of $dt ms between states is not counted (limit $MAX_GAP_MS ms)")
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
    }

    fun flush(now: Long = System.currentTimeMillis()) {
        val id = book
        if (id == null || (pendingListened <= 0L && !pendingSession)) {
            pendingListened = 0
            return
        }
        val zone = tz()
        val at = if (pendingAt > 0) pendingAt else now
        store.add(Slice(id, dayKey(at, zone), hourOf(at, zone), pendingListened, pendingSession, now, sessionMs))
        pendingListened = 0
        pendingSession = false
        pendingAt = 0
    }

    private fun add(listened: Long, now: Long) {
        val zone = tz()
        if (pendingListened > 0 && (dayKey(pendingAt, zone) != dayKey(now, zone) || hourOf(pendingAt, zone) != hourOf(now, zone))) {
            flush(now)
        }
        if (pendingListened == 0L) pendingAt = now
        pendingListened += listened
        sessionMs += listened
        if (pendingListened >= FLUSH_MS) flush(now)
    }
}
