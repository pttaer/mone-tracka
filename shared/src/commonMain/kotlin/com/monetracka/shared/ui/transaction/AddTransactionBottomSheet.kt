package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.ui.theme.MoneTrackaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    isOpen: Boolean,
    categories: List<Category>,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, Long, String, Boolean, com.monetracka.shared.domain.model.RecurringInterval) -> Unit
) {
    if (!isOpen) return

    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    var selectedCategoryId by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.id ?: 1L)
    }

    var isRecurring by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf(com.monetracka.shared.domain.model.RecurringInterval.MONTHLY) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MoneTrackaColors.CardWhite,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(MoneTrackaColors.ProgressTrack)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Log Transaction",
                color = MoneTrackaColors.TextDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(16.dp))

            // Type Toggle (Expense / Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MoneTrackaColors.SurfaceSecondary)
                    .padding(4.dp)
            ) {
                listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    if (type == TransactionType.EXPENSE) MoneTrackaColors.CoralDanger else MoneTrackaColors.MintPrimary
                                } else Color.Transparent
                            )
                            .clickable {
                                selectedType = type
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MoneTrackaColors.TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountStr = filtered
                        errorMessage = null
                    }
                },
                label = { Text("Amount ($)", color = MoneTrackaColors.TextGray) },
                placeholder = { Text("0.00", color = MoneTrackaColors.TextLight) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MoneTrackaColors.TextDark,
                    unfocusedTextColor = MoneTrackaColors.TextDark,
                    focusedBorderColor = MoneTrackaColors.MintPrimary,
                    unfocusedBorderColor = MoneTrackaColors.ProgressTrack
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = noteStr,
                onValueChange = { noteStr = it },
                label = { Text("Note / Description", color = MoneTrackaColors.TextGray) },
                placeholder = { Text("e.g. Lunch with team", color = MoneTrackaColors.TextLight) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MoneTrackaColors.TextDark,
                    unfocusedTextColor = MoneTrackaColors.TextDark,
                    focusedBorderColor = MoneTrackaColors.MintPrimary,
                    unfocusedBorderColor = MoneTrackaColors.ProgressTrack
                )
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Category (${filteredCategories.size})",
                color = MoneTrackaColors.TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredCategories, key = { it.id }) { cat ->
                    val isSel = selectedCategoryId == cat.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) MoneTrackaColors.MintLight else MoneTrackaColors.SurfaceSecondary)
                            .border(
                                width = if (isSel) 1.5.dp else 0.dp,
                                color = if (isSel) MoneTrackaColors.MintPrimary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedCategoryId = cat.id
                                errorMessage = null
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = cat.emoji, fontSize = 16.sp)
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                color = if (isSel) MoneTrackaColors.MintPrimary else MoneTrackaColors.TextDark,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Recurring subscription toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MoneTrackaColors.SurfaceSecondary)
                    .clickable { isRecurring = !isRecurring }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Recurring / Subscription", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MoneTrackaColors.TextDark)
                    Text("Auto-repeat transaction on interval", fontSize = 11.sp, color = MoneTrackaColors.TextGray)
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRecurring) MoneTrackaColors.MintPrimary else MoneTrackaColors.ProgressTrack),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecurring) {
                        Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isRecurring) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.monetracka.shared.domain.model.RecurringInterval.values().forEach { interval ->
                        val isIntervalSel = selectedInterval == interval
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isIntervalSel) MoneTrackaColors.MintLight else MoneTrackaColors.SurfaceSecondary)
                                .border(
                                    width = if (isIntervalSel) 1.dp else 0.dp,
                                    color = if (isIntervalSel) MoneTrackaColors.MintPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedInterval = interval }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = interval.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                color = if (isIntervalSel) MoneTrackaColors.MintPrimary else MoneTrackaColors.TextGray,
                                fontWeight = if (isIntervalSel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MoneTrackaColors.CoralDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0 || !amt.isFinite() || amt > 1_000_000_000.0) {
                        errorMessage = "Amount must be between $0.01 and $1,000,000,000"
                        return@Button
                    }
                    if (selectedCategoryId <= 0) {
                        errorMessage = "Please select a category"
                        return@Button
                    }
                    val sanitizedNote = noteStr.trim().take(255)
                    onSave(amt, selectedType, selectedCategoryId, sanitizedNote, isRecurring, selectedInterval)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.MintPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm Transaction", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
