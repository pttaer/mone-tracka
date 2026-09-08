package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        if (points.size < 2) return@Canvas
        val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val minY = (points.minOrNull() ?: 0f)
        val range = (maxY - minY).coerceAtLeast(1f)

        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = height - ((p - minY) / range) * (height - 8.dp.toPx()) - 4.dp.toPx()
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

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Draw active pulse dot at end
        val lastX = width
        val lastY = height - ((points.last() - minY) / range) * (height - 8.dp.toPx()) - 4.dp.toPx()
        drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(lastX, lastY))
        drawCircle(
            color = lineColor.copy(alpha = 0.3f),
            radius = 7.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(lastX, lastY),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
