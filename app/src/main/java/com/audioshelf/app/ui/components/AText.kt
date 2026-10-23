package com.audioshelf.app.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val LINE_HEIGHT = 1.4f

@Composable
fun AText(
    text: String,
    size: Float,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    tabular: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: Float = size * LINE_HEIGHT,
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            textAlign = align,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
            fontFeatureSettings = if (tabular) "tnum" else null,
        ),
    )
}
