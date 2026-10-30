package com.audioshelf.app.player

import com.audioshelf.app.library.Book
import com.audioshelf.app.library.LibFile
import kotlinx.coroutines.flow.StateFlow

/**
 * What the screen, the service and the notification talk to. It holds no logic of its own: every
 * call goes to the part that owns it (loading, commands, bookmarks, progress, drawing).
 */
class PlayerSession(
    private val current: CurrentBook,
    private val publisher: PlayerUiPublisher,
    private val loader: BookLoader,
    private val commands: PlaybackCommands,
    private val markControl: MarkController,
    private val recorder: ProgressRecorder,
) : PlayerActions {
    val ui: StateFlow<PlayerUi> = publisher.ui
    val track: StateFlow<PlayBook?> = current.track

    fun onScreen(on: Boolean) = publisher.onScreen(on)

    fun open(book: Book) = loader.open(book)

    fun resumeItems(): ResumePlan = loader.resumeItems()

    fun fileOf(uri: String): LibFile? = current.get()?.items?.firstOrNull { it.file.uri == uri }?.file

    fun remember(ended: Boolean = false, periodic: Boolean = false) = recorder.remember(ended, periodic)

    override fun onToggle() = commands.toggle()

    override fun onPrev() = commands.prev()

    override fun onNext() = commands.next()

    override fun onBack() = commands.skip(false)

    override fun onForward() = commands.skip(true)

    override fun onSeek(positionMs: Long) = commands.seek(positionMs)

    override fun onSpeed() = commands.speed()

    override fun onSleep() = commands.sleep()

    override fun onMode() = commands.mode()

    override fun onList() {}

    override fun onMarks() {}

    override fun onToLibrary() {}

    fun chapterRows(): List<ChapterRow> = commands.chapterRows()

    fun playChapter(index: Int) = commands.playChapter(index)

    fun markList(): List<Mark> = markControl.list()

    fun canMark(): Boolean = markControl.canMark()

    fun addMark(): Boolean = markControl.add()

    fun removeMark(created: Long) = markControl.remove(created)

    fun jumpTo(mark: Mark) = markControl.jumpTo(mark)

    fun isMulti(): Boolean = (current.get()?.items?.size ?: 0) > 1

    fun currentKind(): Kind? = current.get()?.let { current.kindOf(it) }
}
