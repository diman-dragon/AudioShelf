package com.audioshelf.app.player

class UiInput(
    val track: PlayBook,
    val state: PlayState,
    val loaded: Boolean,
    val resumeIndex: Int,
    val resumePositionMs: Long,
    val kind: Kind,
    val title: String,
    val author: String,
    val known: Map<Int, Long>,
    val skipBackSec: Int,
    val skipForwardSec: Int,
    val speed: Float,
    val sleep: SleepChoice,
    val mode: PlayMode,
    val marks: Int,
)

fun buildPlayerUi(i: UiInput): PlayerUi {
    val items = i.track.items
    val index = (if (i.loaded) i.state.index else i.resumeIndex).coerceIn(0, items.lastIndex)
    val position = if (i.loaded) i.state.positionMs else i.resumePositionMs
    val duration = when {
        i.loaded && i.state.durationMs > 0 -> i.state.durationMs
        else -> i.known[index] ?: items[index].lengthMs.coerceAtLeast(0)
    }
    val progress = if (i.kind == Kind.Book) bookProgress(i.known, items.size, index, position) else null
    val started = index > 0 || position > 0
    return PlayerUi(
        loaded = true,
        kind = i.kind,
        title = i.title,
        author = i.author,
        chapter = ChapterInfo(index, items.size, items[index].title),
        placeholder = when {
            i.kind == Kind.Album -> Placeholder.Disc
            started -> Placeholder.OpenBook
            else -> Placeholder.ClosedBook
        },
        coverFile = items[index].file,
        positionMs = position,
        durationMs = duration,
        playing = i.loaded && i.state.playing,
        skipBackSec = i.skipBackSec,
        skipForwardSec = i.skipForwardSec,
        speed = i.speed,
        sleep = SleepUi(i.sleep, if (i.sleep == SleepChoice.Chapter) 0 else i.state.sleepLeftMs),
        mode = i.mode,
        marks = i.marks,
        progress = progress,
    )
}
