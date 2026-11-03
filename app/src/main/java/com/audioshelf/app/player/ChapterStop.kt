package com.audioshelf.app.player

class ChapterStop(private val pauseAtEnd: PauseAtEnd) {
    fun set(on: Boolean) {
        pauseAtEnd.setForChapters(on)
    }
}
