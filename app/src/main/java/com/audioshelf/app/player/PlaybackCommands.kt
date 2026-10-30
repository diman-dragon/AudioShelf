package com.audioshelf.app.player

import com.audioshelf.app.data.Prefs
import com.audioshelf.app.library.DurationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The player's buttons. When the book is not loaded yet a command loads it first, from the saved spot. */
class PlaybackCommands(
    private val scope: CoroutineScope,
    private val engine: PlaybackEngine,
    private val current: CurrentBook,
    private val recorder: ProgressRecorder,
    private val durations: DurationStore,
    private val prefs: Prefs,
    private val playPrefs: PlayPrefs,
    private val sleep: SleepState,
    private val publisher: PlayerUiPublisher,
    private val loader: BookLoader,
) {
    fun toggle() {
        val t = current.get() ?: return
        if (!current.loadedNow(t)) {
            val (index, position) = recorder.resumeSpot(t)
            scope.launch { loader.load(index, position, autoplay = true) }
            return
        }
        engine.toggle()
    }

    fun prev() {
        val t = current.get() ?: return
        if (!current.loadedNow(t)) {
            scope.launch { playChapterAt(t, maxOf(0, recorder.resumeSpot(t).first - 1)) }
            return
        }
        engine.previous()
        if (!engine.now().playing) engine.play()
    }

    fun next() {
        val t = current.get() ?: return
        if (t.items.size < 2) return
        if (!current.loadedNow(t)) {
            scope.launch { playChapterAt(t, minOf(t.items.lastIndex, recorder.resumeSpot(t).first + 1)) }
            return
        }
        engine.next()
        if (!engine.now().playing) engine.play()
    }

    fun skip(forward: Boolean) {
        val t = current.get() ?: return
        val seconds = if (forward) prefs.skipForward.value else prefs.skipBack.value
        val s = engine.now()
        val base = if (current.loadedNow(t)) s.positionMs else recorder.resumeSpot(t).second
        val duration = if (current.loadedNow(t)) s.durationMs else 0L
        val target = base + (if (forward) seconds else -seconds) * 1000L
        seek(maxOf(0L, if (duration > 0) minOf(duration, target) else target))
    }

    fun seek(positionMs: Long) {
        val t = current.get() ?: return
        if (current.loadedNow(t)) {
            engine.seekTo(positionMs)
        } else {
            scope.launch { loader.load(recorder.resumeSpot(t).first, positionMs, autoplay = false) }
        }
    }

    fun speed() {
        val now = playPrefs.speed()
        val next = SPEEDS[(SPEEDS.indexOf(now) + 1) % SPEEDS.size]
        playPrefs.setSpeed(next)
        if (engine.created) engine.setSpeed(next)
        publisher.publish()
    }

    fun sleep() {
        sleep.cycle(engine)
        publisher.publish()
    }

    fun mode() {
        val t = current.get() ?: return
        val kind = current.kindOf(t)
        playPrefs.nextMode(kind)
        if (current.loadedNow(t)) engine.setProfile(profileFor(kind, playPrefs.mode(kind)))
        publisher.publish()
    }

    fun chapterRows(): List<ChapterRow> {
        val t = current.get() ?: return emptyList()
        val known = durations.get(t.id)
        val at = if (current.loadedNow(t)) engine.now().index else recorder.resumeSpot(t).first
        return t.items.mapIndexed { i, item -> ChapterRow(i, item.title, known[i] ?: item.lengthMs.takeIf { it > 0 }, i == at) }
    }

    fun playChapter(index: Int) {
        val t = current.get() ?: return
        scope.launch { playChapterAt(t, index.coerceIn(0, t.items.lastIndex)) }
    }

    private fun playChapterAt(t: PlayBook, index: Int) {
        if (current.loadedNow(t)) {
            engine.skipTo(index)
            if (!engine.now().playing) engine.play()
        } else {
            loader.load(index, 0, autoplay = true)
        }
    }
}
