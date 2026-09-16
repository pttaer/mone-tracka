package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun formatTransactionDate(epochMillis: Long): String {
    return try {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val monthStr = dt.month.name.lowercase().take(3).replaceFirstChar { it.uppercase() }
        val hourPad = dt.hour.toString().padStart(2, '0')
        val minPad = dt.minute.toString().padStart(2, '0')
        "$monthStr ${dt.dayOfMonth}, ${dt.year} • $hourPad:$minPad"
    } catch (_: Exception) {
        "Recent"
    }
}

@Composable
fun TransactionRow(
    tx: Transaction,
    category: Category?,
    accounts: Map<Long, Account> = emptyMap(),
    currency: String = "USD",
    onClick: (() -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Transaction?",
                    color = MoneTrackaColors.TextDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${if (tx.note.isNotBlank()) tx.note else category?.name ?: "Transaction"}\" (${if (tx.type == TransactionType.EXPENSE) "-" else "+"}${CurrencyFormatter.format(tx.amount, currency)})?",
                    color = MoneTrackaColors.TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(tx.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.CoralDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = MoneTrackaColors.CardWhite
        )
    }

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
            .clickable(enabled = onClick != null || onDelete != null) {
                if (onClick != null) {
                    onClick()
                } else if (onDelete != null) {
                    showDeleteDialog = true
                }
            }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    color = MoneTrackaColors.TextDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (isTransfer) "Transfer" else categoryName,
                    color = MoneTrackaColors.TextGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(tx.amount, currency)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = currency, color = MoneTrackaColors.TextLight, fontSize = 10.sp)
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Details",
                    tint = MoneTrackaColors.TextLight,
                    modifier = Modifier.size(12.dp)
                )
            } else if (onDelete != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MoneTrackaColors.SurfaceSecondary)
                        .clickable { showDeleteDialog = true }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    isOpen: Boolean,
    tx: Transaction?,
    category: Category?,
    accounts: Map<Long, Account> = emptyMap(),
    currency: String = "USD",
    onDismiss: () -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    if (!isOpen || tx == null) return

    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = {
                Text("Delete Transaction?", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text("This will permanently remove this record from your history and update your account balance.", color = MoneTrackaColors.TextGray, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDelete = false
                        onDelete(tx.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.CoralDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancel", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = MoneTrackaColors.CardWhite
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MoneTrackaColors.CardWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MoneTrackaColors.ProgressTrack) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            val typeLabel = when {
                isTransfer -> "Transfer"
                isExpense -> "Expense"
                else -> "Income"
            }

            // Header amount block
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isTransfer -> MoneTrackaColors.SurfaceSecondary
                                isExpense -> Color(0xFFFFEBEE)
                                else -> MoneTrackaColors.MintLight
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = typeLabel,
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(tx.amount, currency)}",
                    color = amountColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = formatTransactionDate(tx.dateMillis),
                    color = MoneTrackaColors.TextGray,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = MoneTrackaColors.ProgressTrack)

            // Details card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MoneTrackaColors.SurfaceSecondary)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val fromAcc = accounts[tx.accountId]
                val toAcc = tx.toAccountId?.let { accounts[it] }

                DetailItemRow(
                    label = "Account",
                    value = if (isTransfer) "${fromAcc?.emoji ?: "💳"} ${fromAcc?.name ?: "Account"} ➔ ${toAcc?.emoji ?: "💳"} ${toAcc?.name ?: "Account"}"
                    else "${fromAcc?.emoji ?: "💳"} ${fromAcc?.name ?: "Primary Account"}"
                )

                DetailItemRow(
                    label = "Category",
                    value = if (isTransfer) "🔁 Internal Transfer" else "${category?.emoji ?: "📁"} ${category?.name ?: "General"}"
                )

                if (tx.note.isNotBlank()) {
                    DetailItemRow(
                        label = "Note",
                        value = tx.note
                    )
                }
            }

            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.CoralDanger.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MoneTrackaColors.CoralDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Delete Transaction",
                        color = MoneTrackaColors.CoralDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MoneTrackaColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = MoneTrackaColors.TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

