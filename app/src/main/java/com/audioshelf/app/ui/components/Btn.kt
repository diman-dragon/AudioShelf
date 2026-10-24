package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.theme.AppTheme
import com.audioshelf.app.ui.theme.DangerColor

enum class BtnKind { Primary, Ghost, Danger }

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    wide: Boolean = false,
) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    val background = when (kind) {
        BtnKind.Primary -> c.accent
        BtnKind.Ghost -> Color.Transparent
        BtnKind.Danger -> DangerColor
    }
    val foreground = when (kind) {
        BtnKind.Primary -> c.onAccent
        BtnKind.Ghost -> c.text
        BtnKind.Danger -> Color.White
    }
    Box(
        modifier
            .then(if (wide) Modifier.fillMaxWidth() else Modifier)
            .clip(shape)
            .background(background)
            .then(if (kind == BtnKind.Ghost) Modifier.border(1.dp, c.line, shape) else Modifier)
            .tap(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (wide) 12.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AText(text, 14f, foreground, weight = FontWeight.SemiBold, align = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun Actions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = content)
}
