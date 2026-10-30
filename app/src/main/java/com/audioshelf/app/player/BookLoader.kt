package com.audioshelf.app.player

import com.audioshelf.app.library.Background
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.ProgressState
import com.audioshelf.app.library.ProgressStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val OPEN_HOLD_MS = 5000L

/** Opens a book: scans its chapters, makes it the current one, hands its items to the engine. */
class BookLoader(
    private val scope: CoroutineScope,
    private val engine: PlaybackEngine,
    private val library: LibraryController,
    private val current: CurrentBook,
    private val progress: ProgressStore,
    private val marks: MarkStore,
    private val durations: DurationStore,
    private val playPrefs: PlayPrefs,
    private val scanner: ChapterScanner,
    private val background: Background,
    private val recorder: ProgressRecorder,
    private val publisher: PlayerUiPublisher,
    private val filler: DurationFiller,
) {
    private var opening: Job? = null

    /** The book that was open when the app last ran. */
    fun restoreLast() {
        BookJson.decode(playPrefs.last())?.let { current.set(current.expand(it)) }
    }

    /** A second tap while the first book is still being prepared replaces it, so two loads never race each other. */
    fun open(book: Book) {
        background.hold(OPEN_HOLD_MS)
        opening?.cancel()
        opening = scope.launch {
            val shown = current.get()
            if (shown != null && shown.id == book.id && current.isLoaded(shown, engine.state.value)) {
                if (!engine.state.value.playing) engine.play()
                return@launch
            }
            recorder.remember()
            if (progress.state(book.id) == ProgressState.New) progress.save(book.id, 0, 1, false, 0)
            setTrack(book)
            if (scanner.scan(book)) setTrack(book)
            val (index, position) = progress.resume(book.id)
            load(index, position, autoplay = true)
        }
    }

    private fun setTrack(base: Book) {
        val expanded = current.expand(base)
        if (expanded.expanded) migrate(base, expanded)
        playPrefs.setLast(BookJson.encode(base))
        current.set(expanded)
        publisher.publish()
        filler.fill(expanded)
    }

    private fun migrate(base: Book, expanded: PlayBook) {
        val map = remapper(base, expanded)
        progress.remap(base.id, map)
        marks.remap(base.id, map)
        val known = durations.get(base.id)
        if (known.size < expanded.items.size) {
            knownLengths(expanded).forEach { (index, ms) -> if (known[index] != ms) durations.note(base.id, index, ms) }
        }
    }

    /** The cover is a pointer resolved later off the playback path, so building the items never waits on storage. */
    private fun plan(t: PlayBook, index: Int, positionMs: Long): ResumePlan {
        val m = library.metaOf(t.base)
        val art = artUriOf(t.items[0].file.uri)
        val album = m.title.ifEmpty { t.base.name }
        val items = t.items.map { mediaItemOf(it, album, m.author, art) }
        return ResumePlan(items, index.coerceIn(0, items.lastIndex), positionMs)
    }

    fun load(index: Int, positionMs: Long, autoplay: Boolean) {
        val t = current.get() ?: return
        val plan = plan(t, index, positionMs)
        val kind = current.kindOf(t)
        engine.load(plan.items, plan.index, plan.positionMs, autoplay, playPrefs.speed(), profileFor(kind, playPrefs.mode(kind)))
        publisher.publish()
    }

    /** Called on the player's thread when the system resumes playback; sets speed and profile before the items are handed over. */
    fun resumeItems(): ResumePlan {
        val t = current.get() ?: throw IllegalStateException("nothing to resume")
        val (index, position) = progress.resume(t.id)
        val kind = current.kindOf(t)
        engine.setProfile(profileFor(kind, playPrefs.mode(kind)))
        engine.setSpeed(playPrefs.speed())
        return plan(t, index, position)
    }
}
