package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.CategorySpend

@Composable
fun CategoryDonutChart(
    categorySpends: List<CategorySpend>,
    totalText: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 6.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(strokeWidth)
            )

            var startAngle = -90f
            categorySpends.forEach { item ->
                val sweep = (item.percentage.toFloat() / 100f) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = Color(item.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
        }
        Text(
            text = totalText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
