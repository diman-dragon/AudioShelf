package com.audioshelf.app.ui.components

import com.audioshelf.app.R
import com.audioshelf.app.player.Placeholder

fun Placeholder.drawable(): Int = when (this) {
    Placeholder.ClosedBook -> R.drawable.emoji_book_closed
    Placeholder.OpenBook -> R.drawable.emoji_book_open
    Placeholder.Disc -> R.drawable.emoji_disc
}
