package com.monetracka.shared.domain.coach

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class SmartSavingCoachEngineTest {

    private val engine = SmartSavingCoachEngine()

    @Test
    fun `empty transactions returns info insight`() {
        val insight = engine.evaluate(emptyList())
        assertEquals(InsightSeverity.INFO, insight.severity)
        assertEquals("Welcome to Your Saving Journey", insight.title)
    }

    @Test
    fun `deficit returns warning`() {
        val transactions = listOf(
            Transaction(id = 1, amount = 1000.0, type = TransactionType.INCOME, categoryId = 1, dateMillis = 0),
            Transaction(id = 2, amount = 1500.0, type = TransactionType.EXPENSE, categoryId = 2, dateMillis = 0)
        )
        val insight = engine.evaluate(transactions)
        assertEquals(InsightSeverity.WARNING, insight.severity)
        assertEquals("Spending Exceeds Income", insight.title)
    }

    @Test
    fun `high category concentration returns warning`() {
        val categories = listOf(Category(id = 10, name = "Dining", emoji = "🍔", type = TransactionType.EXPENSE))
        val transactions = listOf(
            Transaction(id = 1, amount = 5000.0, type = TransactionType.INCOME, categoryId = 1, dateMillis = 0),
            Transaction(id = 2, amount = 1200.0, type = TransactionType.EXPENSE, categoryId = 10, dateMillis = 0), // 60% of expenses
            Transaction(id = 3, amount = 800.0, type = TransactionType.EXPENSE, categoryId = 20, dateMillis = 0)
        )
        val insight = engine.evaluate(transactions, categories)
        assertEquals(InsightSeverity.WARNING, insight.severity)
        assertEquals("High Spending Concentration", insight.title)
    }

    @Test
    fun `saving rate over 50 percent returns celebration`() {
        val transactions = listOf(
            Transaction(id = 1, amount = 4000.0, type = TransactionType.INCOME, categoryId = 1, dateMillis = 0),
            Transaction(id = 2, amount = 1000.0, type = TransactionType.EXPENSE, categoryId = 2, dateMillis = 0)
        )
        val insight = engine.evaluate(transactions)
        assertEquals(InsightSeverity.CELEBRATION, insight.severity)
        assertEquals("Exceptional Saving Rate!", insight.title)
    }
}
