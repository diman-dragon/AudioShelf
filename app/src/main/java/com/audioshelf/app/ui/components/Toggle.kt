package com.audioshelf.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.theme.AppTheme

private val TRACK_W = 52.dp
private val TRACK_H = 30.dp
private val THUMB = 22.dp
private val PAD = 4.dp

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, description: String? = null, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val track by animateColorAsState(if (checked) c.accent else c.surface, tween(160), label = "track")
    val thumb by animateColorAsState(if (checked) c.onAccent else c.muted, tween(160), label = "thumb")
    val x by animateDpAsState(if (checked) TRACK_W - THUMB - PAD * 2 else 0.dp, tween(160), label = "x")
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .size(TRACK_W, TRACK_H)
            .clip(shape)
            .background(track)
            .border(1.dp, if (checked) c.accent else c.line, shape)
            .semantics { if (description != null) contentDescription = description }
            .toggleable(value = checked, interactionSource = source, indication = null, role = Role.Switch, onValueChange = onChange)
            .padding(PAD),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = x).size(THUMB).clip(CircleShape).background(thumb))
    }
}
