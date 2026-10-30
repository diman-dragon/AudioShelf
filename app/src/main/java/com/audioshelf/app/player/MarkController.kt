package com.audioshelf.app.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Bookmarks of the current book: list, add, remove, jump. */
class MarkController(
    private val scope: CoroutineScope,
    private val engine: PlaybackEngine,
    private val current: CurrentBook,
    private val marks: MarkStore,
    private val loader: BookLoader,
) {
    fun list(): List<Mark> = current.get()?.let { marks.list(it.id) } ?: emptyList()

    fun canMark(): Boolean = current.get()?.let { current.loadedNow(it) } ?: false

    fun add(): Boolean {
        val t = current.get() ?: return false
        if (!current.loadedNow(t)) return false
        val s = engine.now()
        return marks.add(t.id, s.index, s.positionMs, t.expanded)
    }

    fun remove(created: Long) {
        val t = current.get() ?: return
        marks.remove(t.id, created)
    }

    fun jumpTo(mark: Mark) {
        val t = current.get() ?: return
        scope.launch {
            if (!current.loadedNow(t)) {
                loader.load(mark.index, mark.positionMs, autoplay = true)
                return@launch
            }
            if (engine.now().index != mark.index) engine.skipTo(mark.index, mark.positionMs) else engine.seekTo(mark.positionMs)
            if (!engine.now().playing) engine.play()
        }
    }
}
