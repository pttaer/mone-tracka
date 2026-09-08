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


