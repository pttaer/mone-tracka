package com.monetracka.shared.ui.home

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeTransactionRepository(private val txs: List<Transaction>) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs)
    override fun getTransactionsByMonth(yearMonth: String): Flow<List<Transaction>> = flowOf(txs)
    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = flowOf(emptyList())
    override suspend fun insertTransaction(transaction: Transaction): Long = 1L
    override suspend fun updateTransaction(transaction: Transaction) {}
    override suspend fun deleteTransaction(id: Long) {}
    override fun getTotalByTypeAndMonth(type: TransactionType, yearMonth: String): Flow<Double> = flowOf(0.0)
}

class FakeCategoryRepository(private val cats: List<Category>) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = flowOf(cats)
    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = flowOf(cats)
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: Long) {}
    override suspend fun insertDefaultCategories() {}
}

class HomeScreenModelTest {
    @Test
    fun testBalanceCalculation() = runTest {
        val fakeTxs = listOf(
            Transaction(id = 1L, amount = 100.0, type = TransactionType.INCOME, categoryId = 1L, dateMillis = 1000L),
            Transaction(id = 2L, amount = 40.0, type = TransactionType.EXPENSE, categoryId = 2L, dateMillis = 2000L)
        )
        val fakeCats = listOf(
            Category(id = 1L, name = "Salary", emoji = "💰", colorIndex = 0, type = TransactionType.INCOME),
            Category(id = 2L, name = "Dining", emoji = "🍕", colorIndex = 1, type = TransactionType.EXPENSE)
        )

        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(fakeTxs),
            categoryRepository = FakeCategoryRepository(fakeCats)
        )

        assertEquals(60.0, viewModel.state.value.totalBalance, 0.01)
    }
}
