package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    isOpen: Boolean,
    categories: List<com.monetracka.shared.domain.model.Category>,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, Long, String) -> Unit
) {
    if (!isOpen) return

    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C1622),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(text = "Log Transaction", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.height(16.dp))

            // Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF172535))
                    .padding(4.dp)
            ) {
                listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedType == type) Color(0xFF00D09C) else Color.Transparent)
                            .clickable { selectedType = type }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selectedType == type) Color(0xFF022015) else Color(0xFF8FA2B6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount ($)", color = Color(0xFF8FA2B6)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color(0xFF243447)
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = noteStr,
                onValueChange = { noteStr = it },
                label = { Text("Note / Description", color = Color(0xFF8FA2B6)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color(0xFF243447)
                )
            )

            Spacer(Modifier.height(14.dp))

            Text(text = "Category", color = Color(0xFF8FA2B6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                categories.take(4).forEach { cat ->
                    val isSel = selectedCategoryId == cat.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFF00D09C).copy(alpha = 0.2f) else Color(0xFF172535))
                            .clickable { selectedCategoryId = cat.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${cat.emoji} ${cat.name}",
                            fontSize = 11.sp,
                            color = if (isSel) Color(0xFF00D09C) else Color(0xFF8FA2B6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSave(amt, selectedType, selectedCategoryId, noteStr)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D09C)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Confirm Transaction", color = Color(0xFF022015), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
