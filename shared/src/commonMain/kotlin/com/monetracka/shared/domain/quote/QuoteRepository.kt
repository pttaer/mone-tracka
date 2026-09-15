package com.monetracka.shared.domain.quote

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class QuoteCategory { SAVING, DISCIPLINE, INVESTING, MINDSET }

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

class QuoteRepositoryImpl : QuoteRepository {
    override val allQuotes = listOf(
        FinancialQuote("1", "Do not save what is left after spending, but spend what is left after saving.", "Warren Buffett", QuoteCategory.SAVING),
        FinancialQuote("2", "A budget is telling your money where to go instead of wondering where it went.", "Dave Ramsey", QuoteCategory.DISCIPLINE),
        FinancialQuote("3", "Beware of little expenses; a small leak will sink a great ship.", "Benjamin Franklin", QuoteCategory.SAVING),
        FinancialQuote("4", "Wealth is what you don't see. It's the cars not purchased, the watches not worn.", "Morgan Housel", QuoteCategory.MINDSET),
        FinancialQuote("5", "The goal isn't more money. The goal is living life on your terms.", "Chris Brogan", QuoteCategory.MINDSET),
        FinancialQuote("6", "Simplicity is the key to financial security and peace of mind.", "Charlie Munger", QuoteCategory.DISCIPLINE),
        FinancialQuote("7", "Financial freedom is available to those who learn about it and work for it.", "Robert Kiyosaki", QuoteCategory.INVESTING),
        FinancialQuote("8", "Spend less than you make, always be saving, and let compound interest do the heavy lifting.", "Naval Ravikant", QuoteCategory.INVESTING),
        FinancialQuote("9", "Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln", QuoteCategory.DISCIPLINE),
        FinancialQuote("10", "It's not your salary that makes you rich, it's your spending habits.", "Charles A. Jaffe", QuoteCategory.SAVING)
    )

    override fun getDailyQuote(): FinancialQuote {
        val dayOfYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfYear
        return allQuotes[dayOfYear % allQuotes.size]
    }

    override fun getRandomQuote(excludeId: String?): FinancialQuote =
        allQuotes.filter { it.id != excludeId }.ifEmpty { allQuotes }.random()
}
