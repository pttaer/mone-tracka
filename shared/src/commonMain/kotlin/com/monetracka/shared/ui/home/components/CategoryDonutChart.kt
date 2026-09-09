package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.theme.CategoryColors

@Composable
fun CategoryDonutChart(
    categorySpends: List<CategorySpend>,
    totalText: String? = null,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val totalSpend = remember(categorySpends) { categorySpends.sumOf { it.amount } }
    val activeCategory = selectedIndex?.let { categorySpends.getOrNull(it) }

    Box(
        modifier = modifier
            .size(96.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (categorySpends.isNotEmpty()) {
                    selectedIndex = when (val curr = selectedIndex) {
                        null -> 0
                        else -> (curr + 1) % categorySpends.size
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseStroke = 8.dp.toPx()
            val expandedStroke = 11.dp.toPx()

            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                style = Stroke(baseStroke)
            )

            var startAngle = -90f
            categorySpends.forEachIndexed { i, item ->
                val sweep = (item.percentage.toFloat() / 100f) * 360f
                if (sweep > 0f) {
                    val isSelected = selectedIndex == i
                    val itemColor = CategoryColors.getOrElse(item.colorIndex) { Color(item.colorHex) }
                    drawArc(
                        color = if (isSelected) itemColor else itemColor.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        style = Stroke(
                            width = if (isSelected) expandedStroke else baseStroke,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweep
                }
            }
        }

        // Center Content: Selected Category or Total Spend
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            if (activeCategory != null) {
                Text(
                    text = activeCategory.category,
                    color = CategoryColors.getOrElse(activeCategory.colorIndex) { Color(activeCategory.colorHex) },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = CurrencyFormatter.format(activeCategory.amount, currency),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${activeCategory.percentage.toInt()}%",
                    color = Color(0xFF8FA2B6),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = totalText ?: CurrencyFormatter.format(totalSpend, currency),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
