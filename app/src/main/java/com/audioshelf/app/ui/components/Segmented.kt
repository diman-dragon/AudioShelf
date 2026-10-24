package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.player.SvgIcon
import com.audioshelf.app.ui.player.SvgSpec
import com.audioshelf.app.ui.theme.AppTheme

class SegItem(val id: String, val label: String? = null, val icon: SvgSpec? = null, val description: String? = null)

@Composable
fun Segmented(
    items: List<SegItem>,
    current: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { item ->
            val on = item.id == current
            val tint = if (on) c.onAccent else c.muted
            Box(
                Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .heightIn(min = 34.dp)
                    .clip(shape)
                    .background(if (on) c.accent else Color.Transparent)
                    .tap(description = item.description ?: item.label) { onPick(item.id) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (item.icon != null) SvgIcon(item.icon, tint, 20.dp)
                else AText(item.label ?: "", 14f, tint, weight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}
