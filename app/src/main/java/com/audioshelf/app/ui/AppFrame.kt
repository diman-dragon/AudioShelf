package com.audioshelf.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.audioshelf.app.ui.components.Header
import com.audioshelf.app.ui.components.ScreenHost
import com.audioshelf.app.ui.components.SwipeGuard
import com.audioshelf.app.ui.components.LocalSwipeGuard
import com.audioshelf.app.ui.components.TabBar
import com.audioshelf.app.ui.components.swipeNavigation
import com.audioshelf.app.ui.theme.AppTheme
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppFrame(
    screen: Screen,
    direction: Int,
    guard: SwipeGuard,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Screen) -> Unit,
) {
    val c = AppTheme.colors
    CompositionLocalProvider(LocalSwipeGuard provides guard) {
        Column(modifier.fillMaxSize().background(c.bg)) {
            Header(stringResource(screen.label))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .swipeNavigation(guard) { step ->
                        Screen.entries.getOrNull(screen.ordinal + step)?.let(onSelect)
                    },
            ) {
                ScreenHost(screen, direction, content = content)
            }
            TabBar(screen, onSelect)
        }
    }
}
