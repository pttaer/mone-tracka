package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.monetracka.shared.domain.util.CurrencyFormatter

private val ACCOUNT_EMOJIS = listOf("🏦", "💵", "💳", "🪙", "🎯", "💼", "📈", "🏝️", "👛", "🛡️")
private val DECIMAL_REGEX = Regex("""^\d*\.?\d{0,2}$""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountBottomSheet(
    currency: String = "USD",
    onDismiss: () -> Unit,
    onCreateAccount: (name: String, emoji: String, initialBalance: Double, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedEmoji by remember { mutableStateOf(ACCOUNT_EMOJIS.first()) }
    var nameText by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    val canCreate = nameText.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0C1622),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { onDismiss() }
                ) {
                    Text(text = "✕", color = Color(0xFF8FA2B6), fontSize = 14.sp)
                }
            }

            // Emoji Selector Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "CHOOSE ICON",
                    color = Color(0xFF8FA2B6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ACCOUNT_EMOJIS) { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFF00D09C).copy(alpha = 0.2f) else Color(0xFF131F2E))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00D09C) else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedEmoji = emoji }
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            }

            // Account Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "ACCOUNT NAME", color = Color(0xFF8FA2B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Main Checking, Crypto, Emergency", color = Color(0xFF54687F), fontSize = 14.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D09C),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF131F2E),
                        unfocusedContainerColor = Color(0xFF131F2E)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Starting / Initial Balance
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "STARTING BALANCE", color = Color(0xFF8FA2B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(DECIMAL_REGEX)) {
                            balanceText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(text = CurrencyFormatter.symbol(currency), color = Color(0xFF00D09C), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0.00", color = Color(0xFF54687F), fontSize = 15.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D09C),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF131F2E),
                        unfocusedContainerColor = Color(0xFF131F2E)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Description / Note (optional)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "DESCRIPTION (OPTIONAL)", color = Color(0xFF8FA2B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Short description or purpose", color = Color(0xFF54687F), fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D09C),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedContainerColor = Color(0xFF131F2E),
                        unfocusedContainerColor = Color(0xFF131F2E)
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Create Button
            Button(
                onClick = {
                    val initialBal = balanceText.toDoubleOrNull() ?: 0.0
                    onCreateAccount(nameText.trim(), selectedEmoji, initialBal, descriptionText.trim())
                },
                enabled = canCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D09C),
                    disabledContainerColor = Color(0xFF172535),
                    contentColor = Color(0xFF051A12),
                    disabledContentColor = Color(0xFF54687F)
                )
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
