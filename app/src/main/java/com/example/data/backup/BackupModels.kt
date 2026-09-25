package com.example.data.backup

import com.example.data.local.entities.Account
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanInterest
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.User
import com.example.data.local.entities.WishlistItem

const val BACKUP_FORMAT_VERSION = 1
const val BACKUP_FILE_NAME = "sfinance-backup-v1.bin"

/** The encrypted payload contains the complete Room data needed to recreate the local app state. */
data class BackupPayload(
    val users: List<User>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<TransactionEntity>,
    val loans: List<Loan>,
    val loanInterests: List<LoanInterest>,
    val budgets: List<Budget>,
    val wishlistItems: List<WishlistItem>,
    val journalEntries: List<JournalEntry>
)

data class BackupEnvelope(
    val formatVersion: Int,
    val appVersion: String,
    val databaseVersion: Int,
    val createdAt: Long,
    val payload: String,
    val iv: String,
    val salt: String
)

data class BackupSummary(
    val createdAt: Long,
    val appVersion: String,
    val formatVersion: Int
)
