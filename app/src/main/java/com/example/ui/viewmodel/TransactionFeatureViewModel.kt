package com.example.ui.viewmodel

import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TransactionType
import com.example.data.repository.FinanceRepository

class TransactionFeatureViewModel(
    private val repository: FinanceRepository,
    private val emitEvent: (String) -> Unit = {}
) {
    suspend fun createTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        date: Long,
        dateBs: String,
        name: String?,
        notes: String?,
        fromAccountId: String?,
        toAccountId: String?,
        categoryId: String?
    ) {
        val result = repository.createTransaction(
            userId = userId,
            type = type,
            amount = amount,
            date = date,
            dateBs = dateBs,
            name = name,
            notes = notes,
            accountFromId = fromAccountId,
            accountToId = toAccountId,
            categoryId = categoryId
        )
        result.fold(
            onSuccess = { emitEvent("Transaction added successfully") },
            onFailure = { err -> emitEvent(err.message ?: "Failed to add transaction") }
        )
    }

    suspend fun updateTransaction(
        oldTransaction: TransactionEntity,
        newType: TransactionType,
        newAmount: Double,
        newDate: Long,
        newDateBs: String,
        newName: String?,
        newNotes: String?,
        newAccountFromId: String?,
        newAccountToId: String?,
        newCategoryId: String?
    ) {
        val result = repository.updateTransaction(
            oldTransaction,
            newType,
            newAmount,
            newDate,
            newDateBs,
            newName,
            newNotes,
            newAccountFromId,
            newAccountToId,
            newCategoryId
        )
        result.fold(
            onSuccess = { emitEvent("Transaction updated") },
            onFailure = { err -> emitEvent(err.message ?: "Update failed") }
        )
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val result = repository.deleteTransaction(transaction)
        result.fold(
            onSuccess = { emitEvent("Transaction deleted") },
            onFailure = { err -> emitEvent(err.message ?: "Delete failed") }
        )
    }
}
