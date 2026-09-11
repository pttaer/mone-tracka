package com.monetracka.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryBudgetTest {

    @Test
    fun testCategoryBudgetCreation() {
        val budget = CategoryBudget(
            categoryId = 3L,
            monthlyLimit = 400.0,
            rolloverEnabled = true
        )
        assertEquals(3L, budget.categoryId)
        assertEquals(400.0, budget.monthlyLimit)
        assertTrue(budget.rolloverEnabled)
    }

    @Test
    fun testDefaultRolloverDisabled() {
        val budget = CategoryBudget(
            categoryId = 1L,
            monthlyLimit = 250.0
        )
        assertFalse(budget.rolloverEnabled)
    }
}
