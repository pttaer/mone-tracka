package com.monetracka.shared.domain.export

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

object SimpleCsvExporter {

    fun exportTransactions(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("id,amount,type,categoryId,accountId,toAccountId,note,dateMillis,createdAtMillis\n")
        for (tx in transactions) {
            val safeNote = tx.note.replace(",", ";").replace("\n", " ")
            sb.append("${tx.id},${tx.amount},${tx.type.name},${tx.categoryId},${tx.accountId},${tx.toAccountId ?: ""},$safeNote,${tx.dateMillis},${tx.createdAtMillis}\n")
        }
        return sb.toString()
    }

    fun parseTransactions(csv: String): List<Transaction> {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val results = mutableListOf<Transaction>()
        for (line in lines.drop(1)) {
            val tokens = line.split(",")
            if (tokens.size >= 9) {
                val id = tokens[0].toLongOrNull() ?: 0L
                val amount = tokens[1].toDoubleOrNull() ?: 0.0
                val type = runCatching { TransactionType.valueOf(tokens[2]) }.getOrDefault(TransactionType.EXPENSE)
                val categoryId = tokens[3].toLongOrNull() ?: 1L
                val accountId = tokens[4].toLongOrNull() ?: 1L
                val toAccountId = tokens[5].takeIf { it.isNotBlank() }?.toLongOrNull()
                val note = tokens[6]
                val dateMillis = tokens[7].toLongOrNull() ?: 0L
                val createdAtMillis = tokens[8].toLongOrNull() ?: 0L

                results.add(
                    Transaction(
                        id = id,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        toAccountId = toAccountId,
                        note = note,
                        dateMillis = dateMillis,
                        createdAtMillis = createdAtMillis
                    )
                )
            }
        }
        return results
    }
}
