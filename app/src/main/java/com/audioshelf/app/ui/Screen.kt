package com.audioshelf.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.audioshelf.app.R

enum class Screen(@StringRes val label: Int, @DrawableRes val emoji: Int) {
    Library(R.string.nav_library, R.drawable.emoji_books),
    Player(R.string.nav_player, R.drawable.emoji_headphones),
    Sound(R.string.nav_sound, R.drawable.emoji_speaker),
    Settings(R.string.nav_settings, R.drawable.emoji_gear),
}
