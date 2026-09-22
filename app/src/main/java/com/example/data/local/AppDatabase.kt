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
    version = 4,
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

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // No schema changes yet; keep the migration explicit so future releases can add real migration logic safely.
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN relatedTransactionId TEXT")
                database.execSQL("ALTER TABLE transactions ADD COLUMN feeAmount REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE loans ADD COLUMN remainingAmount REAL NOT NULL DEFAULT 0.0")
                database.execSQL("UPDATE loans SET remainingAmount = principal WHERE remainingAmount = 0.0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_finance_journal.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
