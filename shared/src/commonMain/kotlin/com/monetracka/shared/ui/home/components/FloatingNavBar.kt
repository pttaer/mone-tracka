package com.monetracka.shared.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.ui.theme.MoneTrackaColors

private data class NavItemData(val index: Int, val label: String, val icon: ImageVector)

@Composable
fun FloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsLeft = listOf(
        NavItemData(0, "Overview", Icons.Default.Dashboard),
        NavItemData(1, "Analytics", Icons.Default.PieChart)
    )
    val itemsRight = listOf(
        NavItemData(2, "Budgets", Icons.Default.AccountBalanceWallet),
        NavItemData(3, "Settings", Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Glass Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MoneTrackaColors.SurfaceGlass)
                .border(1.dp, MoneTrackaColors.BorderGlassLuminous, RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsLeft.forEach { item ->
                    NavButton(item = item, isSelected = selectedTab == item.index, onSelect = { onTabSelected(item.index) })
                }
            }

            Spacer(modifier = Modifier.width(60.dp)) // Space for center FAB

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsRight.forEach { item ->
                    NavButton(item = item, isSelected = selectedTab == item.index, onSelect = { onTabSelected(item.index) })
                }
            }
        }

        // Center Elevated Glow FAB
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-10).dp)
                .size(54.dp)
                .shadow(12.dp, CircleShape, ambientColor = MoneTrackaColors.MintPrimary, spotColor = MoneTrackaColors.MintPrimary)
                .clip(CircleShape)
                .background(MoneTrackaColors.MintGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenAdd
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                tint = Color(0xFF051A12),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun NavButton(
    item: NavItemData,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MoneTrackaColors.MintPrimary else MoneTrackaColors.TextMuted,
        animationSpec = spring()
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = item.label,
            color = iconColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
