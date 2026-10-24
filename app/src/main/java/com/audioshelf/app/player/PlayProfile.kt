package com.audioshelf.app.player

import androidx.media3.common.Player

class PlayProfile(
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val pauseAfter: Boolean = false,
    val fadeInMs: Int = 0,
    val fadeOutMs: Int = 0,
    val speech: Boolean = false,
)

private const val ALBUM_FADE_IN = 1500
private const val ALBUM_FADE_OUT = 2500

fun profileFor(kind: Kind, mode: PlayMode): PlayProfile = PlayProfile(
    repeat = when (mode) {
        PlayMode.All, PlayMode.Shuffle -> Player.REPEAT_MODE_ALL
        PlayMode.One -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    },
    shuffle = mode == PlayMode.Shuffle,
    pauseAfter = mode == PlayMode.Chapter,
    fadeInMs = if (kind == Kind.Album) ALBUM_FADE_IN else 0,
    fadeOutMs = if (kind == Kind.Album) ALBUM_FADE_OUT else 0,
    speech = kind == Kind.Book,
)
