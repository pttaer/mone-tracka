package com.monetracka.shared.ui.onboarding

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.UserProfile
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.UserProfileRepository
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
import kotlin.test.assertTrue

class OnboardingFakeUserProfileRepo : UserProfileRepository {
    var savedProfile: UserProfile? = null
    override fun getUserProfile(): Flow<UserProfile?> = flowOf(savedProfile)
    override suspend fun saveUserProfile(profile: UserProfile) {
        savedProfile = profile
    }
    override suspend fun hasCompletedOnboarding(): Boolean = savedProfile?.hasCompletedOnboarding ?: false
}

class OnboardingFakeAccountRepo : AccountRepository {
    val insertedAccounts = mutableListOf<Account>()
    override fun getAllAccounts(): Flow<List<Account>> = flowOf(insertedAccounts)
    override suspend fun insertAccount(account: Account): Long {
        insertedAccounts.add(account)
        return insertedAccounts.size.toLong()
    }
    override suspend fun deleteAccount(id: Long) {}
    override suspend fun insertDefaultAccounts() {}
}

class OnboardingFakeCategoryRepo : CategoryRepository {
    var defaultCategoriesInserted = false
    override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun insertDefaultCategories() {
        defaultCategoriesInserted = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingScreenModelTest {
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
    fun testStepNavigationAndInitials() {
        val userRepo = OnboardingFakeUserProfileRepo()
        val accountRepo = OnboardingFakeAccountRepo()
        val categoryRepo = OnboardingFakeCategoryRepo()
        val model = OnboardingScreenModel(userRepo, accountRepo, categoryRepo)

        assertEquals(0, model.state.value.step)
        model.onNameChanged("Alice Wonderland")
        assertEquals("AW", model.state.value.userInitials)

        model.nextStep()
        assertEquals(1, model.state.value.step)

        model.onCurrencySelected("EUR")
        assertEquals("EUR", model.state.value.selectedCurrency)

        model.nextStep()
        assertEquals(2, model.state.value.step)

        model.onCheckingBalanceChanged("1500.50")
        model.onCashBalanceChanged("200")
        assertEquals("1500.50", model.state.value.checkingBalance)
        assertEquals("200", model.state.value.cashBalance)

        model.nextStep()
        assertEquals(3, model.state.value.step)

        model.prevStep()
        assertEquals(2, model.state.value.step)
    }

    @Test
    fun testCompleteOnboarding() = runTest(testDispatcher) {
        val userRepo = OnboardingFakeUserProfileRepo()
        val accountRepo = OnboardingFakeAccountRepo()
        val categoryRepo = OnboardingFakeCategoryRepo()
        val model = OnboardingScreenModel(userRepo, accountRepo, categoryRepo)

        model.onNameChanged("Bob Dylan")
        model.onCurrencySelected("GBP")
        model.onCheckingBalanceChanged("2500")
        model.onCashBalanceChanged("300")

        var successCalled = false
        model.completeOnboarding {
            successCalled = true
        }

        testScheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(categoryRepo.defaultCategoriesInserted)
        assertEquals(2, accountRepo.insertedAccounts.size)
        assertEquals("Main Checking", accountRepo.insertedAccounts[0].name)
        assertEquals(2500.0, accountRepo.insertedAccounts[0].initialBalance)
        assertEquals("Cash Wallet", accountRepo.insertedAccounts[1].name)
        assertEquals(300.0, accountRepo.insertedAccounts[1].initialBalance)

        val profile = userRepo.savedProfile
        assertEquals("Bob Dylan", profile?.userName)
        assertEquals("GBP", profile?.currency)
        assertEquals(true, profile?.hasCompletedOnboarding)
    }

    @Test
    fun testBalanceSanitizationAndDoubleCompleteGuard() = runTest(testDispatcher) {
        val userRepo = OnboardingFakeUserProfileRepo()
        val accountRepo = OnboardingFakeAccountRepo()
        val categoryRepo = OnboardingFakeCategoryRepo()
        val model = OnboardingScreenModel(userRepo, accountRepo, categoryRepo)

        model.onCheckingBalanceChanged("12.34.56")
        assertEquals("12.3456", model.state.value.checkingBalance)

        var callCount = 0
        model.completeOnboarding { callCount++ }
        model.completeOnboarding { callCount++ }
        testScheduler.advanceUntilIdle()

        assertEquals(1, callCount)
        assertEquals(2, accountRepo.insertedAccounts.size)
    }
}
