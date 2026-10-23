package com.audioshelf.app.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

class SwipeGuard {
    var blocked = false
}

val LocalSwipeGuard = compositionLocalOf { SwipeGuard() }

@Composable
fun rememberSwipeGuard(): SwipeGuard = remember { SwipeGuard() }

fun Modifier.noSwipe(): Modifier = composed {
    val guard = LocalSwipeGuard.current
    pointerInput(guard) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed }) guard.blocked = true
            }
        }
    }
}
