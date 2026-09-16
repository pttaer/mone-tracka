package com.monetracka.shared.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

data class Account(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val initialBalance: Double = 0.0,
    val description: String = ""
)

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val toAccountId: Long? = null,
    val note: String = "",
    val dateMillis: Long,
    val createdAtMillis: Long = 0,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorIndex: Int = 0,
    val type: TransactionType,
    val isDefault: Boolean = true,
)

data class UserProfile(
    val userName: String = "User",
    val currency: String = "USD",
    val hasCompletedOnboarding: Boolean = false,
    val monthlyBudgetLimit: Double = 2500.0,
    val isBiometricEnabled: Boolean = false,
) {
    val initials: String
        get() = userName.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
}
data class ExchangeRate(
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val updatedAtMillis: Long = 0L
) {
    fun convert(amount: Double): Double = amount * rate
}

fun Double.convertCurrency(fromRate: Double, toRate: Double): Double {
    if (fromRate <= 0.0) return this
    return this * (toRate / fromRate)
}

data class CategoryBudget(
    val categoryId: Long,
    val monthlyLimit: Double,
    val rolloverEnabled: Boolean = false
)

data class CategorySpend(
    val category: String,
    val amount: Double,
    val totalSpend: Double,
    val colorIndex: Int = 0,
    val colorHex: Long = 0xFF00D09CL,
    val budgetLimit: Double? = null
) {
    val percentage: Double
        get() = if (totalSpend > 0.0) (amount / totalSpend) * 100.0 else 0.0
}

enum class RecurringInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurringTransaction(
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val intervalType: RecurringInterval = RecurringInterval.MONTHLY,
    val intervalCount: Int = 1,
    val nextDueDateMillis: Long,
    val autoPost: Boolean = true
)




