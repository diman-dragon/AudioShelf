package com.audioshelf.app.player

import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.stats.StatsRecorder

/** Watches every engine state: counts listening time, learns lengths, notices the end of a book, keeps the screen current. */
class EngineObserver(
    private val current: CurrentBook,
    private val durations: DurationStore,
    private val statsRecorder: StatsRecorder,
    private val recorder: ProgressRecorder,
    private val advancer: BookAdvancer,
    private val sleep: SleepState,
    private val publisher: PlayerUiPublisher,
) {
    private var wasEnded = false

    fun onEngine(s: PlayState) {
        val t = current.get()?.takeIf { current.isLoaded(it, s) }
        statsRecorder.onState(t?.id, s.playing, s.positionMs, s.speed, System.currentTimeMillis())
        if (t != null) {
            if (s.durationMs > 0) durations.note(t.id, s.index, s.durationMs)
            if (s.ended && !wasEnded) {
                recorder.remember(ended = true)
                advancer.advance(t)
            }
        }
        wasEnded = s.ended
        sleep.sync(s)
        publisher.onEngine(s)
    }
}
