package com.monetracka.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ExchangeRateTest {

    @Test
    fun testDirectConversion() {
        val rate = ExchangeRate(fromCurrency = "USD", toCurrency = "EUR", rate = 0.92, updatedAtMillis = 1000L)
        val converted = rate.convert(100.0)
        assertEquals(92.0, converted)
    }

    @Test
    fun testSameCurrencyConversion() {
        val rate = ExchangeRate(fromCurrency = "USD", toCurrency = "USD", rate = 1.0, updatedAtMillis = 1000L)
        assertEquals(50.0, rate.convert(50.0))
    }

    @Test
    fun testCrossCurrencyConversion() {
        // 100 EUR -> USD (from EUR rate 0.92 per USD to GBP rate 0.79 per USD)
        // fromRate = 0.92, toRate = 0.79 -> 100 * (0.79 / 0.92)
        val amountInEur = 92.0
        val amountInGbp = amountInEur.convertCurrency(fromRate = 0.92, toRate = 0.79)
        assertEquals(79.0, amountInGbp)
    }
}

