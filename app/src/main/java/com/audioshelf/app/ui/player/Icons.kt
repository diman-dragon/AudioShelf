package com.audioshelf.app.ui.player

import com.audioshelf.app.player.PlayMode

private fun stroked(vararg shapes: Shape) = SvgSpec(stroked = true, shapes = shapes.toList())
private fun solid(vararg shapes: Shape) = SvgSpec(stroked = false, shapes = shapes.toList())

object Icons {
    val Play = solid(SPath("M8 5.5v13a1 1 0 0 0 1.5.86l10.5-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5z"))
    val Pause = solid(SRect(6f, 5f, 4.5f, 14f, 1.2f), SRect(13.5f, 5f, 4.5f, 14f, 1.2f))
    val Prev = solid(
        SRect(5f, 5f, 2.6f, 14f, 1f),
        SPath("M19 6.2v11.6a1 1 0 0 1-1.6.8l-8-5.8a1 1 0 0 1 0-1.6l8-5.8A1 1 0 0 1 19 6.2z"),
    )
    val Next = solid(
        SRect(16.4f, 5f, 2.6f, 14f, 1f),
        SPath("M5 6.2v11.6a1 1 0 0 0 1.6.8l8-5.8a1 1 0 0 0 0-1.6l-8-5.8A1 1 0 0 0 5 6.2z"),
    )

    fun back(seconds: Int) = stroked(
        SPath("M5 12a7 7 0 1 0 2.1-5"),
        SPath("M5 4v4h4"),
        SText(seconds.toString(), 12f, 15.5f, 7.5f),
    )

    fun forward(seconds: Int) = stroked(
        SPath("M19 12a7 7 0 1 1-2.1-5"),
        SPath("M19 4v4h-4"),
        SText(seconds.toString(), 12f, 15.5f, 7.5f),
    )

    val Mark = stroked(SPath("M6 4h12v17l-6-4.5L6 21z"))

    val List = stroked(
        SPath("M9 6h11M9 12h11M9 18h11"),
        SCircle(4.5f, 6f, 1f),
        SCircle(4.5f, 12f, 1f),
        SCircle(4.5f, 18f, 1f),
    )

    val Moon = stroked(SPath("M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z"))

    private val repeat = arrayOf(
        SPath("M17 3l3 3-3 3"),
        SPath("M4 11V9a3 3 0 0 1 3-3h13"),
        SPath("M7 21l-3-3 3-3"),
        SPath("M20 13v2a3 3 0 0 1-3 3H4"),
    )

    fun mode(mode: PlayMode): SvgSpec = when (mode) {
        PlayMode.Continue -> stroked(SPath("M4 12h13"), SPath("M13 7l5 5-5 5"))
        PlayMode.Chapter -> stroked(SPath("M4 12h9"), SPath("M9 8l4 4-4 4"), SPath("M19 5v14"))
        PlayMode.Off -> stroked(SPath("M3 8h10M3 12h10M3 16h6"), SPath("M17 9l4 3-4 3z", fill = true))
        PlayMode.All -> stroked(*repeat)
        PlayMode.One -> stroked(*repeat, SText("1", 12f, 15f, 8f))
        PlayMode.Shuffle -> stroked(
            SPath("M3 7h3.5c5 0 6 10 11 10H21"),
            SPath("M3 17h3.5c1.6 0 2.7-.9 3.6-2.1"),
            SPath("M13.8 9.1C14.7 7.9 15.8 7 17.5 7H21"),
            SPath("M18 4l3 3-3 3"),
            SPath("M18 14l3 3-3 3"),
        )
    }
}
