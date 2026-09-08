package com.monetracka.shared.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.home.AccountUiModel
import kotlin.math.roundToInt

@Composable
fun AccountStrip(
    accounts: List<AccountUiModel>,
    onInitiateTransfer: (Account, Account) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingAccountId by remember { mutableStateOf<Long?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var touchRootPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetAccountId by remember { mutableStateOf<Long?>(null) }
    val cardBounds = remember { mutableStateMapOf<Long, Rect>() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ACCOUNTS",
                    color = Color(0xFF8FA2B6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "• Drag card onto another to transfer",
                    color = Color(0xFF00D09C).copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${accounts.size} Active",
                color = Color(0xFF54687F),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(accounts, key = { it.account.id }) { item ->
                val isDragging = draggingAccountId == item.account.id
                val isHoveredTarget = hoveredTargetAccountId == item.account.id

                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.06f else if (isHoveredTarget) 1.04f else 1f,
                    animationSpec = spring()
                )
                val rotation by animateFloatAsState(
                    targetValue = if (isDragging) -3f else 0f,
                    animationSpec = spring()
                )

                AccountCard(
                    item = item,
                    isDragging = isDragging,
                    isHoveredTarget = isHoveredTarget,
                    dragOffset = if (isDragging) dragDelta else Offset.Zero,
                    scale = scale,
                    rotation = rotation,
                    onPositioned = { rect -> cardBounds[item.account.id] = rect },
                    onDragStart = { startOffset ->
                        draggingAccountId = item.account.id
                        dragDelta = Offset.Zero
                        cardBounds[item.account.id]?.let {
                            touchRootPosition = it.topLeft + startOffset
                        }
                    },
                    onDrag = { amount ->
                        dragDelta += amount
                        touchRootPosition += amount
                        // Find which other card contains touchRootPosition
                        val target = cardBounds.entries.firstOrNull { (id, bounds) ->
                            id != item.account.id && bounds.contains(touchRootPosition)
                        }
                        hoveredTargetAccountId = target?.key
                    },
                    onDragEnd = {
                        val targetId = hoveredTargetAccountId
                        val sourceAccount = item.account
                        val targetAccount = accounts.firstOrNull { it.account.id == targetId }?.account
                        if (targetAccount != null && sourceAccount.id != targetAccount.id) {
                            onInitiateTransfer(sourceAccount, targetAccount)
                        }
                        draggingAccountId = null
                        dragDelta = Offset.Zero
                        hoveredTargetAccountId = null
                    },
                    onDragCancel = {
                        draggingAccountId = null
                        dragDelta = Offset.Zero
                        hoveredTargetAccountId = null
                    }
                )
            }

            // + Add Account Card
            item(key = "add_account_button") {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(118.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF101C2A))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onAddAccount() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                        ) {
                            Text(
                                text = "+",
                                color = Color(0xFF00D09C),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Add Account",
                            color = Color(0xFF8FA2B6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    item: AccountUiModel,
    isDragging: Boolean,
    isHoveredTarget: Boolean,
    dragOffset: Offset,
    scale: Float,
    rotation: Float,
    onPositioned: (Rect) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isHoveredTarget -> Color(0xFF00D09C)
        isDragging -> Color(0xFF38EF7D)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val borderWidth = if (isHoveredTarget || isDragging) 2.dp else 1.dp

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 10f else 1f)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .scale(scale)
            .rotate(rotation)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot())
            }
            .pointerInput(item.account.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> onDragStart(offset) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
            .width(140.dp)
            .height(118.dp)
            .shadow(
                elevation = if (isDragging) 16.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF00D09C),
                spotColor = Color(0xFF00D09C)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isHoveredTarget) {
                        listOf(Color(0xFF132A24), Color(0xFF0E1F1B))
                    } else {
                        listOf(Color(0xFF131F2E), Color(0xFF0C1622))
                    }
                )
            )
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    Text(text = item.account.emoji, fontSize = 17.sp)
                }

                if (isHoveredTarget) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00D09C))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DROP HERE",
                            color = Color(0xFF051A12),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.account.name,
                    color = Color(0xFF8FA2B6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = "$${CurrencyFormatter.format(item.balance)}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
