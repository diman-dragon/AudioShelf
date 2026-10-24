package com.audioshelf.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.audioshelf.app.data.LANG_AUTO
import java.util.Locale

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LocalizedContent(language: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val config = LocalConfiguration.current
    val pair = remember(language, config, base) {
        if (language == LANG_AUTO) {
            base to config
        } else {
            val locale = Locale(language)
            val changed = Configuration(config).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            base.createConfigurationContext(changed) to changed
        }
    }
    CompositionLocalProvider(LocalContext provides pair.first, LocalConfiguration provides pair.second) { content() }
}

@Composable
fun SystemBarsEffect(dark: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
