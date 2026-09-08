package com.monetracka.shared.ui.home

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyTrendPercent: Double = 0.0,
    val monthlyTrendAmount: Double = 0.0,
    val sparklinePoints: List<Float> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val coachInsight: com.monetracka.shared.domain.coach.CoachInsight? = null,
    val financialQuote: com.monetracka.shared.domain.quote.FinancialQuote? = null,
    val isAddSheetOpen: Boolean = false,
    val addSheetInitialType: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = false
)

sealed interface HomeIntent {
    data class OpenAddTransaction(val initialType: TransactionType = TransactionType.EXPENSE) : HomeIntent
    data object DismissAddTransaction : HomeIntent
    data object RefreshQuote : HomeIntent
    data class CreateTransaction(
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long,
        val note: String
    ) : HomeIntent
    data class DeleteTransaction(val id: Long) : HomeIntent
}
