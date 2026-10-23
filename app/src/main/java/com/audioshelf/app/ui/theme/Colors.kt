package com.audioshelf.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class AppColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val onAccent: Color,
    val seekBorder: Color,
    val seekTrack: Color,
    val isLight: Boolean,
) {
    val accentSoft: Color get() = accent.copy(alpha = 0.18f)
}

val DarkColors = AppColors(
    bg = Color(0xFF000000),
    surface = Color(0xFF121214),
    text = Color(0xFFF2F2F7),
    muted = Color(0xFF8E8E93),
    line = Color(0xFF222226),
    accent = Color(0xFFF0A64B),
    onAccent = Color(0xFF1A1204),
    seekBorder = Color(0xFF858585),
    seekTrack = Color(0xFF3B3B3B),
    isLight = false,
)

val LightColors = AppColors(
    bg = Color(0xFFE9DCBF),
    surface = Color(0xFFF3E9D1),
    text = Color(0xFF2B2418),
    muted = Color(0xFF7A6C52),
    line = Color(0xFFD3C4A0),
    accent = Color(0xFF9A5B1F),
    onAccent = Color(0xFFFFF8EA),
    seekBorder = Color(0xFFB2B2B2),
    seekTrack = Color(0xFFEFEFEF),
    isLight = true,
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }
