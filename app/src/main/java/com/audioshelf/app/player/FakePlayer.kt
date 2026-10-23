package com.audioshelf.app.player

import kotlin.random.Random

val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

val MODES = mapOf(
    Kind.Book to listOf(PlayMode.Continue, PlayMode.Chapter),
    Kind.Album to listOf(PlayMode.Off, PlayMode.All, PlayMode.One, PlayMode.Shuffle),
)

class FakeChapter(val name: String, val durationMs: Long)

class FakeBook(
    val kind: Kind,
    val title: String,
    val author: String,
    val chapters: List<FakeChapter>,
) {
    val totalMs: Long get() = chapters.sumOf { it.durationMs }
}

val SAMPLE_BOOK = FakeBook(
    kind = Kind.Book,
    title = "Война и мир",
    author = "",
    chapters = List(4) { FakeChapter("Глава ${it + 1}", 600_000L) },
)

val SAMPLE_ALBUM = FakeBook(
    kind = Kind.Album,
    title = "Бетховен - Лунная соната",
    author = "Бетховен",
    chapters = listOf(
        FakeChapter("Adagio sostenuto", 372_000L),
        FakeChapter("Allegretto", 118_000L),
        FakeChapter("Presto agitato", 441_000L),
    ),
)

class FakeSession(
    val book: FakeBook,
    var index: Int = 0,
    var positionMs: Long = 0,
    var playing: Boolean = false,
    var speed: Float = 1f,
    var sleep: SleepChoice = SleepChoice.Off,
    var sleepLeftMs: Long = 0,
    var mode: PlayMode = MODES.getValue(book.kind).first(),
    var skipBackSec: Int = 15,
    var skipForwardSec: Int = 30,
    var marks: Int = 0,
) {
    private val chapter get() = book.chapters[index]

    fun toUi(): PlayerUi {
        val elapsed = book.chapters.take(index).sumOf { it.durationMs } + positionMs
        val total = book.totalMs
        return PlayerUi(
            loaded = true,
            kind = book.kind,
            title = book.title,
            author = book.author,
            chapter = ChapterInfo(index, book.chapters.size, chapter.name),
            placeholder = if (book.kind == Kind.Album) Placeholder.Disc
            else if (elapsed > 0) Placeholder.OpenBook else Placeholder.ClosedBook,
            positionMs = positionMs,
            durationMs = chapter.durationMs,
            playing = playing,
            skipBackSec = skipBackSec,
            skipForwardSec = skipForwardSec,
            speed = speed,
            sleep = SleepUi(sleep, sleepLeftMs),
            mode = mode,
            marks = marks,
            progress = if (book.kind == Kind.Book) {
                BookProgress((elapsed * 100 / total).toInt(), (total - elapsed).coerceAtLeast(0), exact = false)
            } else null,
        )
    }

    fun tick(realMs: Long) {
        if (!playing) return
        positionMs += (realMs * speed).toLong()
        if (sleepLeftMs > 0) {
            sleepLeftMs -= realMs
            if (sleepLeftMs <= 0) {
                sleepLeftMs = 0
                sleep = SleepChoice.Off
                playing = false
                return
            }
        }
        if (positionMs >= chapter.durationMs) finishChapter()
    }

    private fun finishChapter() {
        val last = index == book.chapters.lastIndex
        val sleepAtChapter = sleep == SleepChoice.Chapter
        if (sleepAtChapter) sleep = SleepChoice.Off
        when {
            mode == PlayMode.One -> positionMs = 0
            mode == PlayMode.Shuffle && book.chapters.size > 1 -> jump(randomOther())
            last && mode == PlayMode.All -> jump(0)
            last -> {
                positionMs = chapter.durationMs
                playing = false
            }
            else -> jump(index + 1)
        }
        if (sleepAtChapter || mode == PlayMode.Chapter) playing = false
    }

    private fun randomOther(): Int {
        var next = Random.nextInt(book.chapters.size)
        while (next == index) next = Random.nextInt(book.chapters.size)
        return next
    }

    private fun jump(to: Int) {
        index = to.coerceIn(0, book.chapters.lastIndex)
        positionMs = 0
    }

    fun toggle() {
        if (!playing && index == book.chapters.lastIndex && positionMs >= chapter.durationMs) positionMs = 0
        playing = !playing
    }

    fun step(delta: Int) = jump(index + delta)

    fun previous() {
        if (positionMs > 3000 || index == 0) positionMs = 0 else jump(index - 1)
    }

    fun seek(ms: Long) {
        positionMs = ms.coerceIn(0, chapter.durationMs)
    }

    fun skip(forward: Boolean) {
        val delta = (if (forward) skipForwardSec else -skipBackSec) * 1000L
        seek(positionMs + delta)
    }

    fun cycleSpeed() {
        speed = SPEEDS[(SPEEDS.indexOf(speed) + 1) % SPEEDS.size]
    }

    fun cycleSleep() {
        sleep = sleep.next()
        sleepLeftMs = sleep.minutes * 60_000L
    }

    fun cycleMode() {
        val modes = MODES.getValue(book.kind)
        mode = modes[(modes.indexOf(mode) + 1) % modes.size]
    }
}
