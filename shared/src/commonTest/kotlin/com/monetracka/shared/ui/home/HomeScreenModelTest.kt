package com.monetracka.shared.ui.home

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeTransactionRepository(private val txs: List<Transaction>) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs)
    override suspend fun insertTransaction(transaction: Transaction): Long = 1L
    override suspend fun deleteTransaction(id: Long) {}
}

class FakeCategoryRepository(private val cats: List<Category>) : CategoryRepository {
    override fun getAllCategories(): Flow<List<Category>> = flowOf(cats)
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun insertDefaultCategories() {}
}

class FakeAccountRepository(private val accounts: List<Account>) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> = flowOf(accounts)
    override suspend fun insertAccount(account: Account): Long = 1L
    override suspend fun deleteAccount(id: Long) {}
    override suspend fun insertDefaultAccounts() {}
}

class FakeUserProfileRepository(private val profile: com.monetracka.shared.domain.model.UserProfile?) : com.monetracka.shared.domain.repository.UserProfileRepository {
    override fun getUserProfile(): Flow<com.monetracka.shared.domain.model.UserProfile?> = flowOf(profile)
    override suspend fun saveUserProfile(profile: com.monetracka.shared.domain.model.UserProfile) {}
    override suspend fun hasCompletedOnboarding(): Boolean = profile?.hasCompletedOnboarding ?: false
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testBalanceCalculation() = runTest(testDispatcher) {
        val fakeTxs = listOf(
            Transaction(id = 1L, amount = 100.0, type = TransactionType.INCOME, categoryId = 1L, accountId = 1L, dateMillis = 1000L),
            Transaction(id = 2L, amount = 40.0, type = TransactionType.EXPENSE, categoryId = 2L, accountId = 1L, dateMillis = 2000L)
        )
        val fakeCats = listOf(
            Category(id = 1L, name = "Salary", emoji = "💰", colorIndex = 0, type = TransactionType.INCOME),
            Category(id = 2L, name = "Dining", emoji = "🍕", colorIndex = 1, type = TransactionType.EXPENSE)
        )
        val fakeAccounts = listOf(
            Account(id = 1L, name = "Checking", emoji = "🏦", initialBalance = 0.0)
        )

        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(fakeTxs),
            categoryRepository = FakeCategoryRepository(fakeCats),
            accountRepository = FakeAccountRepository(fakeAccounts)
        )

        testScheduler.advanceUntilIdle()

        assertEquals(60.0, viewModel.state.value.totalBalance, 0.01)
    }

    @Test
    fun testUserProfileLoaded() = runTest(testDispatcher) {
        val profile = com.monetracka.shared.domain.model.UserProfile(
            userName = "Sarah Connor",
            currency = "EUR",
            hasCompletedOnboarding = true
        )

        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList()),
            userProfileRepository = FakeUserProfileRepository(profile)
        )

        testScheduler.advanceUntilIdle()

        assertEquals("Sarah Connor", viewModel.state.value.userName)
        assertEquals("SC", viewModel.state.value.userInitials)
        assertEquals("EUR", viewModel.state.value.currency)
    }
}

