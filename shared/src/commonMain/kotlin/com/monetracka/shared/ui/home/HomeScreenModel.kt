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
) : StateScreenModel<HomeUiState>(HomeUiState(isLoading = true)) {

    init {
        screenModelScope.launch {
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
                        val catColor = com.monetracka.shared.ui.theme.CategoryColors.getOrElse(cat?.colorIndex ?: 0) {
                            com.monetracka.shared.ui.theme.MintGreen
                        }
                        CategorySpend(
                            category = catName,
                            amount = txList.sumOf { it.amount },
                            totalSpend = totalExpense,
                            colorHex = (catColor.value.toLong() shr 32) or (catColor.value.toLong() shl 32)
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

                HomeUiState(
                    totalBalance = balance,
                    monthlyTrendPercent = 18.4,
                    monthlyTrendAmount = totalIncome - totalExpense,
                    sparklinePoints = sparkline,
                    categorySpends = expensesByCategory,
                    recentTransactions = transactions.sortedByDescending { it.dateMillis }.take(20),
                    categories = categoryMap,
                    isLoading = false
                )
            }.collect { newState ->
                mutableState.value = newState
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.OpenAddTransaction -> mutableState.value = mutableState.value.copy(isAddSheetOpen = true)
            HomeIntent.DismissAddTransaction -> mutableState.value = mutableState.value.copy(isAddSheetOpen = false)
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
