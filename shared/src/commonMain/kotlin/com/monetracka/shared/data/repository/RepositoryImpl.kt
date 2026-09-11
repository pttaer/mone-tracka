package com.monetracka.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.monetracka.db.MoneTrackaDatabase
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.model.UserProfile
import com.monetracka.shared.domain.model.ExchangeRate
import com.monetracka.shared.domain.model.CategoryBudget
import com.monetracka.shared.domain.model.RecurringTransaction
import com.monetracka.shared.domain.model.RecurringInterval
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import com.monetracka.shared.domain.repository.UserProfileRepository
import com.monetracka.shared.domain.repository.ExchangeRateRepository
import com.monetracka.shared.domain.repository.CategoryBudgetRepository
import com.monetracka.shared.domain.repository.RecurringTransactionRepository


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class TransactionRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : TransactionRepository {

    private val queries = database.moneTrackaDatabaseQueries

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return queries.getAllTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return queries.searchTransactions(query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }


    override suspend fun insertTransaction(transaction: Transaction): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertTransaction(
            amount = transaction.amount,
            type = transaction.type.name,
            categoryId = transaction.categoryId,
            accountId = transaction.accountId,
            toAccountId = transaction.toAccountId,
            note = transaction.note,
            dateMillis = transaction.dateMillis,
            createdAtMillis = now,
        )
        return queries.lastInsertedTransactionId().executeAsOne()
    }

    override suspend fun deleteTransaction(id: Long) {
        queries.deleteTransaction(id)
    }
}

class CategoryRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : CategoryRepository {

    private val queries = database.moneTrackaDatabaseQueries

    override fun getAllCategories(): Flow<List<Category>> {
        return queries.getAllCategories()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertCategory(category: Category): Long {
        queries.insertCategory(
            name = category.name,
            emoji = category.emoji,
            colorIndex = category.colorIndex.toLong(),
            type = category.type.name,
            isDefault = if (category.isDefault) 1L else 0L,
        )
        return queries.lastInsertedCategoryId().executeAsOne()
    }

    override suspend fun insertDefaultCategories() {
        val count = queries.getCategoryCount().executeAsOne()
        if (count > 0) return

        val defaults = listOf(
            // Expense categories
            Category(name = "Food & Drinks", emoji = "🍔", colorIndex = 0, type = TransactionType.EXPENSE),
            Category(name = "Transport", emoji = "🚗", colorIndex = 1, type = TransactionType.EXPENSE),
            Category(name = "Shopping", emoji = "🛍️", colorIndex = 2, type = TransactionType.EXPENSE),
            Category(name = "Bills & Utilities", emoji = "💡", colorIndex = 3, type = TransactionType.EXPENSE),
            Category(name = "Entertainment", emoji = "🎬", colorIndex = 4, type = TransactionType.EXPENSE),
            Category(name = "Health", emoji = "💊", colorIndex = 5, type = TransactionType.EXPENSE),
            Category(name = "Education", emoji = "📚", colorIndex = 6, type = TransactionType.EXPENSE),
            Category(name = "Groceries", emoji = "🛒", colorIndex = 7, type = TransactionType.EXPENSE),
            Category(name = "Subscriptions", emoji = "📱", colorIndex = 8, type = TransactionType.EXPENSE),
            Category(name = "Other", emoji = "📦", colorIndex = 9, type = TransactionType.EXPENSE),
            // Income categories
            Category(name = "Salary", emoji = "💰", colorIndex = 0, type = TransactionType.INCOME),
            Category(name = "Freelance", emoji = "💻", colorIndex = 1, type = TransactionType.INCOME),
            Category(name = "Investment", emoji = "📈", colorIndex = 2, type = TransactionType.INCOME),
            Category(name = "Gift", emoji = "🎁", colorIndex = 3, type = TransactionType.INCOME),
            Category(name = "Other Income", emoji = "💵", colorIndex = 4, type = TransactionType.INCOME),
            // Transfer category
            Category(name = "Transfer", emoji = "🔁", colorIndex = 0, type = TransactionType.TRANSFER),
        )

        defaults.forEach { insertCategory(it) }
    }
}

class AccountRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : AccountRepository {

    private val queries = database.moneTrackaDatabaseQueries

    override fun getAllAccounts(): Flow<List<Account>> {
        return queries.getAllAccounts()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertAccount(account: Account): Long {
        queries.insertAccount(
            name = account.name,
            emoji = account.emoji,
            initialBalance = account.initialBalance,
            description = account.description,
        )
        return queries.lastInsertedAccountId().executeAsOne()
    }

    override suspend fun deleteAccount(id: Long) {
        queries.deleteAccount(id)
    }

    override suspend fun insertDefaultAccounts() {
        val count = queries.getAccountCount().executeAsOne()
        if (count > 0) return

        insertAccount(Account(name = "Main Checking", emoji = "🏦", initialBalance = 0.0, description = "Daily operational account"))
        insertAccount(Account(name = "Cash Wallet", emoji = "💵", initialBalance = 0.0, description = "Physical cash on hand"))
    }
}

class UserProfileRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : UserProfileRepository {

    private val queries = database.moneTrackaDatabaseQueries

    override fun getUserProfile(): Flow<UserProfile?> {
        return queries.getUserProfile()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        withContext(Dispatchers.IO) {
            queries.insertOrUpdateUserProfile(
                userName = profile.userName,
                currency = profile.currency,
                hasCompletedOnboarding = if (profile.hasCompletedOnboarding) 1L else 0L,
                monthlyBudgetLimit = profile.monthlyBudgetLimit
            )
        }
    }

    override suspend fun updateMonthlyBudget(limit: Double) {
        withContext(Dispatchers.IO) {
            queries.updateMonthlyBudget(limit)
        }
    }

    override suspend fun updateCurrency(currency: String) {
        withContext(Dispatchers.IO) {
            queries.updateCurrency(currency)
        }
    }

    override suspend fun hasCompletedOnboarding(): Boolean = withContext(Dispatchers.IO) {
        val profile = queries.getUserProfile().executeAsOneOrNull()
        profile?.hasCompletedOnboarding == 1L
    }
}


class ExchangeRateRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : ExchangeRateRepository {
    private val queries = database.moneTrackaDatabaseQueries

    override fun getAllExchangeRates(): Flow<List<ExchangeRate>> {
        return queries.getAllExchangeRates()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getRate(fromCurrency: String, toCurrency: String): Double? {
        return withContext(Dispatchers.IO) {
            queries.getExchangeRate(fromCurrency, toCurrency).executeAsOneOrNull()?.rate
        }
    }

    override suspend fun saveRate(exchangeRate: ExchangeRate) {
        withContext(Dispatchers.IO) {
            queries.insertOrUpdateExchangeRate(
                fromCurrency = exchangeRate.fromCurrency,
                toCurrency = exchangeRate.toCurrency,
                rate = exchangeRate.rate,
                updatedAtMillis = exchangeRate.updatedAtMillis
            )
        }
    }

    override suspend fun insertDefaultExchangeRates() {
        withContext(Dispatchers.IO) {
            val count = queries.getExchangeRateCount().executeAsOne()
            if (count > 0) return@withContext

            val now = Clock.System.now().toEpochMilliseconds()
            val defaults = listOf(
                ExchangeRate("USD", "EUR", 0.92, now),
                ExchangeRate("USD", "GBP", 0.79, now),
                ExchangeRate("USD", "VND", 25450.0, now),
                ExchangeRate("USD", "JPY", 155.0, now),
                ExchangeRate("EUR", "USD", 1.087, now),
                ExchangeRate("GBP", "USD", 1.266, now),
                ExchangeRate("VND", "USD", 0.0000393, now),
                ExchangeRate("JPY", "USD", 0.00645, now)
            )
            defaults.forEach { rate ->
                queries.insertOrUpdateExchangeRate(
                    fromCurrency = rate.fromCurrency,
                    toCurrency = rate.toCurrency,
                    rate = rate.rate,
                    updatedAtMillis = rate.updatedAtMillis
                )
            }
        }
    }
}

class CategoryBudgetRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : CategoryBudgetRepository {
    private val queries = database.moneTrackaDatabaseQueries

    override fun getAllCategoryBudgets(): Flow<List<CategoryBudget>> {
        return queries.getAllCategoryBudgets()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBudget(categoryId: Long): CategoryBudget? {
        return withContext(Dispatchers.IO) {
            queries.getCategoryBudget(categoryId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun saveBudget(budget: CategoryBudget) {
        withContext(Dispatchers.IO) {
            queries.insertOrUpdateCategoryBudget(
                categoryId = budget.categoryId,
                monthlyLimit = budget.monthlyLimit,
                rolloverEnabled = if (budget.rolloverEnabled) 1L else 0L
            )
        }
    }

    override suspend fun deleteBudget(categoryId: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteCategoryBudget(categoryId)
        }
    }
}

class RecurringTransactionRepositoryImpl(
    private val database: MoneTrackaDatabase,
) : RecurringTransactionRepository {
    private val queries = database.moneTrackaDatabaseQueries

    override fun getAllRecurring(): Flow<List<RecurringTransaction>> {
        return queries.getAllRecurringTransactions()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getDueRecurring(currentMillis: Long): List<RecurringTransaction> {
        return withContext(Dispatchers.IO) {
            queries.getDueRecurringTransactions(currentMillis).executeAsList().map { it.toDomain() }
        }
    }

    override suspend fun insertRecurring(recurring: RecurringTransaction): Long {
        return withContext(Dispatchers.IO) {
            queries.insertRecurringTransaction(
                title = recurring.title,
                amount = recurring.amount,
                type = recurring.type.name,
                categoryId = recurring.categoryId,
                accountId = recurring.accountId,
                intervalType = recurring.intervalType.name,
                intervalCount = recurring.intervalCount.toLong(),
                nextDueDateMillis = recurring.nextDueDateMillis,
                autoPost = if (recurring.autoPost) 1L else 0L
            )
            queries.lastInsertedTransactionId().executeAsOne()
        }
    }

    override suspend fun updateNextDueDate(id: Long, nextDueDateMillis: Long) {
        withContext(Dispatchers.IO) {
            queries.updateRecurringNextDueDate(nextDueDateMillis = nextDueDateMillis, id = id)
        }
    }

    override suspend fun deleteRecurring(id: Long) {
        withContext(Dispatchers.IO) {
            queries.deleteRecurringTransaction(id)
        }
    }

    override suspend fun processDueRecurring(currentMillis: Long): Int {
        return withContext(Dispatchers.IO) {
            val dueList = queries.getDueRecurringTransactions(currentMillis).executeAsList()
            var processed = 0
            for (rec in dueList) {
                // Post transaction
                queries.insertTransaction(
                    amount = rec.amount,
                    type = rec.type,
                    categoryId = rec.categoryId,
                    accountId = rec.accountId,
                    toAccountId = null,
                    note = rec.title,
                    dateMillis = rec.nextDueDateMillis,
                    createdAtMillis = currentMillis
                )
                // Advance next due date by interval
                val nextIntervalMillis = when (rec.intervalType) {
                    "DAILY" -> 86_400_000L * rec.intervalCount
                    "WEEKLY" -> 7 * 86_400_000L * rec.intervalCount
                    "MONTHLY" -> 30 * 86_400_000L * rec.intervalCount
                    "YEARLY" -> 365 * 86_400_000L * rec.intervalCount
                    else -> 30 * 86_400_000L
                }
                queries.updateRecurringNextDueDate(
                    nextDueDateMillis = rec.nextDueDateMillis + nextIntervalMillis,
                    id = rec.id
                )
                processed++
            }
            processed
        }
    }
}

// Extension functions to map DB entities to domain models
private fun com.monetracka.db.RecurringTransactionEntity.toDomain() = RecurringTransaction(
    id = id,
    title = title,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    accountId = accountId,
    intervalType = runCatching { RecurringInterval.valueOf(intervalType) }.getOrDefault(RecurringInterval.MONTHLY),
    intervalCount = intervalCount.toInt(),
    nextDueDateMillis = nextDueDateMillis,
    autoPost = autoPost == 1L
)

private fun com.monetracka.db.CategoryBudgetEntity.toDomain() = CategoryBudget(
    categoryId = categoryId,
    monthlyLimit = monthlyLimit,
    rolloverEnabled = rolloverEnabled == 1L
)

private fun com.monetracka.db.ExchangeRateEntity.toDomain() = ExchangeRate(
    fromCurrency = fromCurrency,
    toCurrency = toCurrency,
    rate = rate,
    updatedAtMillis = updatedAtMillis
)


private fun com.monetracka.db.UserProfileEntity.toDomain() = UserProfile(
    userName = userName,
    currency = currency,
    hasCompletedOnboarding = hasCompletedOnboarding == 1L,
    monthlyBudgetLimit = monthlyBudgetLimit
)

private fun com.monetracka.db.AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    emoji = emoji,
    initialBalance = initialBalance,
    description = description,
)

private fun com.monetracka.db.CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    emoji = emoji,
    colorIndex = colorIndex.toInt(),
    type = TransactionType.valueOf(type),
    isDefault = isDefault == 1L,
)

private fun com.monetracka.db.TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    note = note,
    dateMillis = dateMillis,
    createdAtMillis = createdAtMillis,
)


