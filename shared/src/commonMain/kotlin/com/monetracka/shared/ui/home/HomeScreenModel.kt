package com.monetracka.shared.ui.home

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import com.monetracka.shared.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class HomeScreenModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val userProfileRepository: UserProfileRepository? = null,
    private val quoteRepository: com.monetracka.shared.domain.quote.QuoteRepository = com.monetracka.shared.domain.quote.QuoteRepositoryImpl(),
    private val coachEngine: com.monetracka.shared.domain.coach.SmartSavingCoachEngine = com.monetracka.shared.domain.coach.SmartSavingCoachEngine(),
) : StateScreenModel<HomeUiState>(HomeUiState(isLoading = true)) {

    private var currentQuote = quoteRepository.getDailyQuote()

    init {
        screenModelScope.launch {
            categoryRepository.insertDefaultCategories()
            accountRepository.insertDefaultAccounts()
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                accountRepository.getAllAccounts(),
                userProfileRepository?.getUserProfile() ?: flowOf(null)
            ) { transactions, categories, accounts, userProfile ->
                val categoryMap = categories.associateBy { it.id }

                var totalIncome = 0.0
                var totalExpense = 0.0
                val accountInflows = mutableMapOf<Long, Double>()
                val accountOutflows = mutableMapOf<Long, Double>()
                val categoryExpenses = mutableMapOf<Long, Double>()

                for (tx in transactions) {
                    when (tx.type) {
                        TransactionType.INCOME -> {
                            totalIncome += tx.amount
                            accountInflows[tx.accountId] = (accountInflows[tx.accountId] ?: 0.0) + tx.amount
                        }
                        TransactionType.EXPENSE -> {
                            totalExpense += tx.amount
                            accountOutflows[tx.accountId] = (accountOutflows[tx.accountId] ?: 0.0) + tx.amount
                            categoryExpenses[tx.categoryId] = (categoryExpenses[tx.categoryId] ?: 0.0) + tx.amount
                        }
                        TransactionType.TRANSFER -> {
                            accountOutflows[tx.accountId] = (accountOutflows[tx.accountId] ?: 0.0) + tx.amount
                            tx.toAccountId?.let { toId ->
                                accountInflows[toId] = (accountInflows[toId] ?: 0.0) + tx.amount
                            }
                        }
                    }
                }

                val accountUiList = accounts.map { acc ->
                    val inflows = accountInflows[acc.id] ?: 0.0
                    val outflows = accountOutflows[acc.id] ?: 0.0
                    AccountUiModel(
                        account = acc,
                        balance = acc.initialBalance + inflows - outflows
                    )
                }

                val totalNetBalance = if (accountUiList.isNotEmpty()) {
                    accountUiList.sumOf { it.balance }
                } else {
                    totalIncome - totalExpense
                }

                val expensesByCategory = categoryExpenses.map { (catId, amount) ->
                    val cat = categoryMap[catId]
                    val catName = cat?.name ?: "Other"
                    val colorIdx = cat?.colorIndex ?: 0
                    val hex = com.monetracka.shared.ui.theme.CategoryColorHexes.getOrElse(colorIdx) { 0xFF00D09CL }
                    CategorySpend(
                        category = catName,
                        amount = amount,
                        totalSpend = totalExpense,
                        colorIndex = colorIdx,
                        colorHex = hex
                    )
                }.sortedByDescending { it.amount }

                val sortedByDateDesc = transactions.sortedByDescending { it.dateMillis }
                val sparkline = if (transactions.isNotEmpty()) {
                    var running = 0.0
                    sortedByDateDesc.take(10).reversed().map {
                        running += when (it.type) {
                            TransactionType.INCOME -> it.amount
                            TransactionType.EXPENSE -> -it.amount
                            TransactionType.TRANSFER -> 0.0
                        }
                        running.toFloat()
                    }
                } else {
                    listOf(0f, 15f, 35f, 28f, 50f, 75f, 90f)
                }

                val trend = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100.0 else if (totalExpense > 0) -100.0 else 0.0

                val insight = coachEngine.evaluate(transactions, categories)

                HomeUiState(
                    totalBalance = totalNetBalance,
                    monthlyTrendPercent = trend,
                    monthlyTrendAmount = totalIncome - totalExpense,
                    sparklinePoints = sparkline,
                    categorySpends = expensesByCategory,
                    recentTransactions = sortedByDateDesc.take(20),
                    categories = categoryMap,
                    accounts = accountUiList,
                    coachInsight = insight,
                    financialQuote = currentQuote,
                    isAddSheetOpen = mutableState.value.isAddSheetOpen,
                    addSheetInitialType = mutableState.value.addSheetInitialType,
                    isTransferSheetOpen = mutableState.value.isTransferSheetOpen,
                    transferSourceAccount = mutableState.value.transferSourceAccount,
                    transferTargetAccount = mutableState.value.transferTargetAccount,
                    isAddAccountSheetOpen = mutableState.value.isAddAccountSheetOpen,
                    userName = userProfile?.userName ?: "User",
                    userInitials = userProfile?.initials ?: "U",
                    currency = userProfile?.currency ?: "USD",
                    monthlyBudgetLimit = userProfile?.monthlyBudgetLimit ?: 2500.0,
                    isAdjustBudgetOpen = mutableState.value.isAdjustBudgetOpen,
                    isLoading = false
                )
            }.collect { newState ->
                mutableState.value = newState
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OpenAddTransaction -> mutableState.value = mutableState.value.copy(
                isAddSheetOpen = true,
                addSheetInitialType = intent.initialType
            )
            HomeIntent.DismissAddTransaction -> mutableState.value = mutableState.value.copy(isAddSheetOpen = false)
            HomeIntent.RefreshQuote -> {
                currentQuote = quoteRepository.getRandomQuote()
                mutableState.value = mutableState.value.copy(financialQuote = currentQuote)
            }
            is HomeIntent.CreateTransaction -> {
                screenModelScope.launch {
                    val now = Clock.System.now().toEpochMilliseconds()
                    val newTx = Transaction(
                        amount = intent.amount,
                        type = intent.type,
                        categoryId = intent.categoryId,
                        accountId = intent.accountId,
                        dateMillis = now,
                        createdAtMillis = now,
                        note = intent.note
                    )
                    transactionRepository.insertTransaction(newTx)
                    mutableState.value = mutableState.value.copy(isAddSheetOpen = false)
                }
            }
            is HomeIntent.DeleteTransaction -> {
                screenModelScope.launch {
                    transactionRepository.deleteTransaction(intent.id)
                }
            }
            is HomeIntent.InitiateTransfer -> {
                mutableState.value = mutableState.value.copy(
                    isTransferSheetOpen = true,
                    transferSourceAccount = intent.fromAccount,
                    transferTargetAccount = intent.toAccount
                )
            }
            HomeIntent.DismissTransfer -> {
                mutableState.value = mutableState.value.copy(
                    isTransferSheetOpen = false,
                    transferSourceAccount = null,
                    transferTargetAccount = null
                )
            }
            is HomeIntent.ExecuteTransfer -> {
                screenModelScope.launch {
                    val now = Clock.System.now().toEpochMilliseconds()
                    val transferCatId = mutableState.value.categories.values.firstOrNull { it.type == TransactionType.TRANSFER }?.id ?: 1L
                    val newTx = Transaction(
                        amount = intent.amount,
                        type = TransactionType.TRANSFER,
                        categoryId = transferCatId,
                        accountId = intent.fromAccountId,
                        toAccountId = intent.toAccountId,
                        note = intent.note,
                        dateMillis = now,
                        createdAtMillis = now
                    )
                    transactionRepository.insertTransaction(newTx)
                    mutableState.value = mutableState.value.copy(
                        isTransferSheetOpen = false,
                        transferSourceAccount = null,
                        transferTargetAccount = null
                    )
                }
            }
            HomeIntent.OpenAddAccount -> {
                mutableState.value = mutableState.value.copy(isAddAccountSheetOpen = true)
            }
            HomeIntent.DismissAddAccount -> {
                mutableState.value = mutableState.value.copy(isAddAccountSheetOpen = false)
            }
            is HomeIntent.CreateAccount -> {
                screenModelScope.launch {
                    accountRepository.insertAccount(
                        Account(
                            name = intent.name,
                            emoji = intent.emoji,
                            initialBalance = intent.initialBalance,
                            description = intent.description
                        )
                    )
                    mutableState.value = mutableState.value.copy(isAddAccountSheetOpen = false)
                }
            }
            is HomeIntent.DeleteAccount -> {
                screenModelScope.launch {
                    accountRepository.deleteAccount(intent.id)
                }
            }
            HomeIntent.OpenAdjustBudget -> {
                mutableState.value = mutableState.value.copy(isAdjustBudgetOpen = true)
            }
            HomeIntent.DismissAdjustBudget -> {
                mutableState.value = mutableState.value.copy(isAdjustBudgetOpen = false)
            }
            is HomeIntent.UpdateMonthlyBudget -> {
                screenModelScope.launch {
                    userProfileRepository?.updateMonthlyBudget(intent.newLimit)
                    mutableState.value = mutableState.value.copy(
                        monthlyBudgetLimit = intent.newLimit,
                        isAdjustBudgetOpen = false
                    )
                }
            }
        }
    }
}

