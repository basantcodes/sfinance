package com.example.ui.viewmodel

import com.example.data.local.entities.InterestFrequency
import com.example.data.local.entities.InterestMode
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanType
import com.example.data.local.entities.WishlistItem
import com.example.data.repository.FinanceRepository

class LoanWishlistJournalFeatureViewModel(
    private val repository: FinanceRepository,
    private val emitEvent: (String) -> Unit = {}
) {
    suspend fun createLoan(
        userId: String,
        counterparty: String,
        type: LoanType,
        principal: Double,
        rate: Double,
        startDate: Long,
        accountId: String?,
        mode: InterestMode,
        frequency: InterestFrequency?,
        notes: String?
    ) {
        val result = repository.createLoan(
            userId, counterparty, type, principal, rate, startDate, accountId, mode, frequency, notes
        )
        result.fold(
            onSuccess = { emitEvent("Loan with $counterparty recorded") },
            onFailure = { err -> emitEvent(err.message ?: "Failed to record loan") }
        )
    }

    suspend fun markLoanRepaid(loan: Loan, accountId: String?) {
        val result = repository.markLoanRepaid(loan, accountId)
        result.fold(
            onSuccess = { emitEvent("Loan with ${loan.counterparty} marked as repaid") },
            onFailure = { err -> emitEvent(err.message ?: "Repayment failed") }
        )
    }

    suspend fun deleteLoan(loan: Loan) {
        repository.deleteLoan(loan)
        emitEvent("Loan deleted")
    }

    suspend fun saveWishlistItem(
        userId: String,
        name: String,
        cost: Double,
        priority: String,
        preferredDate: Long?,
        category: String?,
        notes: String?
    ) {
        repository.createWishlistItem(userId, name, cost, preferredDate, priority, category, notes)
        emitEvent("Wishlist item '$name' added")
    }

    suspend fun purchaseWishlistItem(item: WishlistItem, createExpense: Boolean, accountId: String?, categoryId: String?) {
        val result = repository.purchaseWishlistItem(
            item = item,
            createTransaction = createExpense,
            accountId = accountId,
            categoryId = categoryId
        )
        result.fold(
            onSuccess = { emitEvent("Purchased '${item.name}'") },
            onFailure = { err -> emitEvent(err.message ?: "Purchase failed") }
        )
    }

    suspend fun deleteWishlistItem(item: WishlistItem) {
        repository.deleteWishlistItem(item)
        emitEvent("Wishlist item removed")
    }

    suspend fun saveJournalEntry(
        userId: String,
        content: String,
        mood: String?,
        date: Long
    ) {
        repository.createJournalEntry(userId, content, mood, date)
        emitEvent("Journal entry saved")
    }

    suspend fun deleteJournalEntry(entry: JournalEntry) {
        repository.deleteJournalEntry(entry)
        emitEvent("Journal entry deleted")
    }
}
