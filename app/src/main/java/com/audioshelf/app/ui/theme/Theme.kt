package com.audioshelf.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

object Dimens {
    val radius = 14.dp
    val headerHeight = 40.dp
    val screenPadding = 16.dp
}

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

@Composable
fun AudioShelfTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkColors else LightColors, content = content)
}
