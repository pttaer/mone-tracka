package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

import androidx.compose.runtime.remember

private val HeroCardBrush = Brush.linearGradient(
    listOf(Color(0xFF1C2E42), Color(0xFF132232), Color(0xFF0E1A27))
)

@Composable
fun BalanceHeroCard(
    balance: Double,
    trendPercent: Double,
    sparklinePoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val (intPartWithSymbol, decPart) = remember(balance) {
        com.monetracka.shared.domain.util.CurrencyFormatter.splitAmount(balance, "USD")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(HeroCardBrush)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET PORTFOLIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF8FA2B6)
                )
                Text(
                    text = "USD ($)",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = intPartWithSymbol, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = decPart, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8FA2B6))
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPositive = trendPercent >= 0
                val trendColor = if (isPositive) Color(0xFF00D09C) else Color(0xFFFF5A79)
                val trendPrefix = if (isPositive) "+" else ""
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$trendPrefix%.1f%%".format(trendPercent),
                        color = trendColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "this month",
                    color = Color(0xFF8FA2B6),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            SparklineChart(points = sparklinePoints)
        }
    }
}
