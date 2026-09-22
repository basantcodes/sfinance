package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.JournalDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.LoanInterestDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WishlistDao
import com.example.data.local.entities.Account
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanInterest
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.User
import com.example.data.local.entities.WishlistItem

@Database(
    entities = [
        User::class,
        Account::class,
        Category::class,
        TransactionEntity::class,
        Loan::class,
        LoanInterest::class,
        Budget::class,
        WishlistItem::class,
        JournalEntry::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun loanInterestDao(): LoanInterestDao
    abstract fun budgetDao(): BudgetDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_finance_journal.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
