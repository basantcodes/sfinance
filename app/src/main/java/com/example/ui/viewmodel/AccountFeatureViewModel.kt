package com.example.ui.viewmodel

import android.content.Context
import com.example.R
import com.example.data.local.entities.Account
import com.example.data.local.entities.AccountType
import com.example.data.repository.FinanceRepository

class AccountFeatureViewModel(
    private val context: Context,
    private val repository: FinanceRepository,
    private val emitEvent: (String) -> Unit = {}
) {
    suspend fun createAccount(
        userId: String,
        name: String,
        type: AccountType,
        balance: Double,
        color: String,
        currency: String,
        notes: String?
    ): Account {
        val account = repository.createAccount(
            userId = userId,
            name = name,
            type = type,
            initialBalance = balance,
            color = color,
            currency = currency,
            notes = notes
        )
        emitEvent(context.getString(R.string.account_created, account.name))
        return account
    }

    suspend fun updateAccount(account: Account) {
        repository.updateAccount(account)
        emitEvent(context.getString(R.string.account_updated))
    }

    suspend fun deleteAccount(account: Account) {
        val result = repository.deleteAccount(account)
        result.fold(
            onSuccess = { emitEvent(context.getString(R.string.account_deleted)) },
            onFailure = { err -> emitEvent(err.message ?: context.getString(R.string.cannot_delete_account)) }
        )
    }
}
