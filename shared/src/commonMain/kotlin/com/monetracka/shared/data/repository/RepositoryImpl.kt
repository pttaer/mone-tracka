package com.monetracka.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.monetracka.db.MoneTrackaDatabase
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

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

    override fun getTransactionsByMonth(yearMonth: String): Flow<List<Transaction>> {
        val (startMillis, endMillis) = getMonthRange(yearMonth)
        return queries.getTransactionsByMonth(startMillis, endMillis)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return queries.getTransactionsByCategory(categoryId)
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
            note = transaction.note,
            dateMillis = transaction.dateMillis,
            createdAtMillis = now,
        )
        return queries.lastInsertedTransactionId().executeAsOne()
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        queries.updateTransaction(
            amount = transaction.amount,
            type = transaction.type.name,
            categoryId = transaction.categoryId,
            note = transaction.note,
            dateMillis = transaction.dateMillis,
            id = transaction.id,
        )
    }

    override suspend fun deleteTransaction(id: Long) {
        queries.deleteTransaction(id)
    }

    override fun getTotalByTypeAndMonth(type: TransactionType, yearMonth: String): Flow<Double> {
        val (startMillis, endMillis) = getMonthRange(yearMonth)
        return queries.getTotalByTypeAndDateRange(type.name, startMillis, endMillis)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    private fun getMonthRange(yearMonth: String): Pair<Long, Long> {
        val parts = yearMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val startDate = kotlinx.datetime.LocalDate(year, month, 1)
        val endDate = if (month == 12) {
            kotlinx.datetime.LocalDate(year + 1, 1, 1)
        } else {
            kotlinx.datetime.LocalDate(year, month + 1, 1)
        }

        val tz = TimeZone.currentSystemDefault()
        val startMillis = startDate.atStartOfDayIn(tz).toEpochMilliseconds()
        val endMillis = endDate.atStartOfDayIn(tz).toEpochMilliseconds()
        return Pair(startMillis, endMillis)
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

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return queries.getCategoriesByType(type.name)
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

    override suspend fun updateCategory(category: Category) {
        queries.updateCategory(
            name = category.name,
            emoji = category.emoji,
            colorIndex = category.colorIndex.toLong(),
            id = category.id,
        )
    }

    override suspend fun deleteCategory(id: Long) {
        queries.deleteCategory(id)
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
        )

        defaults.forEach { insertCategory(it) }
    }
}

// Extension functions to map DB entities to domain models
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
    note = note,
    dateMillis = dateMillis,
    createdAtMillis = createdAtMillis,
)
