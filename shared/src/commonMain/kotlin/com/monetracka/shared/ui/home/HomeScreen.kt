package com.monetracka.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.home.components.*
import com.monetracka.shared.ui.theme.CategoryColors
import com.monetracka.shared.ui.transaction.AddTransactionBottomSheet
import com.monetracka.shared.ui.transaction.TransactionListScreen

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<HomeScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var selectedTab by remember { mutableStateOf(0) }
        val overviewListState = rememberLazyListState()
        val analyticsListState = rememberLazyListState()
        val budgetsListState = rememberLazyListState()
        val settingsListState = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B11))
        ) {
            when (selectedTab) {
                0 -> OverviewView(
                    state = state,
                    listState = overviewListState,
                    onOpenAdd = { screenModel.onIntent(HomeIntent.OpenAddTransaction(it)) },
                    onRefreshQuote = { screenModel.onIntent(HomeIntent.RefreshQuote) },
                    onNavigateToAnalytics = { selectedTab = 1 },
                    onNavigateToAllTransactions = { navigator.push(TransactionListScreen()) },
                    onDeleteTransaction = { screenModel.onIntent(HomeIntent.DeleteTransaction(it)) },
                    onInitiateTransfer = { from, to -> screenModel.onIntent(HomeIntent.InitiateTransfer(from, to)) },
                    onAddAccount = { screenModel.onIntent(HomeIntent.OpenAddAccount) }
                )
                1 -> AnalyticsView(
                    state = state,
                    listState = analyticsListState,
                    onOpenAdd = { screenModel.onIntent(HomeIntent.OpenAddTransaction(it)) }
                )
                3 -> BudgetsView(state = state, listState = budgetsListState)
                4 -> SettingsView(listState = settingsListState)
            }

            // Native Material3 Bottom Navigation Bar
            NavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                containerColor = Color(0xFF0C1622).copy(alpha = 0.95f)
            ) {
                val navItems = listOf(
                    "⊞" to "Overview",
                    "∿" to "Analytics",
                    "+" to "Add",
                    "◎" to "Budgets",
                    "⚙" to "Settings"
                )

                navItems.forEachIndexed { i, (icon, label) ->
                    val isAdd = i == 2
                    NavigationBarItem(
                        selected = !isAdd && selectedTab == i,
                        onClick = {
                            if (isAdd) {
                                screenModel.onIntent(HomeIntent.OpenAddTransaction(TransactionType.EXPENSE))
                            } else {
                                selectedTab = i
                            }
                        },
                        icon = {
                            Text(
                                text = icon,
                                fontSize = if (isAdd) 24.sp else 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00D09C),
                            selectedTextColor = Color(0xFF00D09C),
                            unselectedIconColor = Color(0xFF54687F),
                            unselectedTextColor = Color(0xFF54687F),
                            indicatorColor = if (isAdd) Color(0xFF00D09C).copy(alpha = 0.2f) else Color(0xFF172535)
                        )
                    )
                }
            }

            // Quick Add Bottom Sheet
            AddTransactionBottomSheet(
                isOpen = state.isAddSheetOpen,
                categories = state.categories.values.toList(),
                initialType = state.addSheetInitialType,
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

            // Tactile Transfer Bottom Sheet
            if (state.isTransferSheetOpen && state.transferSourceAccount != null && state.transferTargetAccount != null) {
                val sourceBalance = state.accounts.firstOrNull { it.account.id == state.transferSourceAccount?.id }?.balance ?: 0.0
                val targetBalance = state.accounts.firstOrNull { it.account.id == state.transferTargetAccount?.id }?.balance ?: 0.0
                TransferBottomSheet(
                    sourceAccount = state.transferSourceAccount!!,
                    targetAccount = state.transferTargetAccount!!,
                    sourceBalance = sourceBalance,
                    targetBalance = targetBalance,
                    onDismiss = { screenModel.onIntent(HomeIntent.DismissTransfer) },
                    onConfirmTransfer = { amount, note ->
                        screenModel.onIntent(
                            HomeIntent.ExecuteTransfer(
                                fromAccountId = state.transferSourceAccount!!.id,
                                toAccountId = state.transferTargetAccount!!.id,
                                amount = amount,
                                note = note
                            )
                        )
                    }
                )
            }
        }
    }


    @Composable
    private fun OverviewView(
        state: HomeUiState,
        listState: LazyListState,
        onOpenAdd: (TransactionType) -> Unit,
        onRefreshQuote: () -> Unit,
        onNavigateToAnalytics: () -> Unit,
        onNavigateToAllTransactions: () -> Unit,
        onDeleteTransaction: (Long) -> Unit,
        onInitiateTransfer: (com.monetracka.shared.domain.model.Account, com.monetracka.shared.domain.model.Account) -> Unit,
        onAddAccount: () -> Unit
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Header Bar
            item(key = "user_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
            item(key = "balance_hero") {
                BalanceHeroCard(
                    balance = state.totalBalance,
                    trendPercent = state.monthlyTrendPercent,
                    sparklinePoints = state.sparklinePoints
                )
            }

            // Accounts Strip with Drag & Drop
            if (state.accounts.isNotEmpty()) {
                item(key = "accounts_strip") {
                    AccountStrip(
                        accounts = state.accounts,
                        onInitiateTransfer = onInitiateTransfer,
                        onAddAccount = onAddAccount
                    )
                }
            }


            // Smart Saving Coach Card
            state.coachInsight?.let { insight ->
                item(key = "smart_coach") {
                    SmartCoachCard(insight = insight)
                }
            }

            // Daily Financial Wisdom Quote Card
            state.financialQuote?.let { quote ->
                item(key = "daily_quote") {
                    DailyQuoteCard(
                        quote = quote,
                        onRefresh = onRefreshQuote
                    )
                }
            }

            // Quick Action Bar (Add, Income, Scan, More)
            item(key = "quick_actions") {
                QuickActionBar(
                    onAddExpense = { onOpenAdd(TransactionType.EXPENSE) },
                    onIncome = { onOpenAdd(TransactionType.INCOME) },
                    onScan = { onOpenAdd(TransactionType.EXPENSE) },
                    onMore = onNavigateToAnalytics
                )
            }

            // Spending Categories Snapshot Header
            item(key = "categories_header") {
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
                        color = Color(0xFF00D09C),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onNavigateToAnalytics)
                            .padding(4.dp)
                    )
                }
            }

            item(key = "categories_chart") {
                CategoryInsightsCard(categorySpends = state.categorySpends)
            }

            // Recent Transactions Header
            item(key = "transactions_header") {
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
                        color = Color(0xFF00D09C),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onNavigateToAllTransactions)
                            .padding(4.dp)
                    )
                }
            }

            // Recycled Transaction Rows (0 jank on scroll)
            if (state.recentTransactions.isEmpty() && !state.isLoading) {
                item(key = "empty_transactions") {
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
                }
            } else {
                items(
                    items = state.recentTransactions,
                    key = { it.id }
                ) { tx ->
                    val cat = state.categories[tx.categoryId]
                    TransactionRow(
                        tx = tx,
                        category = cat,
                        onDelete = onDeleteTransaction
                    )
                }
            }
        }
    }

    @Composable
    private fun AnalyticsView(
        state: HomeUiState,
        listState: LazyListState,
        onOpenAdd: (TransactionType) -> Unit
    ) {
        val totalExpense = remember(state.categorySpends) { state.categorySpends.sumOf { it.amount } }
        val totalIncome = remember(state.recentTransactions) {
            state.recentTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "analytics_title") {
                Text(
                    text = "Spending Analytics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item(key = "inflow_outflow") {
                // Income vs Expense Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF172535))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Inflow", color = Color(0xFF8FA2B6), fontSize = 12.sp)
                        Text(
                            "+$${CurrencyFormatter.format(totalIncome)}",
                            color = Color(0xFF00D09C),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Outflow", color = Color(0xFF8FA2B6), fontSize = 12.sp)
                        Text(
                            "-$${CurrencyFormatter.format(totalExpense)}",
                            color = Color(0xFFFF5A79),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item(key = "analytics_chart") {
                CategoryInsightsCard(categorySpends = state.categorySpends)
            }

            item(key = "breakdown_title") {
                Text(
                    text = "Category Breakdown (${state.categorySpends.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (state.categorySpends.isEmpty()) {
                item(key = "empty_categories") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No expense categories recorded yet", color = Color(0xFF8FA2B6), fontSize = 13.sp)
                    }
                }
            } else {
                items(
                    items = state.categorySpends,
                    key = { it.category }
                ) { cat ->
                    val catColor = CategoryColors.getOrElse(cat.colorIndex) { Color(cat.colorHex) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF172535))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = cat.category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(text = "$${CurrencyFormatter.format(cat.amount)} (%.1f%%)".format(cat.percentage), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = catColor)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(fraction = (cat.percentage.toFloat() / 100f).coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(catColor)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BudgetsView(state: HomeUiState, listState: LazyListState) {
        val totalSpent = remember(state.categorySpends) { state.categorySpends.sumOf { it.amount } }
        val monthlyBudget = 2500.0
        val remaining = remember(totalSpent) { (monthlyBudget - totalSpent).coerceAtLeast(0.0) }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "budgets_title") {
                Text(text = "Monthly Budgets", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            item(key = "budget_card") {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF172535)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monthly Limit", color = Color(0xFF8FA2B6), fontSize = 12.sp)
                        Text("Remaining: $${CurrencyFormatter.format(remaining)}", color = if (remaining > 0) Color(0xFF00D09C) else Color(0xFFFF5A79), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "$${CurrencyFormatter.format(totalSpent)} / $${CurrencyFormatter.format(monthlyBudget)}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.08f))) {
                        Box(modifier = Modifier.fillMaxWidth(fraction = (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(Color(0xFF00D09C)))
                    }
                }
            }
            item(key = "limits_title") {
                Text(text = "Category Limits", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            items(
                items = state.categorySpends,
                key = { it.category }
            ) { cat ->
                val catColor = CategoryColors.getOrElse(cat.colorIndex) { Color(cat.colorHex) }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF172535)).padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cat.category, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("$${CurrencyFormatter.format(cat.amount)} / $500", color = catColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun SettingsView(listState: LazyListState) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "settings_title") {
                Text(text = "Settings & Preferences", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            item(key = "currency_section") {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF172535)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Currency", color = Color.White, fontSize = 14.sp)
                        Text("USD ($)", color = Color(0xFF00D09C), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Version", color = Color.White, fontSize = 14.sp)
                        Text("MoneTracka 1.0.0", color = Color(0xFF8FA2B6), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
