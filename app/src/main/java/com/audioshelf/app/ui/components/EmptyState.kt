package com.audioshelf.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun EmptyState(
    @DrawableRes emoji: Int,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val c = AppTheme.colors
    Column(
        modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(emoji), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        AText(title, 16f, c.text, weight = FontWeight.SemiBold, align = TextAlign.Center)
        AText(hint, 16f, c.muted, align = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            AText(
                text = action,
                size = 14f,
                color = c.onAccent,
                weight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.accent)
                    .tap(onClick = onAction)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
