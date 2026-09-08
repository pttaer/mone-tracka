package com.monetracka.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CategorySpendTest {
    @Test
    fun testPercentageCalculation() {
        val spend = CategorySpend(
            category = "Dining",
            amount = 50.0,
            totalSpend = 200.0,
            colorHex = 0xFF00D09CL
        )
        assertEquals(25.0, spend.percentage, 0.01)
    }

    @Test
    fun testZeroTotalSpendHandled() {
        val spend = CategorySpend(
            category = "Dining",
            amount = 50.0,
            totalSpend = 0.0,
            colorHex = 0xFF00D09CL
        )
        assertEquals(0.0, spend.percentage, 0.01)
    }
}
