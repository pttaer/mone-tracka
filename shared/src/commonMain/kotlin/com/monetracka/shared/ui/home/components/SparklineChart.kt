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
            .height(48.dp)
            .drawWithCache {
                val width = size.width
                val height = size.height
                val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val minY = (points.minOrNull() ?: 0f)
                val range = (maxY - minY).coerceAtLeast(1f)
                val stepX = width / (points.size - 1)

                val padY = 8.dp.toPx()
                val offsetBottom = 4.dp.toPx()

                val path = Path()
                val fillPath = Path()

                points.forEachIndexed { i, p ->
                    val x = i * stepX
                    val y = height - ((p - minY) / range) * (height - padY) - offsetBottom
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(width, height)
                fillPath.close()

                val fillBrush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                )
                val lineStroke = Stroke(width = 2.5.dp.toPx())
                val outerDotStroke = Stroke(width = 1.5.dp.toPx())

                val lastX = width
                val lastY = height - ((points.last() - minY) / range) * (height - padY) - offsetBottom
                val lastOffset = Offset(lastX, lastY)
                val outerDotColor = lineColor.copy(alpha = 0.3f)
                val dotRadius = 3.5.dp.toPx()
                val pulseRadius = 7.dp.toPx()

                onDrawBehind {
                    drawPath(path = fillPath, brush = fillBrush)
                    drawPath(path = path, color = lineColor, style = lineStroke)
                    drawCircle(color = lineColor, radius = dotRadius, center = lastOffset)
                    drawCircle(color = outerDotColor, radius = pulseRadius, center = lastOffset, style = outerDotStroke)
                }
            }
    )
}
