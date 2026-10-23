package com.audioshelf.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp

sealed interface Shape

class SPath(val d: String, val fill: Boolean = false) : Shape {
    val path: Path by lazy { PathParser().parsePathString(d).toPath() }
}

class SRect(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float) : Shape

class SCircle(val cx: Float, val cy: Float, val r: Float) : Shape

class SText(val text: String, val x: Float, val y: Float, val size: Float) : Shape

class SvgSpec(val stroked: Boolean, val shapes: List<Shape>)

@Composable
fun SvgIcon(spec: SvgSpec, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val texts = remember(spec) { spec.shapes.filterIsInstance<SText>() }
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        scale(s, pivot = Offset.Zero) {
            spec.shapes.forEach { shape ->
                when (shape) {
                    is SPath -> {
                        if (shape.fill || !spec.stroked) drawPath(shape.path, tint, style = Fill)
                        if (spec.stroked) drawPath(shape.path, tint, style = strokeStyle())
                    }
                    is SRect -> drawRoundRect(
                        color = tint,
                        topLeft = Offset(shape.x, shape.y),
                        size = Size(shape.w, shape.h),
                        cornerRadius = CornerRadius(shape.rx, shape.rx),
                    )
                    is SCircle -> {
                        drawCircle(tint, shape.r, Offset(shape.cx, shape.cy), style = Fill)
                        if (spec.stroked) drawCircle(tint, shape.r, Offset(shape.cx, shape.cy), style = strokeStyle())
                    }
                    is SText -> Unit
                }
            }
        }
        texts.forEach { t ->
            val layout = measurer.measure(
                text = t.text,
                style = TextStyle(color = tint, fontSize = (t.size * s).toSp(), fontWeight = FontWeight.Bold),
            )
            drawText(
                layout,
                color = tint,
                topLeft = Offset(t.x * s - layout.size.width / 2f, t.y * s - layout.firstBaseline),
            )
        }
    }
}

private fun strokeStyle() = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)

