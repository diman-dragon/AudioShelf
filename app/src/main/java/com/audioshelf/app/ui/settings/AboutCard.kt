package com.audioshelf.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioshelf.app.R
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.theme.AppTheme
import java.util.Calendar

@Composable
private fun Logo(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.width / 48f
        val stroke = Stroke(2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thin = Stroke(1.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val line = Path().apply {
            moveTo(9f * u, 40f * u)
            lineTo(39f * u, 40f * u)
        }
        drawPath(line, color, style = stroke)
        drawRoundRect(color, Offset(11f * u, 13f * u), Size(7f * u, 27f * u), CornerRadius(1.5f * u), style = stroke)
        drawRoundRect(color, Offset(20.5f * u, 8f * u), Size(7f * u, 32f * u), CornerRadius(1.5f * u), style = stroke)
        val lean = Path().apply {
            moveTo(30f * u, 40f * u)
            lineTo(36.5f * u, 12.5f * u)
            relativeLineTo(6.2f * u, 1.5f * u)
            lineTo(36f * u, 41.5f * u)
        }
        drawPath(lean, color, style = stroke)
        val mark = Path().apply {
            moveTo(24f * u, 17f * u)
            lineTo(24f * u, 31f * u)
        }
        drawPath(mark, color, style = thin)
    }
}

@Composable
fun AboutCard(version: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val outer = RoundedCornerShape(22.dp)
    val inner = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(outer)
            .background(Brush.linearGradient(listOf(c.accentSoft, Color.Transparent)))
            .border(1.dp, c.accent, outer)
            .drawWithContent {
                drawContent()
                val r = 22.dp.toPx()
                val s = Size(2 * r, 2 * r)
                val w = Stroke(2.dp.toPx())
                drawArc(c.accent, 180f, 90f, false, Offset(0f, 0f), s, style = w)
                drawArc(c.accent, 270f, 90f, false, Offset(size.width - 2 * r, 0f), s, style = w)
                drawArc(c.accent, 0f, 90f, false, Offset(size.width - 2 * r, size.height - 2 * r), s, style = w)
                drawArc(c.accent, 90f, 90f, false, Offset(0f, size.height - 2 * r), s, style = w)
            }
            .padding(10.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(inner)
                .background(c.surface)
                .border(1.dp, c.line, inner)
                .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(c.accentSoft).border(1.dp, c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Logo(c.accent, Modifier.size(48.dp))
            }
            AText("AudioShelf", 26f, c.text, Modifier.padding(top = 6.dp), FontWeight.Bold, TextAlign.Center, letterSpacing = 0.5.sp)
            AText(stringResource(R.string.about_tag), 14f, c.muted, align = TextAlign.Center)
            AText(
                stringResource(R.string.settings_version, version),
                12f,
                c.accent,
                Modifier.border(1.dp, c.accent, RoundedCornerShape(percent = 50)).padding(horizontal = 12.dp, vertical = 3.dp),
                FontWeight.Bold,
            )
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).width(56.dp).height(2.dp).background(c.accent))
            AText(stringResource(R.string.about_lead), 14f, c.text, align = TextAlign.Center, lineHeight = 21.7f)
            AText(stringResource(R.string.about_maker), 14f, c.text, align = TextAlign.Center, lineHeight = 21.7f)
            AText("© ${Calendar.getInstance().get(Calendar.YEAR)} DataLaw", 12f, c.muted, Modifier.padding(top = 10.dp))
        }
    }
}
