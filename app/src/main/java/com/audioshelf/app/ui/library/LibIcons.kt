package com.audioshelf.app.ui.library

import com.audioshelf.app.ui.player.SPath
import com.audioshelf.app.ui.player.SRect
import com.audioshelf.app.ui.player.SvgSpec

object LibIcons {
    val Rows = SvgSpec(true, listOf(SPath("M4 6h16M4 12h16M4 18h16")))

    val Cards = SvgSpec(
        false,
        listOf(SRect(4f, 4f, 7f, 7f, 1.6f), SRect(13f, 4f, 7f, 7f, 1.6f), SRect(4f, 13f, 7f, 7f, 1.6f), SRect(13f, 13f, 7f, 7f, 1.6f)),
    )

    val Refresh = SvgSpec(
        true,
        listOf(
            SPath("M20 11a8 8 0 0 0-14.5-4.5L4 8"),
            SPath("M4 4v4h4"),
            SPath("M4 13a8 8 0 0 0 14.5 4.5L20 16"),
            SPath("M20 20v-4h-4"),
        ),
    )

    val Sort = SvgSpec(true, listOf(SPath("M7 5v14M3.5 15.5 7 19l3.5-3.5"), SPath("M17 19V5M13.5 8.5 17 5l3.5 3.5")))
}
