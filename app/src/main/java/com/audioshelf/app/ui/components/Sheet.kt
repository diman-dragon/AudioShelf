package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.ui.theme.AppTheme
import com.audioshelf.app.ui.theme.DangerColor

class SheetAction(val label: String, val danger: Boolean = false, val run: () -> Unit = {})

fun OverlayHost.sheet(title: String, actions: List<SheetAction>) {
    show { SheetView(title, actions) { close() } }
}

@Composable
fun SheetView(title: String, actions: List<SheetAction>, onClose: () -> Unit) {
    val c = AppTheme.colors
    val all = actions + SheetAction(stringResource(R.string.lib_cancel))
    Box(Modifier.fillMaxSize().scrim(onClose), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(c.surface)
                .absorb()
                .navigationBarsPadding()
                .padding(8.dp),
        ) {
            AText(title, 13f, c.muted, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), maxLines = 1)
            all.forEach { action ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
                AText(
                    text = action.label,
                    size = 16f,
                    color = if (action.danger) DangerColor else c.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .tap {
                            onClose()
                            action.run()
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                )
            }
        }
    }
}
