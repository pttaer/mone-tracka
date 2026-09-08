package com.monetracka.shared.domain.quote

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuoteRepositoryTest {
    private val repo = QuoteRepositoryImpl()

    @Test
    fun testGetDailyQuoteReturnsValidQuote() {
        val quote = repo.getDailyQuote()
        assertNotNull(quote.quote)
        assertTrue(quote.quote.isNotBlank())
        assertNotNull(quote.author)
    }

    @Test
    fun testGetRandomQuoteDifferentFromPrevious() {
        val first = repo.getDailyQuote()
        val next = repo.getRandomQuote(excludeId = first.id)
        assertTrue(next.id != first.id || repo.allQuotes.size <= 1)
    }
}
