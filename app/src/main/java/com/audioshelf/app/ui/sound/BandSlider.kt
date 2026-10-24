package com.audioshelf.app.ui.sound

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.audioshelf.app.sound.MAX_DB
import com.audioshelf.app.sound.MIN_DB
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.noSwipe
import com.audioshelf.app.ui.theme.AppTheme
import kotlin.math.roundToInt

private val TRACK_HEIGHT = 170.dp
private val THUMB = 18.dp
private const val SPAN = MAX_DB - MIN_DB

fun fmtDb(db: Int): String = if (db > 0) "+$db" else db.toString()

fun dbAt(y: Float, height: Float): Int {
    val ratio = (1f - y / height).coerceIn(0f, 1f)
    return (MIN_DB + ratio * SPAN).roundToInt()
}

@Composable
fun BandSlider(label: String, db: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val current = rememberUpdatedState(onChange)
    val density = LocalDensity.current
    val heightPx = with(density) { TRACK_HEIGHT.toPx() }
    val ratio = (db - MIN_DB).toFloat() / SPAN
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AText(fmtDb(db), 11f, c.muted, tabular = true, maxLines = 1)
        Box(
            Modifier
                .width(34.dp)
                .height(TRACK_HEIGHT)
                .noSwipe()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        current.value(dbAt(down.position.y, heightPx))
                        drag(down.id) {
                            it.consume()
                            current.value(dbAt(it.position.y, heightPx))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(c.line))
            Box(
                Modifier
                    .width(4.dp)
                    .height(TRACK_HEIGHT * (kotlin.math.abs(db).toFloat() / SPAN))
                    .align(Alignment.Center)
                    .offset(y = TRACK_HEIGHT * (if (db >= 0) -(kotlin.math.abs(db).toFloat() / SPAN) / 2 else (kotlin.math.abs(db).toFloat() / SPAN) / 2))
                    .background(c.accent),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(TRACK_HEIGHT - THUMB) * ratio)
                    .size(THUMB)
                    .clip(CircleShape)
                    .background(c.accent),
            )
        }
        AText(label, 11f, c.muted, maxLines = 1)
    }
}
