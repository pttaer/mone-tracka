package com.monetracka.shared.domain.model

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

