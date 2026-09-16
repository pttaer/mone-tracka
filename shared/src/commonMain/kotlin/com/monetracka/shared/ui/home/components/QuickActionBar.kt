package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.ui.theme.MoneTrackaColors

@Composable
fun QuickActionBar(
    onAddExpense: () -> Unit,
    onIncome: () -> Unit,
    onScan: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton(label = "Add Expense", isPrimary = true, icon = Icons.Default.Add, onClick = onAddExpense)
        QuickActionButton(label = "Income", icon = Icons.Default.ArrowUpward, onClick = onIncome)
        QuickActionButton(label = "Scan Bill", icon = Icons.Default.Receipt, onClick = onScan)
        QuickActionButton(label = "More", icon = Icons.Default.MoreHoriz, onClick = onMore)
    }
}

@Composable
private fun QuickActionButton(label: String, icon: ImageVector, isPrimary: Boolean = false, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isPrimary) MoneTrackaColors.MintPrimary else MoneTrackaColors.SurfaceSecondary)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPrimary) Color.White else MoneTrackaColors.TextDark,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) MoneTrackaColors.TextDark else MoneTrackaColors.TextGray,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
