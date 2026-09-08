package com.monetracka.shared.ui.home

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyTrendPercent: Double = 0.0,
    val monthlyTrendAmount: Double = 0.0,
    val sparklinePoints: List<Float> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val isAddSheetOpen: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface HomeIntent {
    data object OpenAddTransaction : HomeIntent
    data object DismissAddTransaction : HomeIntent
    data class CreateTransaction(
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long,
        val note: String
    ) : HomeIntent
    data class DeleteTransaction(val id: Long) : HomeIntent
}
