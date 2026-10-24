package com.audioshelf.app.ui.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audioshelf.app.R
import com.audioshelf.app.library.ScanProgress
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.BtnKind
import com.audioshelf.app.ui.theme.AppTheme
import com.audioshelf.app.ui.theme.Dimens

private const val RUN = 0.35f

@Composable
fun ScanPanel(progress: ScanProgress, onStop: () -> Unit) {
    val c = AppTheme.colors
    val shape = RoundedCornerShape(Dimens.radius)
    val transition = rememberInfiniteTransition(label = "scan")
    val x by transition.animateFloat(
        initialValue = -RUN,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "scan-x",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AText(stringResource(R.string.scan_title), 16f, c.text, weight = FontWeight.SemiBold)
        AText(stringResource(R.string.scan_count, progress.files, progress.folders), 13f, c.muted)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.line)
                .drawBehind {
                    drawRoundRect(
                        c.accent,
                        topLeft = Offset(size.width * x, 0f),
                        size = Size(size.width * RUN, size.height),
                        cornerRadius = CornerRadius(size.height / 2),
                    )
                },
        )
        Btn(stringResource(R.string.scan_stop), onStop, kind = BtnKind.Ghost)
    }
}
