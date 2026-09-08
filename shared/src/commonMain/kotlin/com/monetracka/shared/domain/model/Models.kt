package com.monetracka.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE
}

@Serializable
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val note: String = "",
    val dateMillis: Long,
    val createdAtMillis: Long = 0,
)

@Serializable
data class Category(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorIndex: Int = 0,
    val type: TransactionType,
    val isDefault: Boolean = true,
)

@Serializable
data class Budget(
    val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Double,
    val yearMonth: String,  // "2026-09" format
)
