package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioshelf.app.ui.theme.AppTheme
import com.audioshelf.app.ui.theme.Dimens

@Composable
fun Header(title: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Column(modifier.fillMaxWidth().background(c.bg).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.fillMaxWidth().height(Dimens.headerHeight).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            AText(
                text = title,
                size = 16f,
                color = c.text,
                weight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    }
}
