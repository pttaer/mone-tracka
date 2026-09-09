package com.monetracka.shared.ui.home

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

import androidx.compose.runtime.Immutable

@Immutable
data class AccountUiModel(
    val account: Account,
    val balance: Double
)

@Immutable
data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyTrendPercent: Double = 0.0,
    val monthlyTrendAmount: Double = 0.0,
    val sparklinePoints: List<Float> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val accounts: List<AccountUiModel> = emptyList(),
    val coachInsight: com.monetracka.shared.domain.coach.CoachInsight? = null,
    val financialQuote: com.monetracka.shared.domain.quote.FinancialQuote? = null,
    val isAddSheetOpen: Boolean = false,
    val addSheetInitialType: TransactionType = TransactionType.EXPENSE,
    val isTransferSheetOpen: Boolean = false,
    val transferSourceAccount: Account? = null,
    val transferTargetAccount: Account? = null,
    val isAddAccountSheetOpen: Boolean = false,
    val userName: String = "User",
    val userInitials: String = "U",
    val currency: String = "USD",
    val monthlyBudgetLimit: Double = 2500.0,
    val isAdjustBudgetOpen: Boolean = false,
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
        val note: String,
        val accountId: Long = 1L
    ) : HomeIntent
    data class DeleteTransaction(val id: Long) : HomeIntent
    data class InitiateTransfer(val fromAccount: Account, val toAccount: Account) : HomeIntent
    data object DismissTransfer : HomeIntent
    data class ExecuteTransfer(val fromAccountId: Long, val toAccountId: Long, val amount: Double, val note: String = "") : HomeIntent
    data object OpenAddAccount : HomeIntent
    data object DismissAddAccount : HomeIntent
    data class CreateAccount(val name: String, val emoji: String, val initialBalance: Double, val description: String = "") : HomeIntent
    data class DeleteAccount(val id: Long) : HomeIntent
    data class UpdateMonthlyBudget(val newLimit: Double) : HomeIntent
    data object OpenAdjustBudget : HomeIntent
    data object DismissAdjustBudget : HomeIntent
}

