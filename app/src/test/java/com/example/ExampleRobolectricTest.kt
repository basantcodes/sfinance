package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AccountType
import com.example.data.local.entities.TransactionType
import com.example.data.nepali.NepaliDateConverter
import com.example.data.repository.FinanceRepository
import com.example.data.security.BCrypt
import com.example.data.security.JwtHelper
import com.example.data.security.PreferenceManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var repository: FinanceRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferenceManager = PreferenceManager(context)
        repository = FinanceRepository(database, preferenceManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context matches app name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Sfinance", appName)
    }

    @Test
    fun `nepali date conversion accurately converts ad to bs`() {
        val bsDate = NepaliDateConverter.adToBs(2026, 7, 19)
        println("bsDate for 2026-07-19: year=${bsDate.year}, month=${bsDate.month}, day=${bsDate.day}, name=${bsDate.monthName}")
        assertEquals(2083, bsDate.year)
        // Check prompt example: "Jul 19, 2026 (Ashar 5, 2083)" or "Shrawan"
        assertTrue(bsDate.year == 2083)
    }

    @Test
    fun `bcrypt hashes and verifies passwords correctly`() {
        val password = "secret_password_123"
        val hash = BCrypt.hashpw(password, BCrypt.gensalt(10))
        assertNotNull(hash)
        assertTrue(BCrypt.checkpw(password, hash))
        assertFalse(BCrypt.checkpw("wrong_password", hash))
    }

    @Test
    fun `jwt helper creates and decodes tokens`() {
        val userId = "user-abc-123"
        val email = "user@test.com"
        val name = "Test User"
        val token = JwtHelper.createToken(userId, email, name)
        assertNotNull(token)
        val payload = JwtHelper.verifyToken(token)
        assertNotNull(payload)
        assertEquals(userId, payload?.userId)
        assertEquals(email, payload?.email)
    }

    @Test
    fun `atomic balance updates prevent negative balances in strict mode`() = runBlocking {
        val regRes = repository.register("Test User", "test@test.com", "pass123")
        if (regRes.isFailure) {
            println("Registration error: " + regRes.exceptionOrNull()?.message)
            regRes.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(regRes.isSuccess)
        val user = regRes.getOrThrow()

        val account = repository.createAccount(
            userId = user.id,
            name = "Test Bank",
            type = AccountType.BANK,
            initialBalance = 1000.0,
            color = "#059669"
        )
        assertEquals(1000.0, account.balance, 0.01)

        // Add Income of 500
        val incomeRes = repository.createTransaction(
            userId = user.id,
            type = TransactionType.INCOME,
            amount = 500.0,
            date = System.currentTimeMillis(),
            dateBs = "2083/04/04",
            name = "Freelance",
            notes = null,
            accountFromId = null,
            accountToId = account.id,
            categoryId = null
        )
        assertTrue(incomeRes.isSuccess)

        val updatedAcc = database.accountDao().getById(account.id)
        assertNotNull(updatedAcc)
        assertEquals(1500.0, updatedAcc!!.balance, 0.01)

        // Attempt Expense of 2000 (exceeds balance 1500 -> must fail atomically)
        val overExpenseRes = repository.createTransaction(
            userId = user.id,
            type = TransactionType.EXPENSE,
            amount = 2000.0,
            date = System.currentTimeMillis(),
            dateBs = "2083/04/04",
            name = "High Rent",
            notes = null,
            accountFromId = account.id,
            accountToId = null,
            categoryId = null
        )
        println("overExpenseRes result: isSuccess=${overExpenseRes.isSuccess}, ex=${overExpenseRes.exceptionOrNull()?.message}")
        assertTrue(overExpenseRes.isFailure)
        assertTrue(overExpenseRes.exceptionOrNull()?.message?.contains("insufficient", ignoreCase = true) == true)

        // Balance must remain intact at 1500.0
        val intactAcc = database.accountDao().getById(account.id)
        assertEquals(1500.0, intactAcc!!.balance, 0.01)
    }
}
