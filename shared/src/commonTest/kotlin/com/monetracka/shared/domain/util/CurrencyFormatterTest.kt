package com.monetracka.shared.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatterTest {
    @Test
    fun testUsdFormatting() {
        assertEquals("$1,240.50", CurrencyFormatter.format(1240.50, "USD"))
    }

    @Test
    fun testNegativeFormatting() {
        assertEquals("-$45.20", CurrencyFormatter.format(-45.20, "USD"))
    }

    @Test
    fun testZeroFormatting() {
        assertEquals("$0.00", CurrencyFormatter.format(0.0, "USD"))
    }

    @Test
    fun testSplitAmountForHeroDisplay() {
        val (integerPart, decimalPart) = CurrencyFormatter.splitAmount(84250.75, "USD")
        assertEquals("$84,250", integerPart)
        assertEquals(".75", decimalPart)
    }
}
