package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter

@Composable
fun TransactionFeed(
    transactions: List<Transaction>,
    categories: Map<Long, Category> = emptyMap(),
    onDelete: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        transactions.forEach { tx ->
            val cat = categories[tx.categoryId]
            TransactionRow(tx = tx, category = cat, onDelete = onDelete)
        }
    }
}

@Composable
fun TransactionRow(
    tx: Transaction,
    category: Category?,
    onDelete: ((Long) -> Unit)?
) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) Color(0xFFFF5A79) else Color(0xFF00D09C)
    val amountPrefix = if (isExpense) "-" else "+"
    val categoryName = category?.name ?: "Transaction"
    val categoryEmoji = category?.emoji ?: if (isExpense) "💸" else "💰"

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
                    text = categoryEmoji,
                    fontSize = 20.sp
                )
            }
            Column {
                Text(
                    text = tx.note.ifBlank { categoryName },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = categoryName, color = Color(0xFF8FA2B6), fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(tx.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = "USD", color = Color(0xFF54687F), fontSize = 10.sp)
            }

            if (onDelete != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onDelete(tx.id) }
                ) {
                    Text(text = "✕", color = Color(0xFF8FA2B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
