package com.audioshelf.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.ui.Screen
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun TabBar(selected: Screen, onSelect: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Column(modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Screen.entries.forEach { screen -> Tab(screen, screen == selected, onSelect) }
        }
    }
}

@Composable
private fun RowScope.Tab(screen: Screen, active: Boolean, onSelect: (Screen) -> Unit) {
    val c = AppTheme.colors
    val label = stringResource(screen.label)
    Column(
        Modifier
            .weight(1f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) c.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
            .tap(description = label) { onSelect(screen) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(screen.emoji), contentDescription = null, modifier = Modifier.size(26.dp))
        AText(
            text = label,
            size = 11f,
            color = if (active) c.text else c.muted,
            weight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
