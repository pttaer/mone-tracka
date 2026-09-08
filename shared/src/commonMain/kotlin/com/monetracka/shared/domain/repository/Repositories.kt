package com.monetracka.shared.domain.repository

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
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


