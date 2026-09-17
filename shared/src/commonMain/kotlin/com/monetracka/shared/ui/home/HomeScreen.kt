package com.monetracka.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.home.components.*
import com.monetracka.shared.domain.security.BiometricAuthManager
import com.monetracka.shared.ui.theme.CategoryColors
import com.monetracka.shared.ui.theme.MoneTrackaColors
import com.monetracka.shared.ui.transaction.AddTransactionBottomSheet
import com.monetracka.shared.ui.transaction.TransactionListScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
                .background(MoneTrackaColors.BackgroundLight)
        ) {
            when (selectedTab) {
                0 -> OverviewView(
                    state = state,
                    listState = overviewListState,
                    onOpenAdd = { screenModel.onIntent(HomeIntent.OpenAddTransaction(it)) },
                    onRefreshQuote = { screenModel.onIntent(HomeIntent.RefreshQuote) },
                    onToggleHideBalance = { screenModel.onIntent(HomeIntent.ToggleHideBalance) },
                    onNavigateToAnalytics = { selectedTab = 1 },
                    onNavigateToAllTransactions = { navigator.push(TransactionListScreen()) },
                    onDeleteTransaction = { screenModel.onIntent(HomeIntent.DeleteTransaction(it)) },
                    onUpdateTransaction = { screenModel.onIntent(HomeIntent.UpdateTransaction(it)) },
                    onInitiateTransfer = { from, to -> screenModel.onIntent(HomeIntent.InitiateTransfer(from, to)) },
                    onAddAccount = { screenModel.onIntent(HomeIntent.OpenAddAccount) }
                )
                1 -> AnalyticsView(
                    state = state,
                    listState = analyticsListState,
                    onOpenAdd = { screenModel.onIntent(HomeIntent.OpenAddTransaction(it)) }
                )
                2 -> BudgetsView(
                    state = state,
                    listState = budgetsListState,
                    onOpenAdjustBudget = { screenModel.onIntent(HomeIntent.OpenAdjustBudget) },
                    onOpenAdjustCategoryBudget = { screenModel.onIntent(HomeIntent.OpenAdjustCategoryBudget(it)) },
                    onDeleteRecurring = { screenModel.onIntent(HomeIntent.DeleteRecurring(it)) }
                )
                3 -> SettingsView(
                    state = state,
                    listState = settingsListState,
                    onSelectCurrency = { newCurr -> screenModel.onIntent(HomeIntent.UpdateCurrency(newCurr)) },
                    onImportTransactions = { screenModel.onIntent(HomeIntent.ImportTransactions(it)) },
                    onToggleBiometrics = { enabled -> screenModel.onIntent(HomeIntent.SetBiometricEnabled(enabled)) }
                )
            }

            // Floating Navigation Bar Dock
            FloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onOpenAdd = { screenModel.onIntent(HomeIntent.OpenAddTransaction(TransactionType.EXPENSE)) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Quick Add Bottom Sheet
            AddTransactionBottomSheet(
                isOpen = state.isAddSheetOpen,
                categories = state.categories.values.toList(),
                accounts = state.accounts,
                initialType = state.addSheetInitialType,
                currency = state.currency,
                onDismiss = { screenModel.onIntent(HomeIntent.DismissAddTransaction) },
                onCreateCategory = { name, emoji, type ->
                    screenModel.onIntent(HomeIntent.CreateCategory(name, emoji, type))
                },
                onSave = { amount, type, categoryId, accountId, note, isRecurring, interval, dateMillis ->
                    screenModel.onIntent(
                        HomeIntent.CreateTransaction(
                            amount = amount,
                            type = type,
                            categoryId = categoryId,
                            accountId = accountId,
                            note = note,
                            isRecurring = isRecurring,
                            recurringInterval = interval,
                            dateMillis = dateMillis
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
                    currency = state.currency,
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

            // Create Account Bottom Sheet
            if (state.isAddAccountSheetOpen) {
                AddAccountBottomSheet(
                    currency = state.currency,
                    onDismiss = { screenModel.onIntent(HomeIntent.DismissAddAccount) },
                    onCreateAccount = { name, emoji, balance, desc ->
                        screenModel.onIntent(
                            HomeIntent.CreateAccount(
                                name = name,
                                emoji = emoji,
                                initialBalance = balance,
                                description = desc
                            )
                        )
                    }
                )
            }

            // Adjust Monthly Budget Target Bottom Sheet
            if (state.isAdjustBudgetOpen) {
                AdjustBudgetBottomSheet(
                    currentBudget = state.monthlyBudgetLimit,
                    currency = state.currency,
                    onDismiss = { screenModel.onIntent(HomeIntent.DismissAdjustBudget) },
                    onSave = { newLimit ->
                        screenModel.onIntent(HomeIntent.UpdateMonthlyBudget(newLimit))
                    }
                )
            }

            // Adjust Category Budget Target Dialog
            if (state.isAdjustCategoryBudgetOpen && state.selectedCategoryBudgetSpend != null) {
                val catSpend = state.selectedCategoryBudgetSpend!!
                val catId = state.categories.entries.firstOrNull { it.value.name.equals(catSpend.category, ignoreCase = true) }?.key ?: 1L
                val currentCatLimit = catSpend.budgetLimit ?: (state.monthlyBudgetLimit * (catSpend.percentage / 100.0).coerceAtLeast(0.05))
                var limitInput by remember(catSpend) { mutableStateOf(currentCatLimit.toLong().toString()) }

                AlertDialog(
                    onDismissRequest = { screenModel.onIntent(HomeIntent.DismissAdjustCategoryBudget) },
                    title = {
                        Text("Adjust ${catSpend.category} Budget", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Set monthly spending limit for ${catSpend.category}:",
                                color = MoneTrackaColors.TextGray,
                                fontSize = 12.sp
                            )
                            OutlinedTextField(
                                value = limitInput,
                                onValueChange = { limitInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Limit (${state.currency})") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MoneTrackaColors.TextDark,
                                    unfocusedTextColor = MoneTrackaColors.TextDark,
                                    focusedBorderColor = MoneTrackaColors.MintPrimary,
                                    unfocusedBorderColor = MoneTrackaColors.ProgressTrack
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val parsedLimit = limitInput.toDoubleOrNull() ?: currentCatLimit
                                screenModel.onIntent(HomeIntent.UpdateCategoryBudget(catId, parsedLimit))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.MintPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { screenModel.onIntent(HomeIntent.DismissAdjustCategoryBudget) }) {
                            Text("Cancel", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    containerColor = MoneTrackaColors.CardWhite
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
        onToggleHideBalance: () -> Unit,
        onNavigateToAnalytics: () -> Unit,
        onNavigateToAllTransactions: () -> Unit,
        onDeleteTransaction: (Long) -> Unit,
        onUpdateTransaction: (Transaction) -> Unit,
        onInitiateTransfer: (com.monetracka.shared.domain.model.Account, com.monetracka.shared.domain.model.Account) -> Unit,
        onAddAccount: () -> Unit
    ) {
        val accountMap = remember(state.accounts) { state.accounts.associate { it.account.id to it.account } }
        var showNotificationsDialog by remember { mutableStateOf(false) }
        var hasUnreadNotifications by remember { mutableStateOf(true) }
        var showScanBillDialog by remember { mutableStateOf(false) }
        var selectedOverviewTx by remember { mutableStateOf<Transaction?>(null) }

        TransactionDetailBottomSheet(
            isOpen = selectedOverviewTx != null,
            tx = selectedOverviewTx,
            category = selectedOverviewTx?.let { state.categories[it.categoryId] },
            accounts = accountMap,
            categories = state.categories.values.toList(),
            currency = state.currency,
            onDismiss = { selectedOverviewTx = null },
            onDelete = {
                onDeleteTransaction(it)
                selectedOverviewTx = null
            },
            onUpdate = {
                onUpdateTransaction(it)
                selectedOverviewTx = null
            }
        )

        if (showNotificationsDialog) {
            val totalExpense = state.categorySpends.sumOf { it.amount }
            AlertDialog(
                onDismissRequest = {
                    hasUnreadNotifications = false
                    showNotificationsDialog = false
                },
                title = {
                    Text(
                        text = "Notifications & Activity",
                        color = MoneTrackaColors.TextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneTrackaColors.MintLight)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("💡", fontSize = 18.sp)
                            Column {
                                Text("Smart Financial Insight", color = MoneTrackaColors.MintDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = state.coachInsight?.description ?: "Keep tracking daily transactions to build accurate forecasting trends.",
                                    color = MoneTrackaColors.TextDark,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (totalExpense > state.monthlyBudgetLimit) MoneTrackaColors.CoralDanger.copy(alpha = 0.12f) else MoneTrackaColors.SurfaceSecondary)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(if (totalExpense > state.monthlyBudgetLimit) "⚠️" else "📊", fontSize = 18.sp)
                            Column {
                                Text("Monthly Budget Monitor", color = if (totalExpense > state.monthlyBudgetLimit) MoneTrackaColors.CoralDanger else MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "Spent ${CurrencyFormatter.format(totalExpense, state.currency)} of ${CurrencyFormatter.format(state.monthlyBudgetLimit, state.currency)} limit.",
                                    color = MoneTrackaColors.TextGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            hasUnreadNotifications = false
                            showNotificationsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.MintPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Mark Read & Close", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = MoneTrackaColors.CardWhite
            )
        }

        if (showScanBillDialog) {
            AlertDialog(
                onDismissRequest = { showScanBillDialog = false },
                title = {
                    Text("Scan Bill (OCR)", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text(
                        text = "Camera OCR scanner automatically parses receipts, vendor names, and totals directly into transactions. Would you like to log this expense manually in the meantime?",
                        color = MoneTrackaColors.TextGray,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showScanBillDialog = false
                            onOpenAdd(TransactionType.EXPENSE)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.MintPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enter Manually", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showScanBillDialog = false }) {
                        Text("Dismiss", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = MoneTrackaColors.CardWhite
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 108.dp),
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
                                .background(MoneTrackaColors.MintPrimary)
                        ) {
                            Text(
                                text = state.userInitials,
                                color = Color(0xFF051A12),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        Column {
                            Text(
                                text = "TOTAL WEALTH",
                                fontSize = 11.sp,
                                color = MoneTrackaColors.TextGray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = state.userName,
                                fontSize = 16.sp,
                                color = MoneTrackaColors.TextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vector Notification Icon with touch target and status dot
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = MoneTrackaColors.CardShadowColor)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MoneTrackaColors.CardWhite)
                            .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(14.dp))
                            .clickable { showNotificationsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MoneTrackaColors.TextDark,
                            modifier = Modifier.size(20.dp)
                        )
                        if (hasUnreadNotifications) {
                            // Notification badge indicator
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MoneTrackaColors.MintPrimary)
                            )
                        }
                    }
                }
            }

            // Balance Hero Card with Live Sparkline
            item(key = "balance_hero") {
                BalanceHeroCard(
                    balance = state.totalBalance,
                    trendPercent = state.monthlyTrendPercent,
                    sparklinePoints = state.sparklinePoints,
                    currency = state.currency,
                    isBalanceHidden = state.isBalanceHidden,
                    onToggleHideBalance = onToggleHideBalance
                )
            }

            // Accounts Strip with Drag & Drop
            if (state.accounts.isNotEmpty()) {
                item(key = "accounts_strip") {
                    AccountStrip(
                        accounts = state.accounts,
                        onInitiateTransfer = onInitiateTransfer,
                        onAddAccount = onAddAccount,
                        currency = state.currency,
                        isBalanceHidden = state.isBalanceHidden
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
                    onScan = { showScanBillDialog = true },
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
                        color = MoneTrackaColors.TextDark
                    )
                    Text(
                        text = "Report →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneTrackaColors.MintDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToAnalytics)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            item(key = "categories_chart") {
                CategoryInsightsCard(
                    categorySpends = state.categorySpends,
                    currency = state.currency
                )
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
                        color = MoneTrackaColors.TextDark
                    )
                    Text(
                        text = "See All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoneTrackaColors.MintDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateToAllTransactions)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Recycled Transaction Rows
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
                            color = MoneTrackaColors.TextGray,
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
                        accounts = accountMap,
                        currency = state.currency,
                        onClick = { selectedOverviewTx = tx },
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
        val totalExpense = state.totalExpense
        val totalIncome = state.totalIncome

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "analytics_title") {
                Text(
                    text = "Spending Analytics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneTrackaColors.TextDark
                )
            }

            item(key = "inflow_outflow") {
                // Income vs Expense Card
                val netSavings = totalIncome - totalExpense
                val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100.0).coerceIn(-100.0, 100.0) else 0.0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Inflow", color = MoneTrackaColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "+${CurrencyFormatter.format(totalIncome, state.currency)}",
                                color = MoneTrackaColors.MintDark,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Outflow", color = MoneTrackaColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "-${CurrencyFormatter.format(totalExpense, state.currency)}",
                                color = MoneTrackaColors.CoralDanger,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // MoM Comparative Bar & Net Savings Rate
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MoneTrackaColors.ProgressTrack)
                    ) {
                        val totalFlow = (totalIncome + totalExpense).coerceAtLeast(1.0)
                        val incomeFrac = (totalIncome / totalFlow).toFloat().coerceIn(0f, 1f)
                        Row(Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = incomeFrac)
                                    .fillMaxHeight()
                                    .background(MoneTrackaColors.MintPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MoneTrackaColors.CoralDanger)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net: ${if (netSavings >= 0) "+" else ""}${CurrencyFormatter.format(netSavings, state.currency)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netSavings >= 0) MoneTrackaColors.MintDark else MoneTrackaColors.CoralDanger
                        )
                        Text(
                            text = "Savings Rate: %.1f%%".format(savingsRate),
                            fontSize = 12.sp,
                            color = MoneTrackaColors.TextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item(key = "net_worth_sparkline") {
                val accountPoints = remember(state.accounts) {
                    val base = state.totalBalance.toFloat()
                    listOf(base * 0.92f, base * 0.95f, base * 0.91f, base * 0.97f, base * 0.94f, base)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Net Worth", color = MoneTrackaColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(
                                CurrencyFormatter.format(state.totalBalance, state.currency),
                                color = MoneTrackaColors.TextDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MoneTrackaColors.MintLight)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("30D Trend", color = MoneTrackaColors.MintDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    SparklineChart(
                        points = accountPoints,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        lineColor = MoneTrackaColors.MintPrimary
                    )
                }
            }

            item(key = "analytics_chart") {
                CategoryInsightsCard(
                    categorySpends = state.categorySpends,
                    currency = state.currency
                )
            }

            item(key = "breakdown_title") {
                Text(
                    text = "Category Breakdown (${state.categorySpends.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneTrackaColors.TextDark
                )
            }

            if (state.categorySpends.isEmpty()) {
                item(key = "empty_categories") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No expense categories recorded yet", color = MoneTrackaColors.TextGray, fontSize = 13.sp)
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
                            .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = MoneTrackaColors.CardShadowColor)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MoneTrackaColors.CardWhite)
                            .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = cat.category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MoneTrackaColors.TextDark)
                            Text(text = "${CurrencyFormatter.format(cat.amount, state.currency)} (%.1f%%)".format(cat.percentage), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = catColor)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(MoneTrackaColors.ProgressTrack)
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
    private fun BudgetsView(
        state: HomeUiState,
        listState: LazyListState,
        onOpenAdjustBudget: () -> Unit,
        onOpenAdjustCategoryBudget: (CategorySpend) -> Unit,
        onDeleteRecurring: (Long) -> Unit
    ) {
        val totalSpent = remember(state.categorySpends) { state.categorySpends.sumOf { it.amount } }
        val monthlyBudget = state.monthlyBudgetLimit
        val remaining = remember(totalSpent, monthlyBudget) { (monthlyBudget - totalSpent).coerceAtLeast(0.0) }
        val burnRateFraction = remember(totalSpent, monthlyBudget) {
            if (monthlyBudget > 0) (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f) else 1f
        }
        val burnStatus = when {
            totalSpent > monthlyBudget -> "Over Budget"
            totalSpent > monthlyBudget * 0.8 -> "Approaching Limit"
            else -> "On Track"
        }
        val burnColor = when {
            totalSpent > monthlyBudget -> MoneTrackaColors.CoralDanger
            totalSpent > monthlyBudget * 0.8 -> Color(0xFFFF9800)
            else -> MoneTrackaColors.MintDark
        }
        val burnBadgeBg = when {
            totalSpent > monthlyBudget -> Color(0xFFFFEBEE)
            totalSpent > monthlyBudget * 0.8 -> Color(0xFFFFF3E0)
            else -> MoneTrackaColors.MintLight
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "budgets_title") {
                Text(text = "Monthly Budgets", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MoneTrackaColors.TextDark)
            }
            item(key = "budget_card") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(22.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(22.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Target", color = MoneTrackaColors.TextGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(burnBadgeBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = burnStatus,
                                    color = burnColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MoneTrackaColors.SurfaceSecondary)
                                .clickable { onOpenAdjustBudget() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Adjust Target", color = MoneTrackaColors.MintDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "${CurrencyFormatter.format(totalSpent, state.currency)} / ${CurrencyFormatter.format(monthlyBudget, state.currency)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MoneTrackaColors.TextDark
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MoneTrackaColors.ProgressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = burnRateFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(burnColor)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining",
                            color = MoneTrackaColors.TextGray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = CurrencyFormatter.format(remaining, state.currency),
                            color = if (remaining > 0) MoneTrackaColors.MintDark else MoneTrackaColors.CoralDanger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item(key = "subscriptions_section") {
                val monthlyBurn = remember(state.recurringTransactions) {
                    state.recurringTransactions.sumOf { rec ->
                        when (rec.intervalType) {
                            com.monetracka.shared.domain.model.RecurringInterval.DAILY -> rec.amount * 30.0
                            com.monetracka.shared.domain.model.RecurringInterval.WEEKLY -> rec.amount * 4.33
                            com.monetracka.shared.domain.model.RecurringInterval.MONTHLY -> rec.amount
                            com.monetracka.shared.domain.model.RecurringInterval.YEARLY -> rec.amount / 12.0
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Subscriptions & Recurring Bills (${state.recurringTransactions.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MoneTrackaColors.TextDark
                        )
                        Text(
                            text = "Committed monthly burn: ${CurrencyFormatter.format(monthlyBurn, state.currency)}/mo",
                            fontSize = 12.sp,
                            color = MoneTrackaColors.TextGray
                        )
                    }

                    if (state.recurringTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneTrackaColors.SurfaceSecondary)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active recurring bills. Toggle recurring in Add Transaction to track subscriptions here.",
                                color = MoneTrackaColors.TextGray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.recurringTransactions.forEach { rec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MoneTrackaColors.SurfaceSecondary)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MoneTrackaColors.TextDark
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MoneTrackaColors.MintLight)
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = rec.intervalType.name.lowercase().replaceFirstChar { it.uppercase() },
                                                    fontSize = 10.sp,
                                                    color = MoneTrackaColors.MintDark,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "${CurrencyFormatter.format(rec.amount, state.currency)} / cycle",
                                                fontSize = 11.sp,
                                                color = MoneTrackaColors.TextGray
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteRecurring(rec.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel subscription",
                                            tint = MoneTrackaColors.CoralDanger,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "limits_title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Category Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MoneTrackaColors.TextDark)
                    Text(text = "Tap to edit limit", fontSize = 11.sp, color = MoneTrackaColors.TextGray)
                }
            }

            if (state.categorySpends.isEmpty()) {
                item(key = "empty_budgets") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("No expense categories recorded yet", color = MoneTrackaColors.TextGray, fontSize = 13.sp)
                    }
                }
            } else {
                items(
                    items = state.categorySpends,
                    key = { it.category }
                ) { cat ->
                    val catColor = CategoryColors.getOrElse(cat.colorIndex) { Color(cat.colorHex) }
                    val limit = cat.budgetLimit ?: (state.monthlyBudgetLimit * (cat.percentage / 100.0).coerceAtLeast(0.05))
                    val fraction = if (limit > 0.0) (cat.amount / limit).coerceIn(0.0, 1.0).toFloat() else 0f
                    val catPercent = if (limit > 0.0) ((cat.amount / limit) * 100).toInt() else 0
                    val catStatusColor = when {
                        cat.amount > limit -> MoneTrackaColors.CoralDanger
                        cat.amount > limit * 0.8 -> Color(0xFFFF9800)
                        else -> catColor
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = MoneTrackaColors.CardShadowColor)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MoneTrackaColors.CardWhite)
                            .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(14.dp))
                            .clickable { onOpenAdjustCategoryBudget(cat) }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.category, color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${CurrencyFormatter.format(cat.amount, state.currency)} / ${CurrencyFormatter.format(limit, state.currency)} ($catPercent%)",
                                color = catStatusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MoneTrackaColors.ProgressTrack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(catStatusColor)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsView(
        state: HomeUiState,
        listState: LazyListState,
        onSelectCurrency: (String) -> Unit,
        onImportTransactions: (List<Transaction>) -> Unit,
        onToggleBiometrics: (Boolean) -> Unit
    ) {
        val biometricAuthManager = koinInject<BiometricAuthManager>()
        val scope = rememberCoroutineScope()
        var showCurrencyDialog by remember { mutableStateOf(false) }
        val currencyOptions = listOf(
            "USD" to "USD ($)",
            "EUR" to "EUR (€)",
            "GBP" to "GBP (£)",
            "VND" to "VND (₫)",
            "JPY" to "JPY (¥)"
        )
        val currencyLabel = currencyOptions.firstOrNull { it.first == state.currency }?.second ?: "${state.currency} ($)"

        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Select Currency", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        currencyOptions.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (code == state.currency) MoneTrackaColors.MintLight else MoneTrackaColors.SurfaceSecondary)
                                    .clickable {
                                        onSelectCurrency(code)
                                        showCurrencyDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, color = MoneTrackaColors.TextDark, fontSize = 14.sp, fontWeight = if (code == state.currency) FontWeight.Bold else FontWeight.Normal)
                                if (code == state.currency) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MoneTrackaColors.MintDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCurrencyDialog = false }) {
                        Text("Close", color = MoneTrackaColors.MintDark, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = MoneTrackaColors.CardWhite
            )
        }

        var showExportDialog by remember { mutableStateOf(false) }
        var showImportDialog by remember { mutableStateOf(false) }
        var importText by remember { mutableStateOf("") }
        var importValidationMsg by remember { mutableStateOf<String?>(null) }

        if (showExportDialog) {
            val csvContent = remember(state.allTransactions) {
                com.monetracka.shared.domain.export.SimpleCsvExporter.exportTransactions(state.allTransactions)
            }
            var isCopied by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = {
                    Text("Export Transactions", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "${state.allTransactions.size} transactions ready for export.",
                            color = MoneTrackaColors.MintDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Standard RFC-4180 CSV format (type, amount, categoryId, accountId, note, date):",
                            color = MoneTrackaColors.TextGray,
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MoneTrackaColors.SurfaceSecondary)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = csvContent.ifBlank { "No transactions recorded yet." },
                                color = MoneTrackaColors.TextDark,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        if (isCopied) {
                            Text(
                                text = "✓ CSV exported to clipboard ready to share/save!",
                                color = MoneTrackaColors.MintDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { isCopied = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.MintPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isCopied) "Copied!" else "Copy CSV", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = MoneTrackaColors.CardWhite
            )
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text("Import Transactions (CSV)", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Paste your CSV records below (Format: type,amount,categoryId,accountId,note,date):",
                            color = MoneTrackaColors.TextGray,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = importText,
                            onValueChange = {
                                importText = it
                                importValidationMsg = null
                            },
                            placeholder = { Text("EXPENSE,14.50,1,1,Lunch,1710000000000", fontSize = 11.sp, color = MoneTrackaColors.TextLight) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MoneTrackaColors.TextDark,
                                unfocusedTextColor = MoneTrackaColors.TextDark,
                                focusedBorderColor = MoneTrackaColors.MintPrimary,
                                unfocusedBorderColor = MoneTrackaColors.ProgressTrack
                            )
                        )
                        importValidationMsg?.let { msg ->
                            Text(text = msg, color = if (msg.startsWith("✓")) MoneTrackaColors.MintDark else MoneTrackaColors.CoralDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = com.monetracka.shared.domain.export.SimpleCsvExporter.parseTransactions(importText)
                            if (parsed.isEmpty()) {
                                importValidationMsg = "No valid records found in pasted CSV. Format must match exported CSV header."
                            } else {
                                onImportTransactions(parsed)
                                importValidationMsg = "✓ Successfully imported ${parsed.size} records!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneTrackaColors.MintPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Validate & Import", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = MoneTrackaColors.CardWhite
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 44.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "settings_title") {
                Text(text = "Settings & Preferences", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MoneTrackaColors.TextDark)
            }
            item(key = "profile_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Profile Name", color = MoneTrackaColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MoneTrackaColors.MintPrimary)
                            ) {
                                Text(
                                    text = state.userInitials,
                                    color = Color(0xFF051A12),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(state.userName, color = MoneTrackaColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = MoneTrackaColors.ProgressTrack)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCurrencyDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Currency", color = MoneTrackaColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Tap to switch default display currency", color = MoneTrackaColors.TextGray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MoneTrackaColors.MintLight)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(currencyLabel, color = MoneTrackaColors.MintDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(color = MoneTrackaColors.ProgressTrack)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Version", color = MoneTrackaColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("MoneTracka 1.0.0", color = MoneTrackaColors.TextGray, fontSize = 13.sp)
                    }
                }
            }
            item(key = "data_backup_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Data & Backups", color = MoneTrackaColors.TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Export or restore your transaction records in CSV format.", color = MoneTrackaColors.TextGray, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneTrackaColors.MintLight)
                                .clickable {
                                    if (state.isBiometricEnabled) {
                                        scope.launch {
                                            val ok = biometricAuthManager.authenticate(
                                                title = "Export Financial Records",
                                                subtitle = "Confirm your identity to export CSV"
                                            )
                                            if (ok) showExportDialog = true
                                        }
                                    } else {
                                        showExportDialog = true
                                    }
                                }
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = MoneTrackaColors.MintDark, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export CSV", color = MoneTrackaColors.MintDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MoneTrackaColors.SurfaceSecondary)
                                .clickable { showImportDialog = true }
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import", tint = MoneTrackaColors.TextDark, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import Data", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
            item(key = "security_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp), spotColor = MoneTrackaColors.CardShadowColor)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Security & Privacy", color = MoneTrackaColors.TextDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Protect your financial data with biometric authentication.", color = MoneTrackaColors.TextGray, fontSize = 12.sp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MoneTrackaColors.SurfaceSecondary)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Biometric App Lock", color = MoneTrackaColors.TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Require Face ID / Fingerprint to open", color = MoneTrackaColors.TextGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.isBiometricEnabled,
                            onCheckedChange = { targetState ->
                                scope.launch {
                                    val title = if (targetState) "Enable Biometric Lock" else "Disable Biometric Lock"
                                    val subtitle = if (targetState) "Confirm identity to enable app lock" else "Confirm identity to disable app lock"
                                    val success = biometricAuthManager.authenticate(title, subtitle)
                                    if (success) {
                                        onToggleBiometrics(targetState)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MoneTrackaColors.CardWhite,
                                checkedTrackColor = MoneTrackaColors.MintPrimary,
                                uncheckedThumbColor = MoneTrackaColors.CardWhite,
                                uncheckedTrackColor = MoneTrackaColors.ProgressTrack
                            )
                        )
                    }
                }
            }
        }
    }
}
