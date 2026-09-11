package com.monetracka.shared.domain.repository

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.UserProfile
import com.monetracka.shared.domain.model.ExchangeRate
import com.monetracka.shared.domain.model.CategoryBudget
import com.monetracka.shared.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow



interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
}


interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun insertDefaultCategories()
}

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun insertAccount(account: Account): Long
    suspend fun deleteAccount(id: Long)
    suspend fun insertDefaultAccounts()
}

interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun updateMonthlyBudget(limit: Double)
    suspend fun updateCurrency(currency: String)
    suspend fun hasCompletedOnboarding(): Boolean
}


interface ExchangeRateRepository {
    fun getAllExchangeRates(): Flow<List<ExchangeRate>>
    suspend fun getRate(fromCurrency: String, toCurrency: String): Double?
    suspend fun saveRate(exchangeRate: ExchangeRate)
    suspend fun insertDefaultExchangeRates()
}

interface CategoryBudgetRepository {
    fun getAllCategoryBudgets(): Flow<List<CategoryBudget>>
    suspend fun getBudget(categoryId: Long): CategoryBudget?
    suspend fun saveBudget(budget: CategoryBudget)
    suspend fun deleteBudget(categoryId: Long)
}

interface RecurringTransactionRepository {
    fun getAllRecurring(): Flow<List<RecurringTransaction>>
    suspend fun getDueRecurring(currentMillis: Long): List<RecurringTransaction>
    suspend fun insertRecurring(recurring: RecurringTransaction): Long
    suspend fun updateNextDueDate(id: Long, nextDueDateMillis: Long)
    suspend fun deleteRecurring(id: Long)
    suspend fun processDueRecurring(currentMillis: Long): Int
}



