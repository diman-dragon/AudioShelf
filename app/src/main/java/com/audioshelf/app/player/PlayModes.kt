package com.audioshelf.app.player

val SPEEDS = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

val MODES = mapOf(
    Kind.Book to listOf(PlayMode.Continue, PlayMode.Chapter),
    Kind.Album to listOf(PlayMode.Off, PlayMode.All, PlayMode.One, PlayMode.Shuffle),
)
