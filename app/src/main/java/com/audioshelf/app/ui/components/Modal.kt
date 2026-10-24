package com.audioshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.ui.theme.AppTheme

@Composable
fun ModalView(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.fillMaxSize().scrim(onClose).systemBarsPadding().imePadding().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(c.surface)
                .border(1.dp, c.line, shape)
                .absorb()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AText(title, 17f, c.text, weight = FontWeight.Bold)
            content()
        }
    }
}

fun OverlayHost.message(textRes: Int) {
    show {
        ModalView(stringResource(R.string.msg_title), { close() }) {
            AText(stringResource(textRes), 16f, AppTheme.colors.text, weight = FontWeight.SemiBold)
            Actions { Btn(stringResource(R.string.msg_ok), { close() }) }
        }
    }
}
