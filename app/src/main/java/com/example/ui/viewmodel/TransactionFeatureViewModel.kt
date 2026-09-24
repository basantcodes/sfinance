package com.example.ui.viewmodel

import android.content.Context
import com.example.R
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TransactionType
import com.example.data.repository.FinanceRepository

class TransactionFeatureViewModel(
    private val context: Context,
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
        categoryId: String?,
        feeAmount: Double = 0.0
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
            categoryId = categoryId,
            feeAmount = feeAmount
        )
        result.fold(
            onSuccess = { emitEvent(context.getString(R.string.transaction_added)) },
            onFailure = { err -> emitEvent(err.message ?: context.getString(R.string.transaction_add_failed)) }
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
        newCategoryId: String?,
        newFeeAmount: Double = 0.0
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
            newCategoryId,
            newFeeAmount
        )
        result.fold(
            onSuccess = { emitEvent(context.getString(R.string.transaction_updated)) },
            onFailure = { err -> emitEvent(err.message ?: context.getString(R.string.update_failed)) }
        )
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val result = repository.deleteTransaction(transaction)
        result.fold(
            onSuccess = { emitEvent(context.getString(R.string.transaction_deleted)) },
            onFailure = { err -> emitEvent(err.message ?: context.getString(R.string.delete_failed)) }
        )
    }
}
