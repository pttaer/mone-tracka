package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.monetracka.shared.ui.theme.MoneTrackaColors

private val DECIMAL_REGEX = Regex("""^\d*\.?\d{0,2}$""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustBudgetBottomSheet(
    currentBudget: Double,
    currency: String = "USD",
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var budgetText by remember { mutableStateOf(currentBudget.toInt().toString()) }
    val parsedAmount = budgetText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MoneTrackaColors.SurfaceLevel1,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget Target",
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

            Text(
                text = "Set your total planned outflow target. MoneTracka tracks your progress and warns you when approaching limits.",
                color = Color(0xFF8FA2B6),
                fontSize = 13.sp
            )

            // Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1500, 2000, 2500, 3000, 4000).forEach { preset ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (parsedAmount == preset.toDouble()) MoneTrackaColors.MintPrimary else MoneTrackaColors.SurfaceLevel2)
                            .clickable { budgetText = preset.toString() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${CurrencyFormatter.symbol(currency)}$preset",
                            color = if (parsedAmount == preset.toDouble()) Color(0xFF051A12) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = budgetText,
                onValueChange = { if (it.isEmpty() || it.matches(DECIMAL_REGEX)) budgetText = it },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text(text = CurrencyFormatter.symbol(currency), color = MoneTrackaColors.MintPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoneTrackaColors.MintPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = MoneTrackaColors.SurfaceLevel2,
                    unfocusedContainerColor = MoneTrackaColors.SurfaceLevel2
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = { onSave(parsedAmount.coerceAtLeast(10.0)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoneTrackaColors.MintPrimary,
                    contentColor = Color(0xFF051A12)
                )
            ) {
                Text(
                    text = "Save Budget Target",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
