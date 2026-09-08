package com.monetracka.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.monetracka.db.MoneTrackaDatabase
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

// Extension functions to map DB entities to domain models
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

