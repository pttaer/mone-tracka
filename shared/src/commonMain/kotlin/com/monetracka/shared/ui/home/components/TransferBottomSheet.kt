package com.monetracka.shared.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.util.CurrencyFormatter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DECIMAL_REGEX = Regex("""^\d*\.?\d{0,2}$""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferBottomSheet(
    sourceAccount: Account,
    targetAccount: Account,
    sourceBalance: Double,
    targetBalance: Double,
    currency: String = "USD",
    onDismiss: () -> Unit,
    onConfirmTransfer: (Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("50") }
    var noteText by remember { mutableStateOf("") }
    val transferAmount = amountText.toDoubleOrNull() ?: 0.0

    val projectedSource = sourceBalance - transferAmount
    val projectedTarget = targetBalance + transferAmount
    val isOverdraft = transferAmount > sourceBalance

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
            // Header: From -> To
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transfer Funds",
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

            // Transfer Direction Badge: Source -> Target
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF131F2E))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text(text = "FROM", color = Color(0xFF8FA2B6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = sourceAccount.emoji, fontSize = 18.sp)
                        Text(
                            text = sourceAccount.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = CurrencyFormatter.format(sourceBalance, currency),
                        color = Color(0xFF00D09C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "➔",
                    color = Color(0xFF00D09C),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Target
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(text = "TO", color = Color(0xFF8FA2B6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = targetAccount.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(text = targetAccount.emoji, fontSize = 18.sp)
                    }
                    Text(
                        text = CurrencyFormatter.format(targetBalance, currency),
                        color = Color(0xFF00D09C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }


            // Amount Input Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "TRANSFER AMOUNT", color = Color(0xFF8FA2B6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(DECIMAL_REGEX)) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text(text = CurrencyFormatter.symbol(currency), color = Color(0xFF00D09C), fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D09C),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color(0xFF131F2E),
                        unfocusedContainerColor = Color(0xFF131F2E)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Fast Preset Chips: +$20, +$50, +$100, All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(20, 50, 100).forEach { preset ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF172535))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .clickable {
                                val cur = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (cur + preset).toInt().toString()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+$$preset", color = Color(0xFF00D09C), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // "All" chip
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF172535))
                        .border(1.dp, Color(0xFF00D09C).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            amountText = if (sourceBalance > 0) sourceBalance.toInt().toString() else "0"
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "All", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Live Projected Balances Delta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF101B27))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PROJECTED BALANCES AFTER TRANSFER",
                    color = Color(0xFF54687F),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${sourceAccount.name}: ${CurrencyFormatter.format(projectedSource, currency)}",
                        color = if (isOverdraft) Color(0xFFFFB300) else Color(0xFF8FA2B6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "-${CurrencyFormatter.format(transferAmount, currency)}",
                        color = Color(0xFFFF5A79),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${targetAccount.name}: ${CurrencyFormatter.format(projectedTarget, currency)}",
                        color = Color(0xFF8FA2B6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "+${CurrencyFormatter.format(transferAmount, currency)}",
                        color = Color(0xFF00D09C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }


                if (isOverdraft) {
                    Text(
                        text = "⚠️ Amount exceeds available balance in ${sourceAccount.name}",
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Optional Note / Description
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Note (optional, e.g. ATM withdrawal)", color = Color(0xFF54687F), fontSize = 13.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = Color(0xFF131F2E),
                    unfocusedContainerColor = Color(0xFF131F2E)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Slide to Transfer Confirmation Slider
            SlideToTransferSlider(
                enabled = transferAmount > 0,
                onConfirmed = {
                    onConfirmTransfer(transferAmount, noteText)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SlideToTransferSlider(
    enabled: Boolean,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var trackWidthPx by remember { mutableStateOf(0f) }
    val thumbSizeDp = 48.dp
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val maxDrag = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) Color(0xFF101D2C) else Color(0xFF101D2C).copy(alpha = 0.5f))
            .border(
                width = 1.5.dp,
                color = if (enabled) Color(0xFF00D09C).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(28.dp)
            )
            .onGloballyPositioned { coordinates ->
                trackWidthPx = coordinates.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Background Text
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (enabled) "Slide to Transfer ➔" else "Enter an amount",
                color = if (enabled) Color(0xFF8FA2B6) else Color(0xFF54687F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .padding(4.dp)
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(Color(0xFF00D09C), Color(0xFF00B085)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF54687F), Color(0xFF334354)))
                    }
                )
                .pointerInput(enabled, maxDrag) {
                    if (!enabled || maxDrag <= 0f) return@pointerInput
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount.x).coerceIn(0f, maxDrag)
                            coroutineScope.launch {
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            if (maxDrag > 0f && offsetX.value / maxDrag >= 0.82f) {
                                coroutineScope.launch {
                                    offsetX.animateTo(maxDrag, spring())
                                    onConfirmed()
                                }
                            } else {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring())
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "➔",
                color = Color(0xFF051A12),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
