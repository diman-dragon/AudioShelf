package com.audioshelf.app.player

import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.ProgressState
import com.audioshelf.app.library.ProgressStore
import com.audioshelf.app.stats.StatsStore

private const val END_GAP_MS = 3000L

/** Writes where the listener is in the current book, and marks the book finished at its end. */
class ProgressRecorder(
    private val current: CurrentBook,
    private val engine: PlaybackEngine,
    private val progress: ProgressStore,
    private val durations: DurationStore,
    private val stats: StatsStore,
) {
    fun remember(ended: Boolean = false, periodic: Boolean = false) {
        val t = current.get() ?: return
        val s = engine.now()
        if (!current.isLoaded(t, s)) return
        if (s.ended && !ended) return
        val count = t.items.size
        val last = s.index == count - 1
        val done = ended || (last && s.durationMs > 0 && s.positionMs >= s.durationMs - END_GAP_MS)
        val info = bookProgress(durations.get(t.id), count, s.index, s.positionMs)
        val percent = if (done) 100 else info?.percent ?: 0
        val before = progress.state(t.id)
        progress.save(t.id, s.index, if (ended) 0 else s.positionMs, done, percent, t.expanded, notify = !periodic)
        if (done && before != ProgressState.Done) stats.finished(t.id, System.currentTimeMillis())
    }

    /** Where playback picks up when the book is not loaded yet. */
    fun resumeSpot(t: PlayBook): Pair<Int, Long> = progress.resume(t.id)
}
