package com.monetracka.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.monetracka.shared.ui.home.components.*
import com.monetracka.shared.ui.transaction.AddTransactionBottomSheet
import com.monetracka.shared.ui.transaction.AddTransactionScreen

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<HomeScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B11))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(top = 44.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Header Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF00D09C))
                            ) {
                                Text(
                                    text = "MT",
                                    color = Color(0xFF051A12),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "TOTAL WEALTH",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8FA2B6),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Alexandre Chen",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF172535))
                        ) {
                            Text(text = "🔔", fontSize = 16.sp)
                        }
                    }
                }

                // Balance Hero Card with Live Sparkline
                item {
                    BalanceHeroCard(
                        balance = state.totalBalance,
                        trendPercent = state.monthlyTrendPercent,
                        sparklinePoints = state.sparklinePoints
                    )
                }

                // Quick Action Bar (Add, Income, Scan, More)
                item {
                    QuickActionBar(
                        onAddExpense = { screenModel.onIntent(HomeIntent.OpenAddTransaction) },
                        onIncome = { navigator.push(AddTransactionScreen()) },
                        onScan = { screenModel.onIntent(HomeIntent.OpenAddTransaction) },
                        onMore = { screenModel.onIntent(HomeIntent.OpenAddTransaction) }
                    )
                }

                // Spending Categories Snapshot
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spending Categories",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Report →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00D09C)
                        )
                    }
                }

                item {
                    CategoryInsightsCard(categorySpends = state.categorySpends)
                }

                // Recent Transactions Feed
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "See All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00D09C)
                        )
                    }
                }

                item {
                    if (state.recentTransactions.isEmpty() && !state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions yet. Tap + to add!",
                                color = Color(0xFF8FA2B6),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        TransactionFeed(transactions = state.recentTransactions)
                    }
                }
            }

            // Native Material3 Bottom Navigation Bar
            NavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                containerColor = Color(0xFF0C1622).copy(alpha = 0.95f)
            ) {
                listOf("⊞" to "Overview", "∿" to "Analytics", "+" to "Add", "◎" to "Budgets", "⚙" to "Settings").forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = i == 0,
                        onClick = { if (i == 2) screenModel.onIntent(HomeIntent.OpenAddTransaction) },
                        icon = { Text(icon, fontSize = if (i == 2) 22.sp else 18.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00D09C),
                            selectedTextColor = Color(0xFF00D09C),
                            unselectedIconColor = Color(0xFF54687F),
                            unselectedTextColor = Color(0xFF54687F),
                            indicatorColor = Color(0xFF172535)
                        )
                    )
                }
            }

            // Quick Add Bottom Sheet
            AddTransactionBottomSheet(
                isOpen = state.isAddSheetOpen,
                categories = state.categories.values.toList(),
                onDismiss = { screenModel.onIntent(HomeIntent.DismissAddTransaction) },
                onSave = { amount, type, categoryId, note ->
                    screenModel.onIntent(
                        HomeIntent.CreateTransaction(
                            amount = amount,
                            type = type,
                            categoryId = categoryId,
                            note = note
                        )
                    )
                }
            )
        }
    }
}
