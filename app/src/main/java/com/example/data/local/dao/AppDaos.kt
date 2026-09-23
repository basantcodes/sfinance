package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.Account
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanInterest
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.User
import com.example.data.local.entities.WishlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY name ASC")
    fun getAllFlow(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAll(userId: String): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("UPDATE accounts SET balance = :newBalance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, newBalance: Double, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    fun getAllFlow(userId: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type ORDER BY name ASC")
    fun getByTypeFlow(userId: String, type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAll(userId: String): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId AND type = :type")
    suspend fun countByType(userId: String, type: String): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getAllFlow(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentFlow(userId: String, limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    suspend fun getAll(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE relatedTransactionId = :transactionId LIMIT 1")
    suspend fun getRelatedTransaction(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountFromId = :accountId OR accountToId = :accountId")
    suspend fun getByAccountId(accountId: String): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountFromId = :accountId OR accountToId = :accountId")
    suspend fun countByAccountId(accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun getByDateRange(userId: String, startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :categoryId AND type = 'EXPENSE' AND date >= :startDate AND date <= :endDate")
    suspend fun getExpensesForCategory(userId: String, categoryId: String, startDate: Long, endDate: Long): List<TransactionEntity>
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllFlow(userId: String): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getByStatusFlow(userId: String, status: String): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAll(userId: String): List<Loan>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan)

    @Update
    suspend fun update(loan: Loan)

    @Delete
    suspend fun delete(loan: Loan)

    @Query("SELECT * FROM loans WHERE userId = :userId AND status = 'ACTIVE' AND interestMode = 'AUTO' AND nextInterestDate IS NOT NULL AND nextInterestDate <= :currentTime")
    suspend fun getDueAutoLoans(userId: String, currentTime: Long): List<Loan>
}

@Dao
interface LoanInterestDao {
    @Query("SELECT * FROM loan_interests WHERE loanId = :loanId ORDER BY date DESC")
    fun getByLoanIdFlow(loanId: String): Flow<List<LoanInterest>>

    @Query("SELECT * FROM loan_interests WHERE loanId = :loanId ORDER BY date DESC")
    suspend fun getByLoanId(loanId: String): List<LoanInterest>

    @Query("SELECT * FROM loan_interests")
    suspend fun getAll(): List<LoanInterest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interest: LoanInterest)

    @Delete
    suspend fun delete(interest: LoanInterest)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getAllFlow(userId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    suspend fun getAll(userId: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId LIMIT 1")
    suspend fun getByCategory(userId: String, categoryId: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllFlow(userId: String): Flow<List<WishlistItem>>

    @Query("SELECT * FROM wishlist_items WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    fun getByStatusFlow(userId: String, status: String): Flow<List<WishlistItem>>

    @Query("SELECT * FROM wishlist_items WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAll(userId: String): List<WishlistItem>

    @Query("SELECT * FROM wishlist_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WishlistItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishlistItem)

    @Update
    suspend fun update(item: WishlistItem)

    @Delete
    suspend fun delete(item: WishlistItem)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC, time DESC, createdAt DESC")
    fun getAllFlow(userId: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC, time DESC, createdAt DESC")
    suspend fun getAll(userId: String): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry)

    @Update
    suspend fun update(entry: JournalEntry)

    @Delete
    suspend fun delete(entry: JournalEntry)
}
