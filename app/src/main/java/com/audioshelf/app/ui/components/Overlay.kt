package com.audioshelf.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val Scrim = Color(0x8C000000)

@Stable
class OverlayHost {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    fun show(content: @Composable () -> Unit) {
        this.content = content
    }

    fun close() {
        content = null
    }
}

val LocalOverlay = staticCompositionLocalOf { OverlayHost() }

@Composable
fun OverlayLayer(host: OverlayHost) {
    val content = host.content
    BackHandler(enabled = content != null) { host.close() }
    if (content != null) content()
}

@Composable
fun Modifier.scrim(onClose: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return background(Scrim).clickable(interactionSource = source, indication = null, onClick = onClose)
}

@Composable
fun Modifier.absorb(): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(interactionSource = source, indication = null, onClick = {})
}
