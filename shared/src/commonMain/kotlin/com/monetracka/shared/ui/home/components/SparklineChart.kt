package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SparklineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00D09C)
) {
    if (points.size < 2) return

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .drawWithCache {
                val width = size.width
                val height = size.height
                val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val minY = (points.minOrNull() ?: 0f)
                val range = (maxY - minY).coerceAtLeast(1f)
                val stepX = width / (points.size - 1)

                val padY = 8.dp.toPx()
                val offsetBottom = 4.dp.toPx()

                val coords = points.mapIndexed { i, p ->
                    val x = i * stepX
                    val y = height - ((p - minY) / range) * (height - padY) - offsetBottom
                    Offset(x, y)
                }

                val path = Path()
                val fillPath = Path()

                path.moveTo(coords.first().x, coords.first().y)
                fillPath.moveTo(coords.first().x, height)
                fillPath.lineTo(coords.first().x, coords.first().y)

                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(coords.last().x, height)
                fillPath.close()

                val fillBrush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.38f), lineColor.copy(alpha = 0.05f), Color.Transparent)
                )
                val lineStroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                val outerDotStroke = Stroke(width = 1.5.dp.toPx())

                val lastOffset = coords.last()
                val outerDotColor = lineColor.copy(alpha = 0.35f)
                val dotRadius = 4.dp.toPx()
                val pulseRadius = 8.dp.toPx()

                onDrawBehind {
                    drawPath(path = fillPath, brush = fillBrush)
                    drawPath(path = path, color = lineColor, style = lineStroke)
                    drawCircle(color = lineColor, radius = dotRadius, center = lastOffset)
                    drawCircle(color = outerDotColor, radius = pulseRadius, center = lastOffset, style = outerDotStroke)
                }
            }
    )
}
