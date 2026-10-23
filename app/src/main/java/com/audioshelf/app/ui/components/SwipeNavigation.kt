package com.audioshelf.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val MIN_DIST = 60.dp
private val EDGE = 24.dp
private const val RATIO = 1.5f

fun Modifier.swipeNavigation(guard: SwipeGuard, onSwipe: (step: Int) -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onSwipe)
    pointerInput(guard) {
        val minDist = MIN_DIST.toPx()
        val edge = EDGE.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val start = down.position
            var last = start
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
                event.changes.firstOrNull { it.id == down.id }?.let { last = it.position }
            } while (event.changes.any { it.pressed })

            val blocked = guard.blocked
            guard.blocked = false
            if (blocked) return@awaitEachGesture

            val dx = last.x - start.x
            val dy = last.y - start.y
            val nearEdge = start.x < edge || start.x > size.width - edge
            if (nearEdge || abs(dx) < minDist || abs(dx) < abs(dy) * RATIO) return@awaitEachGesture
            latest(if (dx < 0) 1 else -1)
        }
    }
}
