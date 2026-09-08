package com.monetracka.shared.ui.home

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class HomeScreenModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val quoteRepository: com.monetracka.shared.domain.quote.QuoteRepository = com.monetracka.shared.domain.quote.QuoteRepositoryImpl(),
    private val coachEngine: com.monetracka.shared.domain.coach.SmartSavingCoachEngine = com.monetracka.shared.domain.coach.SmartSavingCoachEngine(),
) : StateScreenModel<HomeUiState>(HomeUiState(isLoading = true)) {

    private var currentQuote = quoteRepository.getDailyQuote()

    init {
        screenModelScope.launch {
            categoryRepository.insertDefaultCategories()
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories()
            ) { transactions, categories ->
                val categoryMap = categories.associateBy { it.id }

                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

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
                        running += if (it.type == TransactionType.INCOME) it.amount else -it.amount
                        running.toFloat()
                    }
                } else {
                    listOf(0f, 15f, 35f, 28f, 50f, 75f, 90f)
                }

                val trend = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100.0 else if (totalExpense > 0) -100.0 else 0.0

                val insight = coachEngine.evaluate(transactions, categories)

                HomeUiState(
                    totalBalance = balance,
                    monthlyTrendPercent = trend,
                    monthlyTrendAmount = totalIncome - totalExpense,
                    sparklinePoints = sparkline,
                    categorySpends = expensesByCategory,
                    recentTransactions = transactions.sortedByDescending { it.dateMillis }.take(20),
                    categories = categoryMap,
                    coachInsight = insight,
                    financialQuote = currentQuote,
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
        }
    }

}
