package com.monetracka.shared.domain.export

import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

object SimpleCsvExporter {
    private const val HEADER = "id,amount,type,categoryId,accountId,toAccountId,note,dateMillis,createdAtMillis\n"

    fun exportTransactions(transactions: List<Transaction>): String = buildString {
        append(HEADER)
        transactions.forEach { tx ->
            val safeNote = tx.note.replace(",", ";").replace("\n", " ")
            append("${tx.id},${tx.amount},${tx.type.name},${tx.categoryId},${tx.accountId},${tx.toAccountId ?: ""},$safeNote,${tx.dateMillis},${tx.createdAtMillis}\n")
        }
    }

    fun parseTransactions(csv: String): List<Transaction> = csv.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .drop(1)
        .mapNotNull { line ->
            val t = line.split(",")
            if (t.size < 9) null else Transaction(
                id = t[0].toLongOrNull() ?: 0L,
                amount = t[1].toDoubleOrNull() ?: 0.0,
                type = runCatching { TransactionType.valueOf(t[2]) }.getOrDefault(TransactionType.EXPENSE),
                categoryId = t[3].toLongOrNull() ?: 1L,
                accountId = t[4].toLongOrNull() ?: 1L,
                toAccountId = t[5].takeIf { it.isNotBlank() }?.toLongOrNull(),
                note = t[6],
                dateMillis = t[7].toLongOrNull() ?: 0L,
                createdAtMillis = t[8].toLongOrNull() ?: 0L
            )
        }.toList()
}
