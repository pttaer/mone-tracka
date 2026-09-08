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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

@Composable
fun TransactionFeed(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        transactions.forEach { tx ->
            TransactionRow(tx = tx)
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) Color(0xFFFF5A79) else Color(0xFF00D09C)
    val amountPrefix = if (isExpense) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF172535))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(amountColor.copy(alpha = 0.15f))
            ) {
                Text(
                    text = tx.category.take(1).uppercase(),
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column {
                Text(text = tx.note.ifBlank { tx.category }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = tx.category, color = Color(0xFF8FA2B6), fontSize = 11.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix$%.2f".format(tx.amount),
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(text = "USD", color = Color(0xFF54687F), fontSize = 10.sp)
        }
    }
}
