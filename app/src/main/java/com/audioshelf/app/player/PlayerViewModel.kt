package com.audioshelf.app.player

import androidx.lifecycle.ViewModel
import com.audioshelf.app.AppContainer
import com.audioshelf.app.library.Book

class PlayerViewModel(private val container: AppContainer) : ViewModel(), PlayerActions by container.session {
    val ui get() = container.session.ui

    fun open(book: Book) = container.session.open(book)

    fun remember() = container.session.remember()

    fun chapterRows() = container.session.chapterRows()

    fun playChapter(index: Int) = container.session.playChapter(index)

    fun markList() = container.session.markList()

    fun canMark() = container.session.canMark()

    fun addMark() = container.session.addMark()

    fun removeMark(created: Long) = container.session.removeMark(created)

    fun jumpTo(mark: Mark) = container.session.jumpTo(mark)

    fun isMulti() = container.session.isMulti()

    fun kind() = container.session.currentKind()
}
