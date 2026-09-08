package com.monetracka.shared.domain.account

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import com.monetracka.shared.ui.home.HomeScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeAccountRepository(private val accounts: List<Account>) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> = flowOf(accounts)
    override suspend fun insertAccount(account: Account): Long = 1L
    override suspend fun deleteAccount(id: Long) {}
    override suspend fun insertDefaultAccounts() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountBalanceCalculationTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun testAccountBalancesAndNetZeroTransfer() = runTest(testDispatcher) {
        val bankAccount = Account(id = 1L, name = "Bank", emoji = "🏦", initialBalance = 1000.0)
        val cashAccount = Account(id = 2L, name = "Cash", emoji = "💵", initialBalance = 200.0)
        val accounts = listOf(bankAccount, cashAccount)

        val txs = listOf(
            // Income 300 to Bank
            Transaction(id = 1L, amount = 300.0, type = TransactionType.INCOME, categoryId = 1L, accountId = 1L, dateMillis = 1000L),
            // Expense 50 from Cash
            Transaction(id = 2L, amount = 50.0, type = TransactionType.EXPENSE, categoryId = 2L, accountId = 2L, dateMillis = 2000L),
            // Transfer 150 from Bank to Cash
            Transaction(id = 3L, amount = 150.0, type = TransactionType.TRANSFER, categoryId = 3L, accountId = 1L, toAccountId = 2L, dateMillis = 3000L)
        )

        val fakeTxRepo = object : TransactionRepository {
            override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs)
            override suspend fun insertTransaction(transaction: Transaction): Long = 1L
            override suspend fun deleteTransaction(id: Long) {}
        }
        val fakeCatRepo = object : CategoryRepository {
            override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
            override suspend fun insertCategory(category: Category): Long = 1L
            override suspend fun insertDefaultCategories() {}
        }

        val viewModel = HomeScreenModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            accountRepository = FakeAccountRepository(accounts)
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // Bank: 1000 (initial) + 300 (income) - 150 (transfer out) = 1150
        val bankUi = state.accounts.first { it.account.id == 1L }
        assertEquals(1150.0, bankUi.balance, 0.01)

        // Cash: 200 (initial) - 50 (expense) + 150 (transfer in) = 300
        val cashUi = state.accounts.first { it.account.id == 2L }
        assertEquals(300.0, cashUi.balance, 0.01)

        // Total Net Portfolio: 1150 + 300 = 1450 (which equals initial 1200 + 300 income - 50 expense)
        assertEquals(1450.0, state.totalBalance, 0.01)
    }
}
