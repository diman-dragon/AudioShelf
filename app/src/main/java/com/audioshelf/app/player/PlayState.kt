package com.audioshelf.app.player

data class PlayState(
    val loaded: Boolean = false,
    val mediaId: String = "",
    val index: Int = 0,
    val count: Int = 0,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val ended: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val sleepLeftMs: Long = 0,
    val sleepAtChapterEnd: Boolean = false,
)
