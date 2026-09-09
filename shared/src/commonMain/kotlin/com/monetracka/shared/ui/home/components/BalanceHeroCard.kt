package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.theme.MoneTrackaColors
import kotlin.math.abs
import kotlin.math.round

@Composable
fun BalanceHeroCard(
    balance: Double,
    trendPercent: Double,
    sparklinePoints: List<Float>,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    val (intPartWithSymbol, decPart) = remember(balance, currency) {
        CurrencyFormatter.splitAmount(balance, currency)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MoneTrackaColors.HeroCardBrush)
            .border(1.dp, MoneTrackaColors.BorderGlassLuminous, RoundedCornerShape(26.dp))
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL PORTFOLIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MoneTrackaColors.TextSecondary
                )
                val currencyTag = when (currency) {
                    "EUR" -> "EUR (€)"
                    "GBP" -> "GBP (£)"
                    "VND" -> "VND (₫)"
                    else -> "USD ($)"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = currencyTag,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = intPartWithSymbol,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = decPart,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneTrackaColors.TextSecondary
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPositive = trendPercent >= 0
                val trendColor = if (isPositive) MoneTrackaColors.MintPrimary else MoneTrackaColors.CoralDanger
                val trendIcon = if (isPositive) "↑" else "↓"
                val trendSign = if (isPositive) "+" else ""
                val roundedTrend = (round(abs(trendPercent) * 10) / 10.0).toString()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$trendIcon $trendSign$roundedTrend%",
                        color = trendColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "net growth this month",
                    color = MoneTrackaColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            SparklineChart(
                points = sparklinePoints,
                lineColor = if (trendPercent >= 0) MoneTrackaColors.MintPrimary else MoneTrackaColors.CoralDanger
            )
        }
    }
}
