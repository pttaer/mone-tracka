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
    val inserted = mutableListOf<Transaction>()
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs + inserted)
    override fun searchTransactions(query: String): Flow<List<Transaction>> = flowOf((txs + inserted).filter { it.note.contains(query, ignoreCase = true) })
    override suspend fun insertTransaction(transaction: Transaction): Long {
        inserted.add(transaction)
        return 1L
    }
    override suspend fun deleteTransaction(id: Long) {}
}

class FakeCategoryRepository(private val cats: List<Category>) : CategoryRepository {
    val inserted = mutableListOf<Category>()
    override fun getAllCategories(): Flow<List<Category>> = flowOf(cats + inserted)
    override suspend fun insertCategory(category: Category): Long {
        inserted.add(category)
        return (cats.size + inserted.size).toLong()
    }
    override suspend fun insertDefaultCategories() {}
}

class FakeAccountRepository(private val accounts: List<Account>) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> = flowOf(accounts)
    override suspend fun insertAccount(account: Account): Long = 1L
    override suspend fun deleteAccount(id: Long) {}
    override suspend fun insertDefaultAccounts() {}
}

class FakeUserProfileRepository(profile: com.monetracka.shared.domain.model.UserProfile?) : com.monetracka.shared.domain.repository.UserProfileRepository {
    private val profileFlow = kotlinx.coroutines.flow.MutableStateFlow(profile)
    override fun getUserProfile(): Flow<com.monetracka.shared.domain.model.UserProfile?> = profileFlow
    override suspend fun saveUserProfile(profile: com.monetracka.shared.domain.model.UserProfile) {
        profileFlow.value = profile
    }
    override suspend fun updateMonthlyBudget(limit: Double) {
        profileFlow.value = profileFlow.value?.copy(monthlyBudgetLimit = limit)
    }
    override suspend fun updateCurrency(currency: String) {
        profileFlow.value = profileFlow.value?.copy(currency = currency)
    }
    override suspend fun hasCompletedOnboarding(): Boolean = profileFlow.value?.hasCompletedOnboarding ?: false
    override suspend fun updateBiometricEnabled(enabled: Boolean) {
        profileFlow.value = profileFlow.value?.copy(isBiometricEnabled = enabled)
    }
    override suspend fun isBiometricEnabled(): Boolean = profileFlow.value?.isBiometricEnabled ?: false
}

class FakeCategoryBudgetRepository : com.monetracka.shared.domain.repository.CategoryBudgetRepository {
    val budgets = mutableMapOf<Long, com.monetracka.shared.domain.model.CategoryBudget>()
    override fun getAllCategoryBudgets(): Flow<List<com.monetracka.shared.domain.model.CategoryBudget>> = flowOf(budgets.values.toList())
    override suspend fun getBudget(categoryId: Long): com.monetracka.shared.domain.model.CategoryBudget? = budgets[categoryId]
    override suspend fun saveBudget(budget: com.monetracka.shared.domain.model.CategoryBudget) {
        budgets[budget.categoryId] = budget
    }
    override suspend fun deleteBudget(categoryId: Long) {
        budgets.remove(categoryId)
    }
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

    @Test
    fun testTransferBalancesAndNetBalance() = runTest(testDispatcher) {
        val fakeTxs = listOf(
            Transaction(id = 1L, amount = 30.0, type = TransactionType.TRANSFER, categoryId = 1L, accountId = 1L, toAccountId = 2L, dateMillis = 1000L)
        )
        val fakeCats = listOf(
            Category(id = 1L, name = "General", emoji = "🔄", colorIndex = 0, type = TransactionType.TRANSFER)
        )
        val fakeAccounts = listOf(
            Account(id = 1L, name = "Checking", emoji = "🏦", initialBalance = 100.0),
            Account(id = 2L, name = "Savings", emoji = "💰", initialBalance = 50.0)
        )

        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(fakeTxs),
            categoryRepository = FakeCategoryRepository(fakeCats),
            accountRepository = FakeAccountRepository(fakeAccounts)
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        val acc1 = state.accounts.first { it.account.id == 1L }
        val acc2 = state.accounts.first { it.account.id == 2L }

        assertEquals(70.0, acc1.balance, 0.01)
        assertEquals(80.0, acc2.balance, 0.01)
        assertEquals(150.0, state.totalBalance, 0.01)
    }

    @Test
    fun testCategoryExpensesAggregation() = runTest(testDispatcher) {
        val fakeTxs = listOf(
            Transaction(id = 1L, amount = 40.0, type = TransactionType.EXPENSE, categoryId = 1L, accountId = 1L, dateMillis = 1000L),
            Transaction(id = 2L, amount = 70.0, type = TransactionType.EXPENSE, categoryId = 2L, accountId = 1L, dateMillis = 2000L),
            Transaction(id = 3L, amount = 25.0, type = TransactionType.EXPENSE, categoryId = 1L, accountId = 1L, dateMillis = 3000L)
        )
        val fakeCats = listOf(
            Category(id = 1L, name = "Dining", emoji = "🍕", colorIndex = 1, type = TransactionType.EXPENSE),
            Category(id = 2L, name = "Groceries", emoji = "🛒", colorIndex = 2, type = TransactionType.EXPENSE)
        )
        val fakeAccounts = listOf(
            Account(id = 1L, name = "Checking", emoji = "🏦", initialBalance = 500.0)
        )

        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(fakeTxs),
            categoryRepository = FakeCategoryRepository(fakeCats),
            accountRepository = FakeAccountRepository(fakeAccounts)
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.categorySpends.size)
        // Highest spend first: Groceries (70.0), then Dining (65.0)
        assertEquals("Groceries", state.categorySpends[0].category)
        assertEquals(70.0, state.categorySpends[0].amount, 0.01)
        assertEquals("Dining", state.categorySpends[1].category)
        assertEquals(65.0, state.categorySpends[1].amount, 0.01)
    }

    @Test
    fun testSparklineAndRecentTransactionsOrdering() = runTest(testDispatcher) {
        val fakeTxs = listOf(
            Transaction(id = 1L, amount = 100.0, type = TransactionType.INCOME, categoryId = 1L, accountId = 1L, dateMillis = 1000L),
            Transaction(id = 2L, amount = 30.0, type = TransactionType.EXPENSE, categoryId = 2L, accountId = 1L, dateMillis = 3000L),
            Transaction(id = 3L, amount = 20.0, type = TransactionType.EXPENSE, categoryId = 2L, accountId = 1L, dateMillis = 2000L)
        )
        val fakeCats = listOf(
            Category(id = 1L, name = "Salary", emoji = "💰", colorIndex = 0, type = TransactionType.INCOME),
            Category(id = 2L, name = "Food", emoji = "🍔", colorIndex = 1, type = TransactionType.EXPENSE)
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

        val state = viewModel.state.value
        // Recent transactions should be ordered desc by dateMillis: 3000L, 2000L, 1000L
        assertEquals(3000L, state.recentTransactions[0].dateMillis)
        assertEquals(2000L, state.recentTransactions[1].dateMillis)
        assertEquals(1000L, state.recentTransactions[2].dateMillis)

        // Sparkline should follow chronological order:
        // tx 1 (1000L): +100 -> 100
        // tx 3 (2000L): -20 -> 80
        // tx 2 (3000L): -30 -> 50
        assertEquals(listOf(100f, 80f, 50f), state.sparklinePoints)
    }

    @Test
    fun testUpdateMonthlyBudgetIntent() = runTest(testDispatcher) {
        val userRepo = FakeUserProfileRepository(
            com.monetracka.shared.domain.model.UserProfile(userName = "Thanh", monthlyBudgetLimit = 2500.0)
        )
        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList()),
            userProfileRepository = userRepo
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(HomeIntent.UpdateMonthlyBudget(3200.0))
        testScheduler.advanceUntilIdle()

        assertEquals(3200.0, viewModel.state.value.monthlyBudgetLimit)
    }

    @Test
    fun testToggleHideBalance() = runTest(testDispatcher) {
        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList())
        )
        testScheduler.advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isBalanceHidden)
        viewModel.onIntent(HomeIntent.ToggleHideBalance)
        assertEquals(true, viewModel.state.value.isBalanceHidden)
        viewModel.onIntent(HomeIntent.ToggleHideBalance)
        assertEquals(false, viewModel.state.value.isBalanceHidden)
    }

    @Test
    fun testCreateTransactionWithSpecificAccount() = runTest(testDispatcher) {
        val txRepo = FakeTransactionRepository(emptyList())
        val viewModel = HomeScreenModel(
            transactionRepository = txRepo,
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList())
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(
            HomeIntent.CreateTransaction(
                amount = 45.0,
                type = TransactionType.EXPENSE,
                categoryId = 2L,
                accountId = 5L,
                note = "Groceries"
            )
        )
        testScheduler.advanceUntilIdle()

        assertEquals(1, txRepo.inserted.size)
        assertEquals(5L, txRepo.inserted[0].accountId)
        assertEquals(45.0, txRepo.inserted[0].amount)
        assertEquals("Groceries", txRepo.inserted[0].note)
    }

    @Test
    fun testImportTransactionsBatch() = runTest(testDispatcher) {
        val txRepo = FakeTransactionRepository(emptyList())
        val viewModel = HomeScreenModel(
            transactionRepository = txRepo,
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList())
        )
        testScheduler.advanceUntilIdle()

        val imported = listOf(
            Transaction(id = 101L, amount = 12.0, type = TransactionType.EXPENSE, categoryId = 1L, accountId = 1L, note = "A", dateMillis = 1000L, createdAtMillis = 1000L),
            Transaction(id = 102L, amount = 50.0, type = TransactionType.INCOME, categoryId = 2L, accountId = 1L, note = "B", dateMillis = 2000L, createdAtMillis = 2000L)
        )
        viewModel.onIntent(HomeIntent.ImportTransactions(imported))
        testScheduler.advanceUntilIdle()

        assertEquals(2, txRepo.inserted.size)
        assertEquals("A", txRepo.inserted[0].note)
        assertEquals("B", txRepo.inserted[1].note)
    }

    @Test
    fun testUpdateCategoryBudget() = runTest(testDispatcher) {
        val budgetRepo = FakeCategoryBudgetRepository()
        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList()),
            categoryBudgetRepository = budgetRepo
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(HomeIntent.UpdateCategoryBudget(categoryId = 3L, newLimit = 450.0))
        testScheduler.advanceUntilIdle()

        assertEquals(450.0, budgetRepo.budgets[3L]?.monthlyLimit)
    }

    @Test
    fun testBiometricTogglePersistence() = runTest(testDispatcher) {
        val userRepo = FakeUserProfileRepository(
            com.monetracka.shared.domain.model.UserProfile(userName = "Thanh", isBiometricEnabled = false)
        )
        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList()),
            userProfileRepository = userRepo
        )
        testScheduler.advanceUntilIdle()
        assertEquals(false, viewModel.state.value.isBiometricEnabled)

        viewModel.onIntent(HomeIntent.SetBiometricEnabled(true))
        testScheduler.advanceUntilIdle()

        assertEquals(true, userRepo.isBiometricEnabled())
        assertEquals(true, viewModel.state.value.isBiometricEnabled)
    }

    @Test
    fun testCreateCustomCategory() = runTest(testDispatcher) {
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = catRepo,
            accountRepository = FakeAccountRepository(emptyList())
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(HomeIntent.CreateCategory(name = "Gaming", emoji = "🎮", type = TransactionType.EXPENSE))
        testScheduler.advanceUntilIdle()

        assertEquals(1, catRepo.inserted.size)
        assertEquals("Gaming", catRepo.inserted.first().name)
        assertEquals("🎮", catRepo.inserted.first().emoji)
        assertEquals(TransactionType.EXPENSE, catRepo.inserted.first().type)
        assertEquals(false, catRepo.inserted.first().isDefault)
    }
}

