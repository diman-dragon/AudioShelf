package com.audioshelf.app.player

import android.content.Context
import android.net.Uri
import com.audioshelf.app.ScreenWatch
import com.audioshelf.app.data.Prefs
import com.audioshelf.app.library.Book
import com.audioshelf.app.library.Background
import com.audioshelf.app.library.DurationReader
import com.audioshelf.app.library.DurationStore
import com.audioshelf.app.library.LibFile
import com.audioshelf.app.library.LibraryController
import com.audioshelf.app.library.LibraryEvent
import com.audioshelf.app.library.MetaStore
import com.audioshelf.app.library.ProgressState
import com.audioshelf.app.library.NoteStore
import com.audioshelf.app.library.ProgressStore
import com.audioshelf.app.library.QueueStore
import com.audioshelf.app.stats.StatsRecorder
import com.audioshelf.app.stats.StatsStore
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val END_GAP_MS = 3000L
private const val SLEEP_GRACE_MS = 1500L
private const val OPEN_HOLD_MS = 5000L
private const val POSITION_STEP_MS = 200L

class PlayerSession(
    private val scope: CoroutineScope,
    private val context: Context,
    private val engine: PlaybackEngine,
    private val library: LibraryController,
    private val meta: MetaStore,
    private val progress: ProgressStore,
    private val durations: DurationStore,
    private val prefs: Prefs,
    private val playPrefs: PlayPrefs,
    private val chapters: ChapterStore,
    private val scanner: ChapterScanner,
    private val marks: MarkStore,
    private val background: Background,
    private val screen: ScreenWatch,
    private val durationReader: DurationReader,
    private val stats: StatsStore,
    private val recorder: StatsRecorder,
    private val queue: QueueStore,
    private val notes: NoteStore,
) : PlayerActions {
    private val uiFlow = MutableStateFlow(PlayerUi())
    private val trackFlow = MutableStateFlow<PlayBook?>(null)
    private var sleepChoice = SleepChoice.Off
    private var sleepArmedAt = 0L
    private var wasEnded = false
    private var shown: PlayState? = null

    val ui: StateFlow<PlayerUi> = uiFlow.asStateFlow()
    val track: StateFlow<PlayBook?> = trackFlow.asStateFlow()

    init {
        BookJson.decode(playPrefs.last())?.let { trackFlow.value = expand(it) }
        publish()
        scope.launch { engine.state.collect { onEngine(it) } }
        scope.launch {
            combine(prefs.skipBack, prefs.skipForward, marks.version, meta.version) { _, _, _, _ -> }.collect { publish() }
        }
        scope.launch { library.events.collect { onLibraryEvent(it) } }
    }

    private fun expand(base: Book): PlayBook = expandBook(base, chapters::of)

    private fun isLoaded(t: PlayBook, s: PlayState): Boolean =
        s.loaded && s.count == t.items.size && t.items.any { it.uri == s.mediaId }

    private fun kindOf(t: PlayBook): Kind = library.kindOfBook(t.base)

    private fun onEngine(s: PlayState) {
        val t = trackFlow.value
        recorder.onState(if (t != null && isLoaded(t, s)) t.id else null, s.playing, s.positionMs, s.speed, System.currentTimeMillis())
        if (t != null && isLoaded(t, s)) {
            if (s.durationMs > 0) durations.note(t.id, s.index, s.durationMs)
            if (s.ended && !wasEnded) {
                remember(ended = true)
                advance(t)
            }
        }
        wasEnded = s.ended
        syncSleep(s)
        // Fader, sleep timer and listeners all poke the engine; redraw only for a real change or a
        // position step, not for every poke.
        val last = shown
        if (last == null || s.copy(positionMs = last.positionMs) != last || abs(s.positionMs - last.positionMs) >= POSITION_STEP_MS) publish()
    }

    private fun advance(t: PlayBook) {
        var next: Book? = null
        while (next == null) {
            val id = queue.pop() ?: break
            next = library.bookById(id)
        }
        if (next == null && prefs.autoNext.value && kindOf(t) == Kind.Book) {
            val rows = library.snapshot().rows
            val at = rows.indexOfFirst { it.book.id == t.id }
            if (at >= 0) next = rows.drop(at + 1).firstOrNull { it.state != ProgressState.Done && it.kind == Kind.Book }?.book
        }
        if (next != null && next.id != t.id) open(next)
    }

    private fun syncSleep(s: PlayState) {
        val armed = System.currentTimeMillis() - sleepArmedAt > SLEEP_GRACE_MS
        if (sleepChoice != SleepChoice.Off && armed && !(s.sleepLeftMs > 0 || s.sleepAtChapterEnd)) sleepChoice = SleepChoice.Off
    }

    /** Nothing is redrawn behind a dark screen; the first thing on wake-up is a fresh state. */
    fun onScreen(on: Boolean) {
        if (on) publish()
    }

    private fun publish() {
        if (!screen.on.value) return
        shown = engine.state.value
        val t = trackFlow.value
        if (t == null) {
            uiFlow.value = PlayerUi()
            return
        }
        val s = engine.state.value
        val loaded = isLoaded(t, s)
        val (index, position) = if (loaded) 0 to 0L else progress.resume(t.id)
        val m = library.metaOf(t.base)
        val kind = kindOf(t)
        uiFlow.value = buildPlayerUi(
            UiInput(
                track = t,
                state = s,
                loaded = loaded,
                resumeIndex = index,
                resumePositionMs = position,
                kind = kind,
                title = m.title.ifEmpty { t.base.name },
                author = m.author,
                known = durations.get(t.id),
                skipBackSec = prefs.skipBack.value,
                skipForwardSec = prefs.skipForward.value,
                speed = playPrefs.speed(),
                sleep = sleepChoice,
                mode = playPrefs.mode(kind),
                marks = marks.count(t.id),
            ),
        )
    }

    private fun onLibraryEvent(event: LibraryEvent) {
        val t = trackFlow.value ?: return
        when (event) {
            is LibraryEvent.Gone -> if (event.id == t.id) drop()
            is LibraryEvent.Moved -> if (event.from == t.id) retarget(t, event)
            LibraryEvent.FolderCleared -> drop()
        }
    }

    private fun retarget(t: PlayBook, event: LibraryEvent.Moved) {
        val next = t.base.copy(id = event.to, files = listOf(event.file))
        marks.move(event.from, event.to)
        notes.move(event.from, event.to)
        playPrefs.setLast(BookJson.encode(next))
        trackFlow.value = expand(next)
        publish()
    }

    private fun drop() {
        engine.stop()
        playPrefs.setLast(null)
        trackFlow.value = null
        publish()
    }

    fun open(book: Book) {
        background.hold(OPEN_HOLD_MS)
        scope.launch {
            val current = trackFlow.value
            if (current != null && current.id == book.id && isLoaded(current, engine.state.value)) {
                if (!engine.state.value.playing) engine.play()
                return@launch
            }
            remember()
            if (progress.state(book.id) == ProgressState.New) progress.save(book.id, 0, 1, false, 0)
            setTrack(book)
            if (scanner.scan(book)) setTrack(book)
            val (index, position) = progress.resume(book.id)
            load(index, position, autoplay = true)
        }
    }

    private fun setTrack(base: Book) {
        val expanded = expand(base)
        if (expanded.expanded) migrate(base, expanded)
        playPrefs.setLast(BookJson.encode(base))
        trackFlow.value = expanded
        publish()
        fillDurations(expanded)
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

    private fun fillDurations(t: PlayBook) {
        scope.launch {
            t.items.forEachIndexed { index, item ->
                if (durations.get(t.id)[index] != null) return@forEachIndexed
                background.calm()
                screen.on.first { it }
                val total = durationReader.read(item.uri) ?: return@forEachIndexed
                val ms = if (item.clipped) total - item.startMs else total
                if (ms <= 0) return@forEachIndexed
                durations.note(t.id, index, ms)
                if (trackFlow.value?.id == t.id) publish()
            }
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

    private fun load(index: Int, positionMs: Long, autoplay: Boolean) {
        val t = trackFlow.value ?: return
        val plan = plan(t, index, positionMs)
        val kind = kindOf(t)
        engine.load(plan.items, plan.index, plan.positionMs, autoplay, playPrefs.speed(), profileFor(kind, playPrefs.mode(kind)))
        publish()
    }

    /** Called on the player's thread when the system resumes playback; sets speed and profile before the items are handed over. */
    fun resumeItems(): ResumePlan {
        val t = trackFlow.value ?: throw IllegalStateException("nothing to resume")
        val (index, position) = progress.resume(t.id)
        val kind = kindOf(t)
        engine.setProfile(profileFor(kind, playPrefs.mode(kind)))
        engine.setSpeed(playPrefs.speed())
        return plan(t, index, position)
    }

    fun fileOf(uri: String): LibFile? = trackFlow.value?.items?.firstOrNull { it.file.uri == uri }?.file

    fun remember(ended: Boolean = false, periodic: Boolean = false) {
        val t = trackFlow.value ?: return
        val s = engine.now()
        if (!isLoaded(t, s)) return
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

    private fun current(): PlayBook? = trackFlow.value

    private fun resumeSpot(t: PlayBook): Pair<Int, Long> = progress.resume(t.id)

    private fun loadedNow(t: PlayBook): Boolean = isLoaded(t, engine.state.value)

    override fun onToggle() {
        val t = current() ?: return
        if (!loadedNow(t)) {
            val (index, position) = resumeSpot(t)
            scope.launch { load(index, position, autoplay = true) }
            return
        }
        engine.toggle()
    }

    override fun onPrev() {
        val t = current() ?: return
        if (!loadedNow(t)) {
            scope.launch { playChapterAt(t, maxOf(0, resumeSpot(t).first - 1)) }
            return
        }
        engine.previous()
        if (!engine.now().playing) engine.play()
    }

    override fun onNext() {
        val t = current() ?: return
        if (t.items.size < 2) return
        if (!loadedNow(t)) {
            scope.launch { playChapterAt(t, minOf(t.items.lastIndex, resumeSpot(t).first + 1)) }
            return
        }
        engine.next()
        if (!engine.now().playing) engine.play()
    }

    private fun playChapterAt(t: PlayBook, index: Int) {
        if (loadedNow(t)) {
            engine.skipTo(index)
            if (!engine.now().playing) engine.play()
        } else {
            load(index, 0, autoplay = true)
        }
    }

    override fun onBack() = skip(false)

    override fun onForward() = skip(true)

    private fun skip(forward: Boolean) {
        val t = current() ?: return
        val seconds = if (forward) prefs.skipForward.value else prefs.skipBack.value
        val s = engine.now()
        val base = if (loadedNow(t)) s.positionMs else resumeSpot(t).second
        val duration = if (loadedNow(t)) s.durationMs else 0L
        val target = base + (if (forward) seconds else -seconds) * 1000L
        onSeek(maxOf(0L, if (duration > 0) minOf(duration, target) else target))
    }

    override fun onSeek(positionMs: Long) {
        val t = current() ?: return
        if (loadedNow(t)) {
            engine.seekTo(positionMs)
        } else {
            scope.launch { load(resumeSpot(t).first, positionMs, autoplay = false) }
        }
    }

    override fun onSpeed() {
        val current = playPrefs.speed()
        val next = SPEEDS[(SPEEDS.indexOf(current) + 1) % SPEEDS.size]
        playPrefs.setSpeed(next)
        if (engine.created) engine.setSpeed(next)
        publish()
    }

    override fun onSleep() {
        sleepChoice = sleepChoice.next()
        sleepArmedAt = System.currentTimeMillis()
        engine.setSleep(sleepChoice.minutes.toLong(), sleepChoice == SleepChoice.Chapter)
        publish()
    }

    override fun onMode() {
        val t = current() ?: return
        val kind = kindOf(t)
        playPrefs.nextMode(kind)
        if (loadedNow(t)) engine.setProfile(profileFor(kind, playPrefs.mode(kind)))
        publish()
    }

    override fun onList() {}

    override fun onMarks() {}

    override fun onToLibrary() {}

    fun chapterRows(): List<ChapterRow> {
        val t = current() ?: return emptyList()
        val known = durations.get(t.id)
        val current = if (loadedNow(t)) engine.now().index else resumeSpot(t).first
        return t.items.mapIndexed { i, item -> ChapterRow(i, item.title, known[i] ?: item.lengthMs.takeIf { it > 0 }, i == current) }
    }

    fun playChapter(index: Int) {
        val t = current() ?: return
        scope.launch { playChapterAt(t, index.coerceIn(0, t.items.lastIndex)) }
    }

    fun markList(): List<Mark> = current()?.let { marks.list(it.id) } ?: emptyList()

    fun canMark(): Boolean = current()?.let { loadedNow(it) } ?: false

    fun addMark(): Boolean {
        val t = current() ?: return false
        if (!loadedNow(t)) return false
        val s = engine.now()
        return marks.add(t.id, s.index, s.positionMs, t.expanded)
    }

    fun removeMark(created: Long) {
        val t = current() ?: return
        marks.remove(t.id, created)
    }

    fun jumpTo(mark: Mark) {
        val t = current() ?: return
        scope.launch {
            if (!loadedNow(t)) {
                load(mark.index, mark.positionMs, autoplay = true)
                return@launch
            }
            if (engine.now().index != mark.index) engine.skipTo(mark.index, mark.positionMs) else engine.seekTo(mark.positionMs)
            if (!engine.now().playing) engine.play()
        }
    }

    fun isMulti(): Boolean = (current()?.items?.size ?: 0) > 1

    fun currentKind(): Kind? = current()?.let { kindOf(it) }
}
