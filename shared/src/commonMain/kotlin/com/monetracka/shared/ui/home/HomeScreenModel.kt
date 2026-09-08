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

                val accountUiList = accounts.map { acc ->
                    val inflows = transactions.filter { it.type == TransactionType.INCOME && it.accountId == acc.id }.sumOf { it.amount } +
                        transactions.filter { it.type == TransactionType.TRANSFER && it.toAccountId == acc.id }.sumOf { it.amount }
                    val outflows = transactions.filter { it.type == TransactionType.EXPENSE && it.accountId == acc.id }.sumOf { it.amount } +
                        transactions.filter { it.type == TransactionType.TRANSFER && it.accountId == acc.id }.sumOf { it.amount }
                    AccountUiModel(
                        account = acc,
                        balance = acc.initialBalance + inflows - outflows
                    )
                }

                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val totalNetBalance = if (accountUiList.isNotEmpty()) {
                    accountUiList.sumOf { it.balance }
                } else {
                    totalIncome - totalExpense
                }

                val expensesByCategory = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.categoryId }
                    .map { (catId, txList) ->
                        val cat = categoryMap[catId]
                        val catName = cat?.name ?: "Other"
                        val colorIdx = cat?.colorIndex ?: 0
                        val hex = com.monetracka.shared.ui.theme.CategoryColorHexes.getOrElse(colorIdx) { 0xFF00D09CL }
                        CategorySpend(
                            category = catName,
                            amount = txList.sumOf { it.amount },
                            totalSpend = totalExpense,
                            colorIndex = colorIdx,
                            colorHex = hex
                        )
                    }
                    .sortedByDescending { it.amount }

                val sparkline = if (transactions.isNotEmpty()) {
                    var running = 0.0
                    transactions.sortedBy { it.dateMillis }.takeLast(10).map {
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
                    recentTransactions = transactions.sortedByDescending { it.dateMillis }.take(20),
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
        }
    }
}

