package com.audioshelf.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.Screen

private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

@Composable
fun ScreenHost(
    screen: Screen,
    direction: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Screen) -> Unit,
) {
    key(screen) {
        val progress = remember { Animatable(if (direction == 0) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis = 220, easing = EaseOut))
        }
        Box(
            modifier.fillMaxSize().graphicsLayer {
                alpha = progress.value
                translationX = (1f - progress.value) * 48.dp.toPx() * direction
            },
        ) {
            content(screen)
        }
    }
}
