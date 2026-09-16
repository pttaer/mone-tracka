package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.ui.home.AccountUiModel
import com.monetracka.shared.ui.theme.MoneTrackaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    isOpen: Boolean,
    categories: List<Category>,
    accounts: List<AccountUiModel> = emptyList(),
    initialType: TransactionType = TransactionType.EXPENSE,
    currency: String = "USD",
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, Long, Long, String, Boolean, com.monetracka.shared.domain.model.RecurringInterval) -> Unit
) {
    if (!isOpen) return

    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 1L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        try {
            amountFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val currencySymbol = when (currency) {
        "EUR" -> "€"
        "GBP" -> "£"
        "VND" -> "₫"
        "JPY" -> "¥"
        else -> "$"
    }

    val quickIncrements = remember(currency) {
        when (currency) {
            "VND" -> listOf(50000.0 to "+50k", 100000.0 to "+100k", 200000.0 to "+200k", 500000.0 to "+500k")
            "JPY" -> listOf(500.0 to "+500", 1000.0 to "+1k", 5000.0 to "+5k", 10000.0 to "+10k")
            else -> listOf(10.0 to "+10", 25.0 to "+25", 50.0 to "+50", 100.0 to "+100")
        }
    }

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
                    val sanitized = input.replace(',', '.')
                    val filtered = sanitized.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountStr = filtered
                        errorMessage = null
                    }
                },
                label = { Text("Amount ($currencySymbol)", color = MoneTrackaColors.TextGray) },
                placeholder = { Text("0.00", color = MoneTrackaColors.TextLight) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { noteFocusRequester.requestFocus() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MoneTrackaColors.TextDark,
                    unfocusedTextColor = MoneTrackaColors.TextDark,
                    focusedBorderColor = MoneTrackaColors.MintPrimary,
                    unfocusedBorderColor = MoneTrackaColors.ProgressTrack
                )
            )

            // Quick increment chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickIncrements.forEach { (delta, label) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MoneTrackaColors.SurfaceSecondary)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val current = amountStr.toDoubleOrNull() ?: 0.0
                                val next = current + delta
                                amountStr = if (currency == "VND" || currency == "JPY") {
                                    next.toLong().toString()
                                } else {
                                    val cents = (next * 100).toLong()
                                    "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
                                }
                                errorMessage = null
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = MoneTrackaColors.TextDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = noteStr,
                onValueChange = { noteStr = it },
                label = { Text("Note / Description", color = MoneTrackaColors.TextGray) },
                placeholder = { Text("e.g. Lunch with team", color = MoneTrackaColors.TextLight) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0 || !amt.isFinite() || amt > 1_000_000_000.0) {
                            errorMessage = "Amount must be between $0.01 and $1,000,000,000"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            return@KeyboardActions
                        }
                        if (selectedCategoryId <= 0) {
                            errorMessage = "Please select a category"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            return@KeyboardActions
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val sanitizedNote = noteStr.trim().take(255)
                        onSave(amt, selectedType, selectedCategoryId, selectedAccountId, sanitizedNote, isRecurring, selectedInterval)
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(noteFocusRequester),
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

            if (accounts.isNotEmpty()) {
                Text(
                    text = "Account",
                    color = MoneTrackaColors.TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts, key = { it.account.id }) { acc ->
                        val isSel = selectedAccountId == acc.account.id
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
                                    selectedAccountId = acc.account.id
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = acc.account.emoji, fontSize = 16.sp)
                                Text(
                                    text = acc.account.name,
                                    fontSize = 12.sp,
                                    color = if (isSel) MoneTrackaColors.MintPrimary else MoneTrackaColors.TextDark,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // Recurring subscription toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MoneTrackaColors.SurfaceSecondary)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Recurring / Subscription", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MoneTrackaColors.TextDark)
                    Text("Auto-repeat transaction on interval", fontSize = 11.sp, color = MoneTrackaColors.TextGray)
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MoneTrackaColors.CardWhite,
                        checkedTrackColor = MoneTrackaColors.MintPrimary,
                        uncheckedThumbColor = MoneTrackaColors.CardWhite,
                        uncheckedTrackColor = MoneTrackaColors.ProgressTrack
                    )
                )
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
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        return@Button
                    }
                    if (selectedCategoryId <= 0) {
                        errorMessage = "Please select a category"
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        return@Button
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val sanitizedNote = noteStr.trim().take(255)
                    onSave(amt, selectedType, selectedCategoryId, selectedAccountId, sanitizedNote, isRecurring, selectedInterval)
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
