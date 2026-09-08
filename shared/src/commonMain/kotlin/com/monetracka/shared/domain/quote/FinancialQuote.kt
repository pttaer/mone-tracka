package com.monetracka.shared.domain.quote

enum class QuoteCategory {
    SAVING,
    DISCIPLINE,
    INVESTING,
    MINDSET
}

data class FinancialQuote(
    val id: String,
    val quote: String,
    val author: String,
    val category: QuoteCategory
)

interface QuoteRepository {
    val allQuotes: List<FinancialQuote>
    fun getDailyQuote(): FinancialQuote
    fun getRandomQuote(excludeId: String? = null): FinancialQuote
}
