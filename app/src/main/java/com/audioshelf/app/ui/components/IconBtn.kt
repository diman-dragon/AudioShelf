package com.audioshelf.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.player.SvgIcon
import com.audioshelf.app.ui.player.SvgSpec
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun IconBtn(
    icon: SvgSpec,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    spinning: Boolean = false,
) {
    val c = AppTheme.colors
    val angle = if (spinning) {
        val transition = rememberInfiniteTransition(label = "spin")
        val value by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "angle")
        value
    } else {
        0f
    }
    Box(
        modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(c.accentSoft)
            .border(1.dp, c.accent, CircleShape)
            .tap(enabled = enabled, description = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(icon, c.text, 20.dp, Modifier.graphicsLayer { rotationZ = angle })
    }
}
