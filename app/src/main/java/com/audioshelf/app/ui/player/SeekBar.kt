package com.audioshelf.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.player.fmtTime
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.LocalSwipeGuard
import com.audioshelf.app.ui.theme.AppTheme

private val AREA_HEIGHT = 22.dp
private val TRACK_HEIGHT = 7.5.dp
private val THUMB = 15.dp
private val TRACK_START = 3.dp
private val CENTER_Y = 10.dp

@Composable
fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val guard = LocalSwipeGuard.current
    var dragging by remember { mutableStateOf<Long?>(null) }
    val shown = (dragging ?: positionMs).coerceIn(0, durationMs.coerceAtLeast(0))
    val fraction = if (durationMs > 0) shown.toFloat() / durationMs else 0f
    val description = stringResource(R.string.player_seek)

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(AREA_HEIGHT)
                .semantics { contentDescription = description }
                .pointerInput(durationMs) {
                    if (durationMs <= 0) return@pointerInput
                    val start = TRACK_START.toPx() + THUMB.toPx() / 2
                    val end = size.width - THUMB.toPx() / 2
                    fun at(x: Float) = (((x - start) / (end - start)).coerceIn(0f, 1f) * durationMs).toLong()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        guard.blocked = true
                        down.consume()
                        dragging = at(down.position.x)
                        val finished = drag(down.id) { change ->
                            dragging = at(change.position.x)
                            change.consume()
                        }
                        val result = dragging
                        dragging = null
                        if (finished && result != null) onSeek(result)
                    }
                },
        ) {
            val trackLeft = TRACK_START.toPx()
            val trackH = TRACK_HEIGHT.toPx()
            val trackTop = CENTER_Y.toPx() - trackH / 2
            val trackW = size.width - trackLeft
            val radius = CornerRadius(trackH / 2, trackH / 2)
            val border = 1.dp.toPx()
            val thumb = THUMB.toPx()

            drawRoundRect(c.seekTrack, Offset(trackLeft, trackTop), Size(trackW, trackH), radius)

            val thumbCenter = trackLeft + thumb / 2 + fraction * (trackW - thumb)
            val clip = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        trackLeft, trackTop, trackLeft + trackW, trackTop + trackH, radius,
                    ),
                )
            }
            clipPath(clip) {
                drawRect(c.accent, Offset(trackLeft, trackTop), Size(thumbCenter - trackLeft, trackH))
            }
            drawRoundRect(
                c.seekBorder,
                Offset(trackLeft + border / 2, trackTop + border / 2),
                Size(trackW - border, trackH - border),
                radius,
                style = Stroke(border),
            )
            drawCircle(c.accent, thumb / 2, Offset(thumbCenter, CENTER_Y.toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AText(fmtTime(shown), 12f, c.muted, tabular = true)
            AText(if (durationMs > 0) fmtTime(durationMs) else "", 12f, c.muted, tabular = true)
        }
    }
}
