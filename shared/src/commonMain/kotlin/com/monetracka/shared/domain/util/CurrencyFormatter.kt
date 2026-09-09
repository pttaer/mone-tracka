package com.monetracka.shared.domain.util

import kotlin.math.abs

object CurrencyFormatter {
    private val SYMBOLS = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "VND" to "₫"
    )

    fun symbol(currency: String = "USD"): String = SYMBOLS[currency] ?: "$"

    fun format(amount: Double, currency: String = "USD"): String {
        val (intPart, decPart) = splitAmount(amount, currency)
        return "$intPart$decPart"
    }

    fun splitAmount(amount: Double, currency: String = "USD"): Pair<String, String> {
        val symbol = SYMBOLS[currency] ?: "$"
        val isNegative = amount < 0
        val positive = abs(amount)
        val whole = positive.toLong()
        val cents = ((positive - whole) * 100 + 0.5).toInt()
        val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
        val prefix = if (isNegative) "-$symbol" else symbol
        return Pair("$prefix$grouped", "." + cents.toString().padStart(2, '0'))
    }
}
