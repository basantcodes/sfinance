package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class AccountType {
    BANK, WALLET, CASH, INVESTMENT
}

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER, LEND, BORROW
}

enum class CategoryType {
    INCOME, EXPENSE
}

enum class LoanType {
    LEND, BORROW
}

enum class InterestMode {
    AUTO, MANUAL
}

enum class InterestFrequency {
    MONTHLY, QUARTERLY, ANNUALLY
}

enum class LoanStatus {
    ACTIVE, REPAID, CLOSED
}

enum class WishlistPriority {
    HIGH, MEDIUM, LOW
}

enum class WishlistStatus {
    PLANNED, IN_PROGRESS, PURCHASED
}

enum class Mood {
    HAPPY, SAD, ANXIOUS, GRATEFUL, NEUTRAL
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val password: String, // bcrypt hash
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["userId"])]
)
data class Account(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val type: String = AccountType.BANK.name, // BANK, WALLET, CASH, INVESTMENT
    val balance: Double = 0.0,
    val color: String = "#6366f1",
    val currency: String = "NPR",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["userId"])]
)
data class Category(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val type: String = CategoryType.EXPENSE.name, // INCOME, EXPENSE
    val icon: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["accountFromId"]),
        Index(value = ["accountToId"]),
        Index(value = ["categoryId"]),
        Index(value = ["loanId"]),
        Index(value = ["date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: String = TransactionType.EXPENSE.name, // INCOME, EXPENSE, TRANSFER, LEND, BORROW
    val name: String? = null,
    val notes: String? = null,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val dateBs: String, // "YYYY/MM/DD"
    val accountFromId: String? = null,
    val accountToId: String? = null,
    val categoryId: String? = null,
    val loanId: String? = null,
    val relatedTransactionId: String? = null,
    val feeAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loans",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["accountId"])
    ]
)
data class Loan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val counterparty: String,
    val type: String = LoanType.LEND.name, // LEND, BORROW
    val principal: Double,
    val interestRate: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val accountId: String? = null,
    val interestMode: String = InterestMode.MANUAL.name, // AUTO, MANUAL
    val nextInterestDate: Long? = null,
    val interestFrequency: String? = InterestFrequency.MONTHLY.name, // MONTHLY, QUARTERLY, ANNUALLY
    val remainingAmount: Double = principal,
    val status: String = LoanStatus.ACTIVE.name, // ACTIVE, REPAID, CLOSED
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loan_interests",
    indices = [Index(value = ["loanId"])]
)
data class LoanInterest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loanId: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val dateBs: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["userId", "categoryId"], unique = true)
    ]
)
data class Budget(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val categoryId: String,
    val monthlyLimit: Double,
    val rollover: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wishlist_items",
    indices = [Index(value = ["userId"])]
)
data class WishlistItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val estimatedCost: Double,
    val preferredDate: Long? = null,
    val priority: String = WishlistPriority.MEDIUM.name, // HIGH, MEDIUM, LOW
    val status: String = WishlistStatus.PLANNED.name, // PLANNED, IN_PROGRESS, PURCHASED
    val category: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"])
    ]
)
data class JournalEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val content: String,
    val date: Long = System.currentTimeMillis(),
    val dateBs: String,
    val time: String, // "HH:mm"
    val mood: String? = Mood.NEUTRAL.name, // HAPPY, SAD, ANXIOUS, GRATEFUL, NEUTRAL
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
