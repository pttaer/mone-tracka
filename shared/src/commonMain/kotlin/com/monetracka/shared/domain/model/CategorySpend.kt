package com.monetracka.shared.domain.model

data class CategorySpend(
    val category: String,
    val amount: Double,
    val totalSpend: Double,
    val colorHex: Long
) {
    val percentage: Double
        get() = if (totalSpend > 0.0) (amount / totalSpend) * 100.0 else 0.0
}
