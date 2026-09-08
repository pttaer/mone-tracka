package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
        QuickActionButton(label = "Add Expense", isPrimary = true, icon = "+", onClick = onAddExpense)
        QuickActionButton(label = "Income", icon = "↑", onClick = onIncome)
        QuickActionButton(label = "Scan Bill", icon = "📷", onClick = onScan)
        QuickActionButton(label = "More", icon = "•••", onClick = onMore)
    }
}

@Composable
private fun QuickActionButton(label: String, icon: String, isPrimary: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isPrimary) Color(0xFF00D09C) else Color(0xFF172535))
        ) {
            Text(icon, fontSize = if (isPrimary) 22.sp else 18.sp, fontWeight = FontWeight.Bold, color = if (isPrimary) Color(0xFF022015) else Color.White)
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isPrimary) Color.White else Color(0xFF8FA2B6), modifier = Modifier.padding(top = 6.dp))
    }
}
