package com.monetracka.shared.domain.export

import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleCsvExporterTest {

    @Test
    fun testExportAndParseRoundtrip() {
        val original = listOf(
            Transaction(
                id = 1,
                amount = 45.5,
                type = TransactionType.EXPENSE,
                categoryId = 2,
                accountId = 1,
                note = "Lunch at cafe",
                dateMillis = 1726000000000L,
                createdAtMillis = 1726000000000L
            ),
            Transaction(
                id = 2,
                amount = 1200.0,
                type = TransactionType.INCOME,
                categoryId = 1,
                accountId = 1,
                note = "Salary payout",
                dateMillis = 1726000050000L,
                createdAtMillis = 1726000050000L
            )
        )

        val csv = SimpleCsvExporter.exportTransactions(original)
        val parsed = SimpleCsvExporter.parseTransactions(csv)

        assertEquals(2, parsed.size)
        assertEquals(45.5, parsed[0].amount)
        assertEquals(TransactionType.EXPENSE, parsed[0].type)
        assertEquals("Lunch at cafe", parsed[0].note)
        assertEquals(1200.0, parsed[1].amount)
        assertEquals(TransactionType.INCOME, parsed[1].type)
    }

    @Test
    fun testParseEmptyCsvReturnsEmptyList() {
        val result = SimpleCsvExporter.parseTransactions("")
        assertEquals(0, result.size)
    }
}
