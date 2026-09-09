package com.monetracka.shared.ui.home.components

import com.monetracka.shared.domain.util.CurrencyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceHeroCardTest {
    @Test
    fun testCurrencyFormattingSplitsCorrectly() {
        val (intPart, decPart) = CurrencyFormatter.splitAmount(1234.56, "USD")
        assertEquals("$1,234", intPart)
        assertEquals(".56", decPart)
    }
}
