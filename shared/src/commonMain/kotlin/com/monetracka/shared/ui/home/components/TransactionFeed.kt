package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.theme.MoneTrackaColors

@Composable
fun TransactionRow(
    tx: Transaction,
    category: Category?,
    accounts: Map<Long, Account> = emptyMap(),
    currency: String = "USD",
    onDelete: ((Long) -> Unit)?
) {
    val isTransfer = tx.type == TransactionType.TRANSFER
    val isExpense = tx.type == TransactionType.EXPENSE
    val amountColor = when {
        isTransfer -> MoneTrackaColors.TextGray
        isExpense -> MoneTrackaColors.CoralDanger
        else -> MoneTrackaColors.MintDark
    }
    val amountPrefix = when {
        isTransfer -> ""
        isExpense -> "-"
        else -> "+"
    }
    val fromAccount = accounts[tx.accountId]
    val toAccount = tx.toAccountId?.let { accounts[it] }

    val categoryName = when {
        isTransfer -> "Internal Transfer"
        else -> category?.name ?: "Transaction"
    }
    val categoryEmoji = when {
        isTransfer -> "🔁"
        else -> category?.emoji ?: if (isExpense) "💸" else "💰"
    }
    val title = when {
        tx.note.isNotBlank() -> tx.note
        isTransfer -> "${fromAccount?.name ?: "Account"} ➔ ${toAccount?.name ?: "Account"}"
        else -> categoryName
    }

    val emojiIconBg = when {
        isTransfer -> MoneTrackaColors.SurfaceSecondary
        isExpense -> Color(0xFFFFEBEE)
        else -> MoneTrackaColors.MintLight
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = MoneTrackaColors.CardShadowColor)
            .clip(RoundedCornerShape(16.dp))
            .background(MoneTrackaColors.CardWhite)
            .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(16.dp))
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
                    .background(emojiIconBg)
            ) {
                Text(
                    text = categoryEmoji,
                    fontSize = 20.sp
                )
            }
            Column {
                Text(
                    text = title,
                    color = MoneTrackaColors.TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = categoryName, color = MoneTrackaColors.TextGray, fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(tx.amount, currency)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = currency, color = MoneTrackaColors.TextLight, fontSize = 10.sp)
            }

            if (onDelete != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MoneTrackaColors.SurfaceSecondary)
                        .clickable { onDelete(tx.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete transaction",
                        tint = MoneTrackaColors.TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
