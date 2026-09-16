package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    isBalanceHidden: Boolean = false,
    onToggleHideBalance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (intPartWithSymbol, decPart) = remember(balance, currency) {
        CurrencyFormatter.splitAmount(balance, currency)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MoneTrackaColors.HeroGradientBrush)
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
                    color = Color.White.copy(alpha = 0.75f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                            .clickable(onClick = onToggleHideBalance)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isBalanceHidden) "Show Balance" else "Hide Balance",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    val currencyTag = "$currency (${CurrencyFormatter.symbol(currency)})"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.20f))
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
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                if (isBalanceHidden) {
                    Text(
                        text = "••••••",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                } else {
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
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPositive = trendPercent >= 0
                val trendColor = if (isPositive) Color(0xFFB2FFE0) else Color(0xFFFFB2B2)
                val trendBgColor = if (isPositive) Color.White.copy(alpha = 0.20f) else Color(0x33FF5A79)
                val trendIcon = if (isPositive) "↑" else "↓"
                val trendSign = if (isPositive) "+" else ""
                val roundedTrend = (round(abs(trendPercent) * 10) / 10.0).toString()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendBgColor)
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
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            SparklineChart(
                points = sparklinePoints,
                lineColor = Color.White.copy(alpha = 0.80f)
            )
        }
    }
}
