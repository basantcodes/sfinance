package com.example.ui.viewmodel

import com.example.data.local.entities.Account
import com.example.data.local.entities.AccountType
import com.example.data.repository.FinanceRepository

class AccountFeatureViewModel(
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
        emitEvent("Account '${account.name}' created")
        return account
    }

    suspend fun updateAccount(account: Account) {
        repository.updateAccount(account)
        emitEvent("Account updated")
    }

    suspend fun deleteAccount(account: Account) {
        val result = repository.deleteAccount(account)
        result.fold(
            onSuccess = { emitEvent("Account deleted") },
            onFailure = { err -> emitEvent(err.message ?: "Cannot delete account") }
        )
    }
}
