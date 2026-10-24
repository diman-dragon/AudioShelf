package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun Cycler(text: String, onNext: () -> Unit, description: String? = null, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .widthIn(min = 84.dp)
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(c.accentSoft)
            .border(1.dp, c.accent, shape)
            .tap(description = description ?: text, onClick = onNext)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AText(text, 15f, c.text, weight = FontWeight.SemiBold, maxLines = 1)
    }
}
