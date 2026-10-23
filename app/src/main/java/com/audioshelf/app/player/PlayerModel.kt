package com.audioshelf.app.player

enum class Kind { Book, Album }

enum class PlayMode { Continue, Chapter, Off, All, One, Shuffle }

enum class Placeholder { ClosedBook, OpenBook, Disc }

enum class SleepChoice(val minutes: Int) {
    Off(0), Min15(15), Min30(30), Min45(45), Min60(60), Chapter(0);

    fun next(): SleepChoice = entries[(ordinal + 1) % entries.size]
}

data class SleepUi(val choice: SleepChoice = SleepChoice.Off, val leftMs: Long = 0)

data class BookProgress(val percent: Int, val leftMs: Long, val exact: Boolean)

data class ChapterInfo(val index: Int, val count: Int, val name: String)

data class PlayerUi(
    val loaded: Boolean = false,
    val kind: Kind = Kind.Book,
    val title: String = "",
    val author: String = "",
    val chapter: ChapterInfo = ChapterInfo(0, 1, ""),
    val placeholder: Placeholder = Placeholder.ClosedBook,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playing: Boolean = false,
    val skipBackSec: Int = 15,
    val skipForwardSec: Int = 30,
    val speed: Float = 1f,
    val sleep: SleepUi = SleepUi(),
    val mode: PlayMode = PlayMode.Continue,
    val marks: Int = 0,
    val progress: BookProgress? = null,
) {
    val multi: Boolean get() = chapter.count > 1
}

interface PlayerActions {
    fun onToggle()
    fun onPrev()
    fun onNext()
    fun onBack()
    fun onForward()
    fun onSeek(positionMs: Long)
    fun onSpeed()
    fun onSleep()
    fun onList()
    fun onMode()
    fun onMarks()
    fun onToLibrary()

    companion object {
        val None = object : PlayerActions {
            override fun onToggle() {}
            override fun onPrev() {}
            override fun onNext() {}
            override fun onBack() {}
            override fun onForward() {}
            override fun onSeek(positionMs: Long) {}
            override fun onSpeed() {}
            override fun onSleep() {}
            override fun onList() {}
            override fun onMode() {}
            override fun onMarks() {}
            override fun onToLibrary() {}
        }
    }
}
