package com.audioshelf.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.audioshelf.app.R
import com.audioshelf.app.ui.Screen
import com.audioshelf.app.ui.components.EmptyState

@Composable
fun ComingSoonScreen(screen: Screen, modifier: Modifier = Modifier) {
    EmptyState(
        emoji = screen.emoji,
        title = stringResource(R.string.soon_title),
        hint = stringResource(R.string.soon_hint),
        modifier = modifier,
    )
}
