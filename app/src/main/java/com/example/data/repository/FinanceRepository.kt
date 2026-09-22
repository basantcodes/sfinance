package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.Account
import com.example.data.local.entities.AccountType
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.CategoryType
import com.example.data.local.entities.InterestFrequency
import com.example.data.local.entities.InterestMode
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.LoanInterest
import com.example.data.local.entities.LoanStatus
import com.example.data.local.entities.LoanType
import com.example.data.local.entities.Mood
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TransactionType
import com.example.data.local.entities.User
import com.example.data.local.entities.WishlistItem
import com.example.data.local.entities.WishlistPriority
import com.example.data.local.entities.WishlistStatus
import com.example.data.nepali.NepaliDateConverter
import com.example.data.security.BCrypt
import com.example.data.security.JwtHelper
import com.example.data.security.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class InsufficientBalanceException(message: String) : Exception(message)

data class DashboardData(
    val netWorth: Double = 0.0,
    val totalCash: Double = 0.0,
    val totalBank: Double = 0.0,
    val totalWallet: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val activeLoansCount: Int = 0,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netReceivable: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlySaved: Double = 0.0,
    val savingsRate: Double = 0.0,
    val expenseByCategory: List<CategoryExpenseShare> = emptyList(),
    val incomeExpenseTrend: List<MonthlyTrendPoint> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val budgetsWithProgress: List<BudgetProgress> = emptyList()
)

data class CategoryExpenseShare(
    val categoryName: String,
    val amount: Double,
    val percentage: Float,
    val color: String
)

data class MonthlyTrendPoint(
    val monthLabel: String,
    val monthIndex: Int,
    val year: Int,
    val income: Double,
    val expense: Double
)

data class BudgetProgress(
    val budget: Budget,
    val categoryName: String,
    val spent: Double,
    val limit: Double,
    val percentage: Float, // e.g. 0.75 for 75%
    val isOverBudget: Boolean
)

enum class Affordability {
    CAN_AFFORD_NOW,
    CAN_AFFORD_BY_DATE,
    CANNOT_AFFORD
}

data class WishlistItemWithAffordability(
    val item: WishlistItem,
    val affordability: Affordability,
    val savingsNeeded: Double
)

class FinanceRepository(
    private val database: AppDatabase,
    private val preferenceManager: PreferenceManager
) {
    private val userDao = database.userDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val loanDao = database.loanDao()
    private val loanInterestDao = database.loanInterestDao()
    private val budgetDao = database.budgetDao()
    private val wishlistDao = database.wishlistDao()
    private val journalDao = database.journalDao()

    // --- AUTH ---
    suspend fun register(name: String, email: String, passwordPlain: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val existing = userDao.getByEmail(trimmedEmail)
        if (existing != null) {
            return Result.failure(Exception("User with this email already exists"))
        }
        val hashedPassword = BCrypt.hashpw(passwordPlain)
        val user = User(
            name = name.trim(),
            email = trimmedEmail,
            password = hashedPassword
        )
        userDao.insert(user)
        // Seed default categories and initial cash account
        seedDefaultCategories(user.id)
        accountDao.insert(
            Account(
                userId = user.id,
                name = "Primary Cash",
                type = AccountType.CASH.name,
                balance = 0.0,
                color = "#10b981",
                currency = "NPR"
            )
        )
        val token = JwtHelper.createToken(user.id, user.email, user.name)
        preferenceManager.saveAuthToken(token, user.id, user.email, user.name)
        return Result.success(user)
    }

    suspend fun login(email: String, passwordPlain: String): Result<User> {
        val trimmedEmail = email.trim().lowercase()
        val user = userDao.getByEmail(trimmedEmail) ?: return Result.failure(Exception("Invalid email or password"))
        if (!BCrypt.checkpw(passwordPlain, user.password)) {
            return Result.failure(Exception("Invalid email or password"))
        }
        val token = JwtHelper.createToken(user.id, user.email, user.name)
        preferenceManager.saveAuthToken(token, user.id, user.email, user.name)
        return Result.success(user)
    }

    suspend fun getActiveUser(): User? {
        val userId = preferenceManager.getActiveUserId() ?: return null
        val token = preferenceManager.getAuthToken() ?: return null
        val payload = JwtHelper.verifyToken(token) ?: return null
        return userDao.getById(payload.userId)
    }

    fun logout() {
        preferenceManager.clearAuth()
    }

    // --- CATEGORIES ---
    fun getCategoriesFlow(userId: String): Flow<List<Category>> = categoryDao.getAllFlow(userId)
    suspend fun getCategories(userId: String): List<Category> = categoryDao.getAll(userId)

    suspend fun createCategory(userId: String, name: String, type: CategoryType, icon: String? = null): Category {
        val category = Category(
            userId = userId,
            name = name.trim(),
            type = type.name,
            icon = icon,
            isDefault = false
        )
        categoryDao.insert(category)
        return category
    }

    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun seedDefaultCategories(userId: String) {
        val defaultExpenses = listOf(
            "Food & Dining", "Transport", "Groceries", "Shopping", "Entertainment",
            "Bills & Utilities", "Healthcare", "Education", "Personal Care", "Rent",
            "Subscriptions", "Gifts", "Travel", "Others", "Utilities", "Gym", "Other Expense"
        )
        val defaultIncomes = listOf(
            "Salary", "Freelance", "Business", "Investment Returns", "Gift Received",
            "Refund", "Others", "Other Gig"
        )

        val categories = mutableListOf<Category>()
        defaultExpenses.forEach { name ->
            categories.add(Category(userId = userId, name = name, type = CategoryType.EXPENSE.name, isDefault = true))
        }
        defaultIncomes.forEach { name ->
            categories.add(Category(userId = userId, name = name, type = CategoryType.INCOME.name, isDefault = true))
        }
        categoryDao.insertAll(categories)
    }

    // --- ACCOUNTS ---
    fun getAccountsFlow(userId: String): Flow<List<Account>> = accountDao.getAllFlow(userId)
    suspend fun getAccounts(userId: String): List<Account> = accountDao.getAll(userId)
    suspend fun getAccountById(id: String): Account? = accountDao.getById(id)

    suspend fun createAccount(
        userId: String,
        name: String,
        type: AccountType,
        initialBalance: Double,
        color: String = "#6366f1",
        currency: String = "NPR",
        notes: String? = null
    ): Account {
        val account = Account(
            userId = userId,
            name = name.trim(),
            type = type.name,
            balance = initialBalance,
            color = color,
            currency = currency,
            notes = notes
        )
        accountDao.insert(account)
        return account
    }

    suspend fun updateAccount(account: Account) {
        accountDao.update(account)
    }

    suspend fun deleteAccount(account: Account): Result<Unit> {
        val count = transactionDao.countByAccountId(account.id)
        if (count > 0) {
            return Result.failure(Exception("Cannot delete account with existing transactions ($count transactions linked)."))
        }
        accountDao.delete(account)
        return Result.success(Unit)
    }

    // --- TRANSACTIONS (ATOMIC WITH BALANCE UPDATE & NEGATIVE BALANCE PREVENTION) ---
    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>> = transactionDao.getAllFlow(userId)
    fun getRecentTransactionsFlow(userId: String, limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentFlow(userId, limit)

    private fun checkSufficientBalance(account: Account, amount: Double, allowNegative: Boolean) {
        if (!allowNegative && (account.balance - amount < -0.0001)) {
            val formattedBal = String.format(Locale.US, "%.2f", account.balance)
            val formattedReq = String.format(Locale.US, "%.2f", amount)
            throw InsufficientBalanceException(
                "Insufficient funds in ${account.name}: balance is $formattedBal, transaction requires $formattedReq"
            )
        }
    }

    suspend fun createTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        date: Long,
        dateBs: String,
        name: String? = null,
        notes: String? = null,
        accountFromId: String? = null,
        accountToId: String? = null,
        categoryId: String? = null,
        loanId: String? = null
    ): Result<TransactionEntity> = runCatching {
        val allowNegative = runCatching { preferenceManager.allowNegativeBalanceFlow.first() }.getOrDefault(false)

        database.withTransaction {
            when (type) {
                TransactionType.INCOME -> {
                    requireNotNull(accountToId) { "Account to credit is required for income" }
                    val toAccount = requireNotNull(accountDao.getById(accountToId)) { "Account not found" }
                    accountDao.updateBalance(toAccount.id, toAccount.balance + amount)
                }
                TransactionType.EXPENSE -> {
                    requireNotNull(accountFromId) { "Account to debit is required for expense" }
                    val fromAccount = requireNotNull(accountDao.getById(accountFromId)) { "Account not found" }
                    checkSufficientBalance(fromAccount, amount, allowNegative)
                    accountDao.updateBalance(fromAccount.id, fromAccount.balance - amount)
                }
                TransactionType.TRANSFER -> {
                    requireNotNull(accountFromId) { "Source account is required for transfer" }
                    requireNotNull(accountToId) { "Destination account is required for transfer" }
                    require(accountFromId != accountToId) { "Cannot transfer to the same account" }
                    val fromAccount = requireNotNull(accountDao.getById(accountFromId)) { "Source account not found" }
                    val toAccount = requireNotNull(accountDao.getById(accountToId)) { "Destination account not found" }
                    checkSufficientBalance(fromAccount, amount, allowNegative)
                    accountDao.updateBalance(fromAccount.id, fromAccount.balance - amount)
                    accountDao.updateBalance(toAccount.id, toAccount.balance + amount)
                }
                TransactionType.LEND -> {
                    requireNotNull(accountFromId) { "Account to lend from is required" }
                    val fromAccount = requireNotNull(accountDao.getById(accountFromId)) { "Account not found" }
                    checkSufficientBalance(fromAccount, amount, allowNegative)
                    accountDao.updateBalance(fromAccount.id, fromAccount.balance - amount)
                }
                TransactionType.BORROW -> {
                    requireNotNull(accountToId) { "Account to receive borrowed funds is required" }
                    val toAccount = requireNotNull(accountDao.getById(accountToId)) { "Account not found" }
                    accountDao.updateBalance(toAccount.id, toAccount.balance + amount)
                }
            }

            val transaction = TransactionEntity(
                userId = userId,
                type = type.name,
                name = name,
                notes = notes,
                amount = amount,
                date = date,
                dateBs = dateBs,
                accountFromId = accountFromId,
                accountToId = accountToId,
                categoryId = categoryId,
                loanId = loanId
            )
            transactionDao.insert(transaction)
            transaction
        }
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
        newCategoryId: String?
    ): Result<TransactionEntity> = runCatching {
        val allowNegative = runCatching { preferenceManager.allowNegativeBalanceFlow.first() }.getOrDefault(false)

        database.withTransaction {
            // Step 1: Reverse old change
            when (TransactionType.valueOf(oldTransaction.type)) {
                TransactionType.INCOME -> {
                    oldTransaction.accountToId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance - oldTransaction.amount)
                    }
                }
                TransactionType.EXPENSE -> {
                    oldTransaction.accountFromId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance + oldTransaction.amount)
                    }
                }
                TransactionType.TRANSFER -> {
                    oldTransaction.accountFromId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance + oldTransaction.amount)
                    }
                    oldTransaction.accountToId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance - oldTransaction.amount)
                    }
                }
                TransactionType.LEND -> {
                    oldTransaction.accountFromId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance + oldTransaction.amount)
                    }
                }
                TransactionType.BORROW -> {
                    oldTransaction.accountToId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance - oldTransaction.amount)
                    }
                }
            }

            // Step 2: Apply new change with balance validation
            when (newType) {
                TransactionType.INCOME -> {
                    requireNotNull(newAccountToId) { "Account to credit is required" }
                    val toAcc = requireNotNull(accountDao.getById(newAccountToId))
                    accountDao.updateBalance(toAcc.id, toAcc.balance + newAmount)
                }
                TransactionType.EXPENSE -> {
                    requireNotNull(newAccountFromId) { "Account to debit is required" }
                    val fromAcc = requireNotNull(accountDao.getById(newAccountFromId))
                    checkSufficientBalance(fromAcc, newAmount, allowNegative)
                    accountDao.updateBalance(fromAcc.id, fromAcc.balance - newAmount)
                }
                TransactionType.TRANSFER -> {
                    requireNotNull(newAccountFromId) { "Source account is required" }
                    requireNotNull(newAccountToId) { "Destination account is required" }
                    val fromAcc = requireNotNull(accountDao.getById(newAccountFromId))
                    val toAcc = requireNotNull(accountDao.getById(newAccountToId))
                    checkSufficientBalance(fromAcc, newAmount, allowNegative)
                    accountDao.updateBalance(fromAcc.id, fromAcc.balance - newAmount)
                    accountDao.updateBalance(toAcc.id, toAcc.balance + newAmount)
                }
                TransactionType.LEND -> {
                    requireNotNull(newAccountFromId) { "Account to debit is required" }
                    val fromAcc = requireNotNull(accountDao.getById(newAccountFromId))
                    checkSufficientBalance(fromAcc, newAmount, allowNegative)
                    accountDao.updateBalance(fromAcc.id, fromAcc.balance - newAmount)
                }
                TransactionType.BORROW -> {
                    requireNotNull(newAccountToId) { "Account to credit is required" }
                    val toAcc = requireNotNull(accountDao.getById(newAccountToId))
                    accountDao.updateBalance(toAcc.id, toAcc.balance + newAmount)
                }
            }

            val updated = oldTransaction.copy(
                type = newType.name,
                name = newName,
                notes = newNotes,
                amount = newAmount,
                date = newDate,
                dateBs = newDateBs,
                accountFromId = newAccountFromId,
                accountToId = newAccountToId,
                categoryId = newCategoryId,
                updatedAt = System.currentTimeMillis()
            )
            transactionDao.update(updated)
            updated
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity): Result<Unit> = runCatching {
        val allowNegative = preferenceManager.allowNegativeBalanceFlow.first()

        database.withTransaction {
            // Reversing the change
            when (TransactionType.valueOf(transaction.type)) {
                TransactionType.INCOME -> {
                    transaction.accountToId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        checkSufficientBalance(acc, transaction.amount, allowNegative)
                        accountDao.updateBalance(acc.id, acc.balance - transaction.amount)
                    }
                }
                TransactionType.EXPENSE -> {
                    transaction.accountFromId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance + transaction.amount)
                    }
                }
                TransactionType.TRANSFER -> {
                    transaction.accountToId?.let { id ->
                        val toAcc = requireNotNull(accountDao.getById(id))
                        checkSufficientBalance(toAcc, transaction.amount, allowNegative)
                        accountDao.updateBalance(toAcc.id, toAcc.balance - transaction.amount)
                    }
                    transaction.accountFromId?.let { id ->
                        val fromAcc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(fromAcc.id, fromAcc.balance + transaction.amount)
                    }
                }
                TransactionType.LEND -> {
                    transaction.accountFromId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        accountDao.updateBalance(acc.id, acc.balance + transaction.amount)
                    }
                }
                TransactionType.BORROW -> {
                    transaction.accountToId?.let { id ->
                        val acc = requireNotNull(accountDao.getById(id))
                        checkSufficientBalance(acc, transaction.amount, allowNegative)
                        accountDao.updateBalance(acc.id, acc.balance - transaction.amount)
                    }
                }
            }
            transactionDao.delete(transaction)
        }
    }

    // --- LOANS & INTERESTS ---
    fun getLoansFlow(userId: String): Flow<List<Loan>> = loanDao.getAllFlow(userId)
    fun getLoansByStatusFlow(userId: String, status: LoanStatus): Flow<List<Loan>> =
        loanDao.getByStatusFlow(userId, status.name)
    fun getLoanInterestsFlow(loanId: String): Flow<List<LoanInterest>> =
        loanInterestDao.getByLoanIdFlow(loanId)

    suspend fun createLoan(
        userId: String,
        counterparty: String,
        type: LoanType,
        principal: Double,
        interestRate: Double = 0.0,
        startDate: Long = System.currentTimeMillis(),
        accountId: String? = null,
        interestMode: InterestMode = InterestMode.MANUAL,
        interestFrequency: InterestFrequency? = InterestFrequency.MONTHLY,
        notes: String? = null
    ): Result<Loan> = runCatching {
        val allowNegative = preferenceManager.allowNegativeBalanceFlow.first()

        database.withTransaction {
            val loanId = UUID.randomUUID().toString()
            val bsDate = NepaliDateConverter.adToBs(startDate).formatted

            // Calculate nextInterestDate if AUTO
            val nextInterestDate = if (interestMode == InterestMode.AUTO) {
                calculateNextInterestDate(startDate, interestFrequency ?: InterestFrequency.MONTHLY)
            } else null

            // If accountId is provided, auto-create LEND/BORROW transaction and update balance
            if (!accountId.isNullOrEmpty()) {
                val account = requireNotNull(accountDao.getById(accountId)) { "Account not found" }
                if (type == LoanType.LEND) {
                    checkSufficientBalance(account, principal, allowNegative)
                    accountDao.updateBalance(account.id, account.balance - principal)
                    val txn = TransactionEntity(
                        userId = userId,
                        type = TransactionType.LEND.name,
                        name = "Loan to $counterparty",
                        notes = "Principal disbursed: $principal",
                        amount = principal,
                        date = startDate,
                        dateBs = bsDate,
                        accountFromId = accountId,
                        loanId = loanId
                    )
                    transactionDao.insert(txn)
                } else {
                    // BORROW: funds received into account
                    accountDao.updateBalance(account.id, account.balance + principal)
                    val txn = TransactionEntity(
                        userId = userId,
                        type = TransactionType.BORROW.name,
                        name = "Loan from $counterparty",
                        notes = "Principal received: $principal",
                        amount = principal,
                        date = startDate,
                        dateBs = bsDate,
                        accountToId = accountId,
                        loanId = loanId
                    )
                    transactionDao.insert(txn)
                }
            }

            val loan = Loan(
                id = loanId,
                userId = userId,
                counterparty = counterparty.trim(),
                type = type.name,
                principal = principal,
                interestRate = interestRate,
                startDate = startDate,
                accountId = accountId,
                interestMode = interestMode.name,
                nextInterestDate = nextInterestDate,
                interestFrequency = interestFrequency?.name,
                status = LoanStatus.ACTIVE.name,
                notes = notes
            )
            loanDao.insert(loan)
            loan
        }
    }

    suspend fun addLoanInterest(
        loanId: String,
        amount: Double,
        date: Long = System.currentTimeMillis()
    ): LoanInterest {
        val bsDate = NepaliDateConverter.adToBs(date).formatted
        val interest = LoanInterest(
            loanId = loanId,
            amount = amount,
            date = date,
            dateBs = bsDate
        )
        loanInterestDao.insert(interest)
        return interest
    }

    suspend fun markLoanRepaid(
        loan: Loan,
        accountId: String?,
        repayDate: Long = System.currentTimeMillis()
    ): Result<Unit> = runCatching {
        val allowNegative = preferenceManager.allowNegativeBalanceFlow.first()

        database.withTransaction {
            val interests = loanInterestDao.getByLoanId(loan.id)
            val totalInterest = interests.sumOf { it.amount }
            val totalRepayAmount = loan.principal + totalInterest
            val bsDate = NepaliDateConverter.adToBs(repayDate).formatted

            if (!accountId.isNullOrEmpty()) {
                val account = requireNotNull(accountDao.getById(accountId)) { "Account not found" }
                if (loan.type == LoanType.LEND.name) {
                    // Repayment received -> INCOME transaction
                    accountDao.updateBalance(account.id, account.balance + totalRepayAmount)
                    val txn = TransactionEntity(
                        userId = loan.userId,
                        type = TransactionType.INCOME.name,
                        name = "Repayment from ${loan.counterparty}",
                        notes = "Loan repaid (Principal: ${loan.principal}, Interest: $totalInterest)",
                        amount = totalRepayAmount,
                        date = repayDate,
                        dateBs = bsDate,
                        accountToId = accountId,
                        loanId = loan.id
                    )
                    transactionDao.insert(txn)
                } else {
                    // Borrow repaid -> EXPENSE transaction
                    checkSufficientBalance(account, totalRepayAmount, allowNegative)
                    accountDao.updateBalance(account.id, account.balance - totalRepayAmount)
                    val txn = TransactionEntity(
                        userId = loan.userId,
                        type = TransactionType.EXPENSE.name,
                        name = "Repayment to ${loan.counterparty}",
                        notes = "Borrow repaid (Principal: ${loan.principal}, Interest: $totalInterest)",
                        amount = totalRepayAmount,
                        date = repayDate,
                        dateBs = bsDate,
                        accountFromId = accountId,
                        loanId = loan.id
                    )
                    transactionDao.insert(txn)
                }
            }

            loanDao.update(
                loan.copy(
                    status = LoanStatus.REPAID.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteLoan(loan: Loan) {
        loanDao.delete(loan)
    }

    suspend fun checkAndGenerateAutoInterests(userId: String) {
        val now = System.currentTimeMillis()
        val dueLoans = loanDao.getDueAutoLoans(userId, now)
        for (loan in dueLoans) {
            val freq = loan.interestFrequency?.let { InterestFrequency.valueOf(it) } ?: InterestFrequency.MONTHLY
            val factor = when (freq) {
                InterestFrequency.MONTHLY -> 1.0 / 12.0
                InterestFrequency.QUARTERLY -> 3.0 / 12.0
                InterestFrequency.ANNUALLY -> 1.0
            }
            val interestAmount = (loan.principal * loan.interestRate / 100.0) * factor
            if (interestAmount > 0) {
                addLoanInterest(loan.id, interestAmount, loan.nextInterestDate ?: now)
            }
            val nextDate = calculateNextInterestDate(loan.nextInterestDate ?: now, freq)
            loanDao.update(loan.copy(nextInterestDate = nextDate, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun calculateNextInterestDate(baseDate: Long, frequency: InterestFrequency): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseDate
        }
        when (frequency) {
            InterestFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            InterestFrequency.QUARTERLY -> cal.add(Calendar.MONTH, 3)
            InterestFrequency.ANNUALLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    // --- BUDGETS ---
    fun getBudgetsFlow(userId: String): Flow<List<Budget>> = budgetDao.getAllFlow(userId)

    suspend fun setBudget(userId: String, categoryId: String, monthlyLimit: Double, rollover: Boolean): Budget {
        val existing = budgetDao.getByCategory(userId, categoryId)
        val budget = if (existing != null) {
            existing.copy(monthlyLimit = monthlyLimit, rollover = rollover, updatedAt = System.currentTimeMillis())
        } else {
            Budget(userId = userId, categoryId = categoryId, monthlyLimit = monthlyLimit, rollover = rollover)
        }
        budgetDao.insert(budget)
        return budget
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.delete(budget)
    }

    // --- WISHLIST & AFFORDABILITY ---
    fun getWishlistFlow(userId: String): Flow<List<WishlistItem>> = wishlistDao.getAllFlow(userId)
    fun getWishlistByStatusFlow(userId: String, status: WishlistStatus): Flow<List<WishlistItem>> =
        wishlistDao.getByStatusFlow(userId, status.name)

    suspend fun createWishlistItem(
        userId: String,
        name: String,
        estimatedCost: Double,
        preferredDate: Long? = null,
        priority: String,
        category: String? = null,
        notes: String? = null
    ): WishlistItem {
        val item = WishlistItem(
            userId = userId,
            name = name.trim(),
            estimatedCost = estimatedCost,
            preferredDate = preferredDate,
            priority = priority,
            status = WishlistStatus.PLANNED.name,
            category = category,
            notes = notes
        )
        wishlistDao.insert(item)
        return item
    }

    suspend fun updateWishlistItem(item: WishlistItem) {
        wishlistDao.update(item)
    }

    suspend fun deleteWishlistItem(item: WishlistItem) {
        wishlistDao.delete(item)
    }

    suspend fun purchaseWishlistItem(
        item: WishlistItem,
        createTransaction: Boolean,
        accountId: String?,
        categoryId: String?
    ): Result<Unit> = runCatching {
        database.withTransaction {
            if (createTransaction && !accountId.isNullOrEmpty()) {
                val now = System.currentTimeMillis()
                val bsDate = NepaliDateConverter.adToBs(now).formatted
                val txnResult = createTransaction(
                    userId = item.userId,
                    type = TransactionType.EXPENSE,
                    amount = item.estimatedCost,
                    date = now,
                    dateBs = bsDate,
                    name = "Wishlist: ${item.name}",
                    notes = item.notes,
                    accountFromId = accountId,
                    categoryId = categoryId
                )
                if (txnResult.isFailure) {
                    throw txnResult.exceptionOrNull() ?: Exception("Failed to create expense transaction")
                }
            }
            wishlistDao.update(item.copy(status = WishlistStatus.PURCHASED.name, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun calculateAffordability(item: WishlistItem): WishlistItemWithAffordability {
        val accounts = accountDao.getAll(item.userId)
        // Liquid cash is non-investment accounts
        val totalCash = accounts
            .filter { it.type != AccountType.INVESTMENT.name }
            .sumOf { it.balance }

        if (totalCash >= item.estimatedCost) {
            return WishlistItemWithAffordability(
                item = item,
                affordability = Affordability.CAN_AFFORD_NOW,
                savingsNeeded = 0.0
            )
        }

        // Check projected balance (3-month avg surplus)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -3)
        }
        val threeMonthsAgo = cal.timeInMillis
        val recentTxns = transactionDao.getByDateRange(item.userId, threeMonthsAgo, now)
        val income = recentTxns.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val expense = recentTxns.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
        val avgMonthlySurplus = (income - expense) / 3.0

        if (item.preferredDate != null && item.preferredDate > now && avgMonthlySurplus > 0) {
            val monthsToTarget = ((item.preferredDate - now) / (1000L * 60 * 60 * 24 * 30.4)).coerceAtLeast(0.5)
            val projectedCash = totalCash + (avgMonthlySurplus * monthsToTarget)
            if (projectedCash >= item.estimatedCost) {
                return WishlistItemWithAffordability(
                    item = item,
                    affordability = Affordability.CAN_AFFORD_BY_DATE,
                    savingsNeeded = item.estimatedCost - totalCash
                )
            }
        }

        return WishlistItemWithAffordability(
            item = item,
            affordability = Affordability.CANNOT_AFFORD,
            savingsNeeded = (item.estimatedCost - totalCash).coerceAtLeast(0.0)
        )
    }

    // --- JOURNAL ---
    fun getJournalEntriesFlow(userId: String): Flow<List<JournalEntry>> = journalDao.getAllFlow(userId)

    suspend fun createJournalEntry(
        userId: String,
        content: String,
        mood: String?,
        date: Long = System.currentTimeMillis()
    ): JournalEntry {
        val bsDate = NepaliDateConverter.adToBs(date).formatted
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val timeStr = timeFormat.format(Date(date))

        val entry = JournalEntry(
            userId = userId,
            content = content,
            date = date,
            dateBs = bsDate,
            time = timeStr,
            mood = mood
        )
        journalDao.insert(entry)
        return entry
    }

    suspend fun updateJournalEntry(entry: JournalEntry) {
        journalDao.update(entry.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteJournalEntry(entry: JournalEntry) {
        journalDao.delete(entry)
    }

    // --- DASHBOARD AGGREGATIONS ---
    suspend fun getDashboardData(userId: String, year: Int, monthIndex: Int): DashboardData {
        // monthIndex is 1-indexed (1=Jan, 12=Dec)
        val accounts = accountDao.getAll(userId)
        val loans = loanDao.getAll(userId)
        val categories = categoryDao.getAll(userId).associateBy { it.id }
        val budgets = budgetDao.getAll(userId)
        val allTxns = transactionDao.getAll(userId)

        val netWorth = accounts.sumOf { it.balance }
        val totalBank = accounts.filter { it.type.equals(AccountType.BANK.name, ignoreCase = true) }.sumOf { it.balance }
        val totalCash = accounts.filter { it.type.equals(AccountType.CASH.name, ignoreCase = true) }.sumOf { it.balance }
        val totalWallet = accounts.filter { it.type.equals(AccountType.WALLET.name, ignoreCase = true) }.sumOf { it.balance }
        val totalInvestments = accounts.filter { it.type.equals(AccountType.INVESTMENT.name, ignoreCase = true) }.sumOf { it.balance }

        val activeLoans = loans.filter { it.status.equals(LoanStatus.ACTIVE.name, ignoreCase = true) }
        val totalLent = activeLoans.filter { it.type.equals(LoanType.LEND.name, ignoreCase = true) }.sumOf { it.principal }
        val totalBorrowed = activeLoans.filter { it.type.equals(LoanType.BORROW.name, ignoreCase = true) }.sumOf { it.principal }
        val netReceivable = totalLent - totalBorrowed

        // Transactions strictly for currently selected (year, monthIndex)
        val monthTransactions = allTxns.filter { txn ->
            val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = txn.date }
            c.get(Calendar.YEAR) == year && (c.get(Calendar.MONTH) + 1) == monthIndex
        }

        val monthlyIncome = monthTransactions.filter { it.type.equals(TransactionType.INCOME.name, ignoreCase = true) }.sumOf { it.amount }
        val monthlyExpenses = monthTransactions.filter { it.type.equals(TransactionType.EXPENSE.name, ignoreCase = true) }.sumOf { it.amount }
        val monthlySaved = monthlyIncome - monthlyExpenses
        val savingsRate = if (monthlyIncome > 0) ((monthlySaved / monthlyIncome) * 100.0).coerceIn(-100.0, 100.0) else 0.0

        // Expenses by Category (strictly filtered to currently selected month)
        val expenseTxns = monthTransactions.filter { it.type.equals(TransactionType.EXPENSE.name, ignoreCase = true) }
        val totalExpenseAmount = expenseTxns.sumOf { it.amount }
        val palette = listOf("#10b981", "#ef4444", "#f59e0b", "#3b82f6", "#8b5cf6", "#ec4899", "#14b8a6", "#6366f1")
        val expenseByCategory = expenseTxns
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val catName = categories[catId]?.name ?: if (!catId.isNullOrBlank() && catId.startsWith("expense-", ignoreCase = true)) {
                    catId.substringAfterLast("-").replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                } else "Uncategorized"
                val sum = txns.sumOf { it.amount }
                val pct = if (totalExpenseAmount > 0) (sum / totalExpenseAmount).toFloat() else 0f
                CategoryExpenseShare(
                    categoryName = catName,
                    amount = sum,
                    percentage = pct,
                    color = "#10b981"
                )
            }
            .sortedByDescending { it.amount }
            .mapIndexed { idx, item ->
                item.copy(color = palette[idx % palette.size])
            }

        // 6-Month Income / Expense Trend
        // Anchored to the selected period (year, monthIndex)
        val trendPoints = mutableListOf<MonthlyTrendPoint>()
        for (i in 5 downTo 0) {
            val curCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthIndex - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -i)
            }
            val y = curCal.get(Calendar.YEAR)
            val m = curCal.get(Calendar.MONTH) + 1

            val txns = allTxns.filter { txn ->
                val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = txn.date }
                c.get(Calendar.YEAR) == y && (c.get(Calendar.MONTH) + 1) == m
            }
            val inc = txns.filter { it.type.equals(TransactionType.INCOME.name, ignoreCase = true) }.sumOf { it.amount }
            val exp = txns.filter { it.type.equals(TransactionType.EXPENSE.name, ignoreCase = true) }.sumOf { it.amount }
            val label = SimpleDateFormat("MMM", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(curCal.time)
            trendPoints.add(
                MonthlyTrendPoint(
                    monthLabel = label,
                    monthIndex = m,
                    year = y,
                    income = inc,
                    expense = exp
                )
            )
        }

        // Budgets with Spent and Rollover
        val budgetsWithProgress = budgets.map { b ->
            val catName = categories[b.categoryId]?.name ?: "Category"
            val spent = monthTransactions.filter {
                it.categoryId == b.categoryId && it.type.equals(TransactionType.EXPENSE.name, ignoreCase = true)
            }.sumOf { it.amount }
            val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
            BudgetProgress(
                budget = b,
                categoryName = catName,
                spent = spent,
                limit = b.monthlyLimit,
                percentage = pct,
                isOverBudget = spent > b.monthlyLimit
            )
        }

        val recent = allTxns.take(10)

        return DashboardData(
            netWorth = netWorth,
            totalCash = totalCash,
            totalBank = totalBank,
            totalWallet = totalWallet,
            totalInvestments = totalInvestments,
            activeLoansCount = activeLoans.size,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            netReceivable = netReceivable,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            monthlySaved = monthlySaved,
            savingsRate = savingsRate,
            expenseByCategory = expenseByCategory,
            incomeExpenseTrend = trendPoints,
            recentTransactions = recent,
            budgetsWithProgress = budgetsWithProgress
        )
    }

    // --- RECALCULATE BALANCES ---
    suspend fun recalculateBalances(userId: String): Result<Unit> = runCatching {
        database.withTransaction {
            val accounts = accountDao.getAll(userId)
            val transactions = transactionDao.getAll(userId).sortedBy { it.date }
            val balanceMap = mutableMapOf<String, Double>()
            accounts.forEach { balanceMap[it.id] = 0.0 }

            for (txn in transactions) {
                val txnType = try {
                    TransactionType.valueOf(txn.type.trim().uppercase())
                } catch (_: Exception) {
                    if (txn.type.contains("INC", ignoreCase = true)) TransactionType.INCOME
                    else if (txn.type.contains("TRANS", ignoreCase = true)) TransactionType.TRANSFER
                    else if (txn.type.contains("LEND", ignoreCase = true)) TransactionType.LEND
                    else if (txn.type.contains("BORR", ignoreCase = true)) TransactionType.BORROW
                    else TransactionType.EXPENSE
                }
                when (txnType) {
                    TransactionType.INCOME -> {
                        txn.accountToId?.let { id ->
                            balanceMap[id] = (balanceMap[id] ?: 0.0) + txn.amount
                        }
                    }
                    TransactionType.EXPENSE -> {
                        txn.accountFromId?.let { id ->
                            balanceMap[id] = (balanceMap[id] ?: 0.0) - txn.amount
                        }
                    }
                    TransactionType.TRANSFER -> {
                        txn.accountFromId?.let { id ->
                            balanceMap[id] = (balanceMap[id] ?: 0.0) - txn.amount
                        }
                        txn.accountToId?.let { id ->
                            balanceMap[id] = (balanceMap[id] ?: 0.0) + txn.amount
                        }
                    }
                    TransactionType.LEND -> {
                        txn.accountFromId?.let { id ->
                            balanceMap[id] = (balanceMap[id] ?: 0.0) - txn.amount
                        }
                    }
                    TransactionType.BORROW -> {
                        txn.accountToId?.let { id ->
                            balanceMap[id] = (balanceMap[id] ?: 0.0) + txn.amount
                        }
                    }
                }
            }

            for ((id, bal) in balanceMap) {
                accountDao.updateBalance(id, bal)
            }
        }
    }

    // --- EXPORT & IMPORT (JSON & CSV) ---
    suspend fun exportFullJson(userId: String): String {
        val accounts = accountDao.getAll(userId)
        val categories = categoryDao.getAll(userId)
        val transactions = transactionDao.getAll(userId)
        val loans = loanDao.getAll(userId)
        val loanInterests = loanInterestDao.getAll()
        val budgets = budgetDao.getAll(userId)
        val wishlist = wishlistDao.getAll(userId)
        val journal = journalDao.getAll(userId)

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("userId", userId)

        val accountsArr = JSONArray()
        accounts.forEach { a ->
            accountsArr.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("type", a.type)
                put("balance", a.balance)
                put("color", a.color)
                put("currency", a.currency)
                put("notes", a.notes ?: "")
            })
        }
        root.put("accounts", accountsArr)

        val categoriesArr = JSONArray()
        categories.forEach { c ->
            categoriesArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type)
                put("icon", c.icon ?: "")
                put("isDefault", c.isDefault)
            })
        }
        root.put("categories", categoriesArr)

        val transactionsArr = JSONArray()
        transactions.forEach { t ->
            transactionsArr.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("name", t.name ?: "")
                put("notes", t.notes ?: "")
                put("amount", t.amount)
                put("date", t.date)
                put("dateBs", t.dateBs)
                put("accountFromId", t.accountFromId ?: "")
                put("accountToId", t.accountToId ?: "")
                put("categoryId", t.categoryId ?: "")
                put("loanId", t.loanId ?: "")
            })
        }
        root.put("transactions", transactionsArr)

        val loansArr = JSONArray()
        loans.forEach { l ->
            loansArr.put(JSONObject().apply {
                put("id", l.id)
                put("counterparty", l.counterparty)
                put("type", l.type)
                put("principal", l.principal)
                put("interestRate", l.interestRate)
                put("startDate", l.startDate)
                put("accountId", l.accountId ?: "")
                put("interestMode", l.interestMode)
                put("status", l.status)
                put("notes", l.notes ?: "")
            })
        }
        root.put("loans", loansArr)

        val loanInterestsArr = JSONArray()
        loanInterests.forEach { li ->
            loanInterestsArr.put(JSONObject().apply {
                put("id", li.id)
                put("loanId", li.loanId)
                put("amount", li.amount)
                put("date", li.date)
                put("dateBs", li.dateBs)
            })
        }
        root.put("loanInterests", loanInterestsArr)

        val wishlistArr = JSONArray()
        wishlist.forEach { w ->
            wishlistArr.put(JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("estimatedCost", w.estimatedCost)
                put("preferredDate", w.preferredDate ?: JSONObject.NULL)
                put("priority", w.priority)
                put("status", w.status)
                put("category", w.category ?: "")
                put("notes", w.notes ?: "")
            })
        }
        root.put("wishlist", wishlistArr)
        root.put("wishlistItems", wishlistArr)

        val journalArr = JSONArray()
        journal.forEach { j ->
            journalArr.put(JSONObject().apply {
                put("id", j.id)
                put("content", j.content)
                put("date", j.date)
                put("dateBs", j.dateBs)
                put("time", j.time)
                put("mood", j.mood ?: "")
            })
        }
        root.put("journal", journalArr)
        root.put("journalEntries", journalArr)

        val budgetsArr = JSONArray()
        budgets.forEach { b ->
            budgetsArr.put(JSONObject().apply {
                put("id", b.id)
                put("categoryId", b.categoryId)
                put("monthlyLimit", b.monthlyLimit)
                put("rollover", b.rollover)
            })
        }
        root.put("budgets", budgetsArr)

        return root.toString(2)
    }

    suspend fun importFullJson(userId: String, jsonStr: String): Result<Int> = runCatching {
        val root = JSONObject(jsonStr)
        var count = 0

        database.withTransaction {
            // 1. Users (if present, update active user's name/email)
            val usersArr = root.optJSONArray("users")
            if (usersArr != null && usersArr.length() > 0) {
                val userObj = usersArr.getJSONObject(0)
                val impName = userObj.optString("name").trim()
                val impEmail = userObj.optString("email").trim()
                if (impName.isNotBlank() || impEmail.isNotBlank()) {
                    val current = userDao.getById(userId)
                    if (current != null) {
                        userDao.update(
                            current.copy(
                                name = if (impName.isNotBlank()) impName else current.name,
                                email = if (impEmail.isNotBlank()) impEmail else current.email
                            )
                        )
                    }
                }
            }

            // 2. Accounts - preserve their authoritative balance
            val accountsArr = root.optJSONArray("accounts")
            if (accountsArr != null) {
                for (i in 0 until accountsArr.length()) {
                    val obj = accountsArr.getJSONObject(i)
                    val rawType = obj.optString("type", AccountType.BANK.name).trim().uppercase()
                    val accType = try {
                        AccountType.valueOf(rawType).name
                    } catch (_: Exception) {
                        if (rawType.contains("CASH")) AccountType.CASH.name
                        else if (rawType.contains("WALL")) AccountType.WALLET.name
                        else if (rawType.contains("INV")) AccountType.INVESTMENT.name
                        else AccountType.BANK.name
                    }
                    accountDao.insert(
                        Account(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = userId,
                            name = obj.optString("name", "Account"),
                            type = accType,
                            balance = obj.optDouble("balance", 0.0),
                            color = obj.optString("color", "#6366f1"),
                            currency = obj.optString("currency", "NPR"),
                            notes = obj.optString("notes").ifEmpty { null }
                        )
                    )
                    count++
                }
            }

            // 3. Categories
            val categoriesArr = root.optJSONArray("categories")
            val knownCategoryIds = mutableSetOf<String>()
            if (categoriesArr != null) {
                for (i in 0 until categoriesArr.length()) {
                    val obj = categoriesArr.getJSONObject(i)
                    val catId = obj.optString("id", UUID.randomUUID().toString())
                    val rawType = obj.optString("type", CategoryType.EXPENSE.name).trim()
                    val catType = if (rawType.equals("INCOME", ignoreCase = true)) CategoryType.INCOME.name else CategoryType.EXPENSE.name
                    val catName = obj.optString("name").ifBlank {
                        obj.optString("title").ifBlank {
                            obj.optString("categoryName", "Category")
                        }
                    }
                    categoryDao.insert(
                        Category(
                            id = catId,
                            userId = userId,
                            name = catName,
                            type = catType,
                            icon = obj.optString("icon").ifEmpty { null },
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                    knownCategoryIds.add(catId)
                    count++
                }
            }

            // 4. Transactions
            val transactionsArr = root.optJSONArray("transactions")
            if (transactionsArr != null) {
                for (i in 0 until transactionsArr.length()) {
                    val obj = transactionsArr.getJSONObject(i)
                    val rawDate = obj.opt("date")?.toString()?.trim() ?: ""
                    val rawDateBs = obj.optString("dateBs", "").trim()
                    val date = if (rawDate.isNotBlank()) {
                        parseFlexibleDate(rawDate)
                    } else if (rawDateBs.isNotBlank()) {
                        try {
                            val parts = rawDateBs.split("/", "-")
                            if (parts.size == 3) {
                                NepaliDateConverter.bsToAd(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()).time
                            } else System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                    } else {
                        System.currentTimeMillis()
                    }
                    val dateBs = if (rawDateBs.isNotBlank()) rawDateBs else NepaliDateConverter.adToBs(date).formatted

                    val rawType = obj.optString("type", TransactionType.EXPENSE.name).trim().uppercase()
                    val txnType = try {
                        TransactionType.valueOf(rawType).name
                    } catch (_: Exception) {
                        if (rawType.contains("INC")) TransactionType.INCOME.name
                        else if (rawType.contains("TRANS")) TransactionType.TRANSFER.name
                        else if (rawType.contains("LEND")) TransactionType.LEND.name
                        else if (rawType.contains("BORR")) TransactionType.BORROW.name
                        else TransactionType.EXPENSE.name
                    }

                    val catId = obj.optString("categoryId").ifEmpty { null }
                    // Synthesize category if not in categories table
                    if (catId != null && !knownCategoryIds.contains(catId) && categoryDao.getById(catId) == null) {
                        val slug = catId.substringAfterLast("-", catId).replace("_", " ").trim()
                        val synthName = slug.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                        val synthType = if (catId.startsWith("income", ignoreCase = true) || txnType == TransactionType.INCOME.name) {
                            CategoryType.INCOME.name
                        } else {
                            CategoryType.EXPENSE.name
                        }
                        val synthCat = Category(
                            id = catId,
                            userId = userId,
                            name = synthName.ifBlank { "General" },
                            type = synthType,
                            isDefault = false
                        )
                        categoryDao.insert(synthCat)
                        knownCategoryIds.add(catId)
                    }

                    transactionDao.insert(
                        TransactionEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = userId,
                            type = txnType,
                            name = obj.optString("name").ifEmpty { null },
                            notes = obj.optString("notes").ifEmpty { null },
                            amount = obj.optDouble("amount", 0.0),
                            date = date,
                            dateBs = dateBs,
                            accountFromId = obj.optString("accountFromId").ifEmpty { null },
                            accountToId = obj.optString("accountToId").ifEmpty { null },
                            categoryId = catId,
                            loanId = obj.optString("loanId").ifEmpty { null }
                        )
                    )
                    count++
                }
            }

            // 5. Loans
            val loansArr = root.optJSONArray("loans")
            if (loansArr != null) {
                for (i in 0 until loansArr.length()) {
                    val obj = loansArr.getJSONObject(i)
                    val rawStart = obj.opt("startDate")?.toString()?.trim() ?: ""
                    val startDate = if (rawStart.isNotBlank()) parseFlexibleDate(rawStart) else System.currentTimeMillis()
                    val rawNext = obj.opt("nextInterestDate")?.toString()?.trim() ?: ""
                    val nextDate = if (rawNext.isNotBlank() && rawNext != "null") parseFlexibleDate(rawNext) else null

                    val rawType = obj.optString("type", LoanType.LEND.name).trim().uppercase()
                    val loanType = if (rawType.contains("BORR")) LoanType.BORROW.name else LoanType.LEND.name

                    val rawMode = obj.optString("interestMode", InterestMode.MANUAL.name).trim().uppercase()
                    val interestMode = if (rawMode.contains("AUTO")) InterestMode.AUTO.name else InterestMode.MANUAL.name

                    val rawStatus = obj.optString("status", LoanStatus.ACTIVE.name).trim().uppercase()
                    val status = try {
                        LoanStatus.valueOf(rawStatus).name
                    } catch (_: Exception) {
                        LoanStatus.ACTIVE.name
                    }

                    loanDao.insert(
                        Loan(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = userId,
                            counterparty = obj.optString("counterparty").ifBlank { obj.optString("name", "Unknown") },
                            type = loanType,
                            principal = obj.optDouble("principal", obj.optDouble("amount", 0.0)),
                            interestRate = obj.optDouble("interestRate", obj.optDouble("rate", 0.0)),
                            startDate = startDate,
                            accountId = obj.optString("accountId").ifEmpty { null },
                            interestMode = interestMode,
                            nextInterestDate = nextDate,
                            interestFrequency = obj.optString("interestFrequency", InterestFrequency.MONTHLY.name).uppercase(),
                            status = status,
                            notes = obj.optString("notes").ifEmpty { null }
                        )
                    )
                    count++
                }
            }

            // 6. Loan Interests
            val loanInterestsArr = root.optJSONArray("loanInterests")
            if (loanInterestsArr != null) {
                for (i in 0 until loanInterestsArr.length()) {
                    val obj = loanInterestsArr.getJSONObject(i)
                    val rawDate = obj.opt("date")?.toString()?.trim() ?: ""
                    val date = if (rawDate.isNotBlank()) parseFlexibleDate(rawDate) else System.currentTimeMillis()
                    val rawDateBs = obj.optString("dateBs", "").trim()
                    val dateBs = if (rawDateBs.isNotBlank()) rawDateBs else NepaliDateConverter.adToBs(date).formatted
                    loanInterestDao.insert(
                        LoanInterest(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            loanId = obj.optString("loanId", ""),
                            amount = obj.optDouble("amount", 0.0),
                            date = date,
                            dateBs = dateBs
                        )
                    )
                    count++
                }
            }

            // 7. Budgets
            val budgetsArr = root.optJSONArray("budgets")
            if (budgetsArr != null) {
                for (i in 0 until budgetsArr.length()) {
                    val obj = budgetsArr.getJSONObject(i)
                    budgetDao.insert(
                        Budget(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = userId,
                            categoryId = obj.optString("categoryId", ""),
                            monthlyLimit = obj.optDouble("monthlyLimit", obj.optDouble("amount", obj.optDouble("limit", 0.0))),
                            rollover = obj.optBoolean("rollover", false)
                        )
                    )
                    count++
                }
            }

            // 8. Wishlist (supports both "wishlistItems" and "wishlist")
            val wishlistArr = root.optJSONArray("wishlistItems") ?: root.optJSONArray("wishlist")
            if (wishlistArr != null) {
                for (i in 0 until wishlistArr.length()) {
                    val obj = wishlistArr.getJSONObject(i)
                    val rawPrefDate = obj.opt("preferredDate")?.toString()?.trim() ?: ""
                    val prefDate = if (rawPrefDate.isNotBlank() && rawPrefDate != "null") {
                        parseFlexibleDate(rawPrefDate)
                    } else null

                    val rawPriority = obj.optString("priority", WishlistPriority.MEDIUM.name).trim().uppercase()
                    val priority = try {
                        WishlistPriority.valueOf(rawPriority).name
                    } catch (_: Exception) {
                        WishlistPriority.MEDIUM.name
                    }

                    val rawStatus = obj.optString("status", WishlistStatus.PLANNED.name).trim().uppercase()
                    val status = try {
                        WishlistStatus.valueOf(rawStatus).name
                    } catch (_: Exception) {
                        WishlistStatus.PLANNED.name
                    }

                    val cost = obj.optDouble("estimatedCost", obj.optDouble("cost", obj.optDouble("amount", obj.optDouble("price", 0.0))))
                    val name = obj.optString("name").ifBlank { obj.optString("title", "Wishlist Item") }

                    wishlistDao.insert(
                        WishlistItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = userId,
                            name = name,
                            estimatedCost = cost,
                            preferredDate = prefDate,
                            priority = priority,
                            status = status,
                            category = obj.optString("category").ifEmpty { null },
                            notes = obj.optString("notes").ifEmpty { null }
                        )
                    )
                    count++
                }
            }

            // 9. Journal (supports both "journalEntries" and "journal")
            val journalArr = root.optJSONArray("journalEntries") ?: root.optJSONArray("journal")
            if (journalArr != null) {
                for (i in 0 until journalArr.length()) {
                    val obj = journalArr.getJSONObject(i)
                    val rawDate = obj.opt("date")?.toString()?.trim() ?: ""
                    val rawDateBs = obj.optString("dateBs", "").trim()
                    val date = if (rawDate.isNotBlank()) {
                        parseFlexibleDate(rawDate)
                    } else if (rawDateBs.isNotBlank()) {
                        try {
                            val parts = rawDateBs.split("/", "-")
                            if (parts.size == 3) {
                                NepaliDateConverter.bsToAd(parts[0].toInt(), parts[1].toInt(), parts[2].toInt()).time
                            } else System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                    } else {
                        System.currentTimeMillis()
                    }
                    val dateBs = if (rawDateBs.isNotBlank()) rawDateBs else NepaliDateConverter.adToBs(date).formatted
                    val content = obj.optString("content").ifBlank {
                        obj.optString("body").ifBlank {
                            obj.optString("entry").ifBlank {
                                obj.optString("text", obj.optString("note", ""))
                            }
                        }
                    }
                    val rawMood = obj.optString("mood").trim().uppercase()
                    val mood = try {
                        if (rawMood.isNotEmpty()) Mood.valueOf(rawMood).name else Mood.NEUTRAL.name
                    } catch (_: Exception) {
                        Mood.NEUTRAL.name
                    }

                    journalDao.insert(
                        JournalEntry(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            userId = userId,
                            content = content,
                            date = date,
                            dateBs = dateBs,
                            time = obj.optString("time", "12:00"),
                            mood = mood
                        )
                    )
                    count++
                }
            }
        }
        count
    }

    suspend fun exportTransactionsCsv(userId: String): String {
        val txns = transactionDao.getAll(userId)
        val accounts = accountDao.getAll(userId).associateBy { it.id }
        val categories = categoryDao.getAll(userId).associateBy { it.id }

        val sb = StringBuilder()
        sb.append("ID,Type,Name,Amount,Date_AD,Date_BS,Account_From,Account_To,Category,Notes\n")
        val adFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        txns.forEach { t ->
            val fromAcc = t.accountFromId?.let { accounts[it]?.name } ?: ""
            val toAcc = t.accountToId?.let { accounts[it]?.name } ?: ""
            val cat = t.categoryId?.let { categories[it]?.name } ?: ""
            val name = (t.name ?: "").replace("\"", "\"\"")
            val notes = (t.notes ?: "").replace("\"", "\"\"")

            sb.append("\"${t.id}\",")
            sb.append("\"${t.type}\",")
            sb.append("\"$name\",")
            sb.append("${t.amount},")
            sb.append("\"${adFormat.format(Date(t.date))}\",")
            sb.append("\"${t.dateBs}\",")
            sb.append("\"$fromAcc\",")
            sb.append("\"$toAcc\",")
            sb.append("\"$cat\",")
            sb.append("\"$notes\"\n")
        }
        return sb.toString()
    }

    suspend fun importTransactionsCsv(userId: String, csvContent: String): Result<Int> = runCatching {
        val cleanCsv = csvContent.removePrefix("\uFEFF").trim()
        if (cleanCsv.isBlank()) return@runCatching 0

        val records = parseAllCsvRecords(cleanCsv)
        if (records.isEmpty()) return@runCatching 0

        val headerRow = records[0]
        val headerMap = mutableMapOf<String, Int>()
        headerRow.forEachIndexed { idx, h ->
            val norm = h.lowercase().trim().replace(Regex("[^a-z0-9]"), "")
            headerMap[norm] = idx
        }

        fun findCol(vararg candidates: String): Int? {
            for (c in candidates) {
                val norm = c.lowercase().replace(Regex("[^a-z0-9]"), "")
                val found = headerMap[norm] ?: headerMap.entries.firstOrNull { it.key.contains(norm) }?.value
                if (found != null) return found
            }
            return null
        }

        val hasHeader = findCol("type", "amount", "date", "name", "desc", "account") != null

        val typeCol = findCol("type", "txntype", "action") ?: 1
        val nameCol = findCol("name", "description", "desc", "title", "payee", "item", "narration", "particulars") ?: 2
        val amountCol = findCol("amount", "amt", "value", "cost", "total") ?: 3
        val dateAdCol = findCol("datead", "date", "createdat", "time", "datetime", "timestamp") ?: 4
        val dateBsCol = findCol("datebs", "bs", "nepalidate", "bsdate") ?: 5
        val fromCol = findCol("accountfrom", "fromaccount", "fromacc", "sourceaccount", "from") ?: 6
        val toCol = findCol("accountto", "toaccount", "toacc", "destinationaccount", "targetaccount", "to") ?: 7
        val genericAccountCol = findCol("account", "accountname", "acc", "bank", "wallet")
        val catCol = findCol("category", "categoryname", "cat") ?: 8
        val notesCol = findCol("notes", "note", "memo", "remark", "remarks", "comment") ?: 9

        // Load existing accounts and categories
        val existingAccounts = accountDao.getAll(userId).toMutableList()
        val accountByName = existingAccounts.associateBy { it.name.trim().lowercase() }.toMutableMap()
        val accountById = existingAccounts.associateBy { it.id }.toMutableMap()

        val existingCategories = categoryDao.getAll(userId).toMutableList()
        val categoryByName = existingCategories.associateBy { it.name.trim().lowercase() }.toMutableMap()
        val categoryById = existingCategories.associateBy { it.id }.toMutableMap()

        suspend fun getOrCreateAccount(rawNameOrId: String, defaultType: AccountType): Account {
            val clean = rawNameOrId.trim()
            if (clean.isBlank()) {
                return existingAccounts.firstOrNull() ?: run {
                    val created = createAccount(userId, "Default Account", AccountType.CASH, 0.0)
                    existingAccounts.add(created)
                    accountByName[created.name.lowercase()] = created
                    accountById[created.id] = created
                    created
                }
            }
            accountById[clean]?.let { return it }
            accountByName[clean.lowercase()]?.let { return it }

            // Infer type
            val inferredType = when {
                clean.contains("bank", ignoreCase = true) -> AccountType.BANK
                clean.contains("wallet", ignoreCase = true) || clean.contains("esewa", ignoreCase = true) || clean.contains("khalti", ignoreCase = true) -> AccountType.WALLET
                clean.contains("invest", ignoreCase = true) || clean.contains("share", ignoreCase = true) -> AccountType.INVESTMENT
                else -> defaultType
            }
            val created = createAccount(userId, clean, inferredType, 0.0)
            existingAccounts.add(created)
            accountByName[created.name.lowercase()] = created
            accountById[created.id] = created
            return created
        }

        suspend fun getOrCreateCategory(rawNameOrId: String, typeStr: String): Category? {
            val clean = rawNameOrId.trim()
            if (clean.isBlank()) return null
            categoryById[clean]?.let { return it }
            categoryByName[clean.lowercase()]?.let { return it }

            // Auto create category
            val isExpense = typeStr.contains("exp", ignoreCase = true)
            val newCat = Category(
                id = UUID.randomUUID().toString(),
                userId = userId,
                name = clean,
                type = if (isExpense) CategoryType.EXPENSE.name else CategoryType.INCOME.name,
                icon = if (isExpense) "receipt" else "wallet",
                isDefault = false
            )
            categoryDao.insert(newCat)
            existingCategories.add(newCat)
            categoryByName[clean.lowercase()] = newCat
            categoryById[newCat.id] = newCat
            return newCat
        }

        val txnsToInsert = mutableListOf<TransactionEntity>()
        val startRow = if (hasHeader) 1 else 0

        for (i in startRow until records.size) {
            val cols = records[i]
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            val rawType = cols.getOrNull(typeCol)?.trim() ?: ""
            val rawAmount = cols.getOrNull(amountCol)?.trim() ?: "0"
            val rawName = cols.getOrNull(nameCol)?.trim()?.ifEmpty { null }
            val rawDateAd = cols.getOrNull(dateAdCol)?.trim() ?: ""
            val rawDateBs = cols.getOrNull(dateBsCol)?.trim() ?: ""
            val rawFrom = cols.getOrNull(fromCol)?.trim() ?: cols.getOrNull(genericAccountCol ?: -1)?.trim() ?: ""
            val rawTo = cols.getOrNull(toCol)?.trim() ?: cols.getOrNull(genericAccountCol ?: -1)?.trim() ?: ""
            val rawCat = cols.getOrNull(catCol)?.trim() ?: ""
            val rawNotes = cols.getOrNull(notesCol)?.trim()?.ifEmpty { null }

            // Parse amount
            var isNegative = false
            var cleanAmt = rawAmount
                .replace("NPR", "", ignoreCase = true)
                .replace("Rs.", "", ignoreCase = true)
                .replace("Rs", "", ignoreCase = true)
                .replace("रु", "")
                .replace("$", "")
                .replace("€", "")
                .replace("£", "")
                .replace(",", "")
                .trim()
            if (cleanAmt.startsWith("(") && cleanAmt.endsWith(")")) {
                isNegative = true
                cleanAmt = cleanAmt.removeSurrounding("(", ")")
            } else if (cleanAmt.startsWith("-")) {
                isNegative = true
                cleanAmt = cleanAmt.removePrefix("-")
            }
            val parsedAmount = cleanAmt.toDoubleOrNull() ?: 0.0

            // Determine transaction type
            val type = when {
                rawType.contains("inc", ignoreCase = true) || rawType.contains("credit", ignoreCase = true) || rawType.contains("deposit", ignoreCase = true) -> TransactionType.INCOME
                rawType.contains("trans", ignoreCase = true) -> TransactionType.TRANSFER
                rawType.contains("lend", ignoreCase = true) -> TransactionType.LEND
                rawType.contains("borrow", ignoreCase = true) -> TransactionType.BORROW
                rawType.contains("exp", ignoreCase = true) || rawType.contains("debit", ignoreCase = true) || rawType.contains("withdraw", ignoreCase = true) -> TransactionType.EXPENSE
                isNegative -> TransactionType.EXPENSE
                else -> TransactionType.EXPENSE
            }

            // Parse Date
            val parsedDate = parseFlexibleDate(rawDateAd)
            val finalDateBs = if (rawDateBs.isNotBlank()) rawDateBs else NepaliDateConverter.adToBs(parsedDate).formatted

            val fromAcc = if (type == TransactionType.EXPENSE || type == TransactionType.TRANSFER || type == TransactionType.LEND) {
                getOrCreateAccount(rawFrom, AccountType.BANK)
            } else null

            val toAcc = if (type == TransactionType.INCOME || type == TransactionType.TRANSFER || type == TransactionType.BORROW) {
                getOrCreateAccount(rawTo, AccountType.BANK)
            } else null

            val cat = getOrCreateCategory(rawCat, type.name)

            val txn = TransactionEntity(
                userId = userId,
                type = type.name,
                name = rawName,
                amount = Math.abs(parsedAmount),
                date = parsedDate,
                dateBs = finalDateBs,
                accountFromId = fromAcc?.id,
                accountToId = toAcc?.id,
                categoryId = cat?.id,
                notes = rawNotes
            )
            txnsToInsert.add(txn)
        }

        database.withTransaction {
            transactionDao.insertAll(txnsToInsert)
            recalculateBalances(userId)
        }
        txnsToInsert.size
    }

    suspend fun getAllTransactions(userId: String): List<TransactionEntity> = transactionDao.getAll(userId)

    suspend fun updateUser(user: User) = userDao.update(user)

    private fun parseFlexibleDate(rawDate: String): Long {
        if (rawDate.isBlank()) return System.currentTimeMillis()
        val trimmed = rawDate.trim()
        trimmed.toLongOrNull()?.let { num ->
            return if (num > 100_000_000_000L) num else num * 1000L
        }

        // Try standard java.time first (handles ISO-8601 with Z, offsets, fractional seconds)
        try {
            return java.time.Instant.parse(trimmed).toEpochMilli()
        } catch (_: Exception) {}
        try {
            return java.time.OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
        } catch (_: Exception) {}
        try {
            return java.time.ZonedDateTime.parse(trimmed).toInstant().toEpochMilli()
        } catch (_: Exception) {}
        try {
            return java.time.LocalDate.parse(trimmed).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {}

        // Fallback to SimpleDateFormat
        val cleanIso = trimmed.substringBefore("+").substringBefore("Z").trim()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy",
            "MM-dd-yyyy",
            "MM/dd/yyyy"
        )
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(cleanIso)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun parseAllCsvRecords(csvContent: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csvContent.length) {
            val c = csvContent[i]
            if (c == '"') {
                if (inQuotes && i + 1 < csvContent.length && csvContent[i + 1] == '"') {
                    currentField.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                currentRecord.add(currentField.toString().trim())
                currentField.clear()
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < csvContent.length && csvContent[i + 1] == '\n') {
                    i++
                }
                currentRecord.add(currentField.toString().trim())
                currentField.clear()
                if (currentRecord.any { it.isNotBlank() }) {
                    records.add(currentRecord.toList())
                }
                currentRecord.clear()
            } else {
                currentField.append(c)
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString().trim())
            if (currentRecord.any { it.isNotBlank() }) {
                records.add(currentRecord.toList())
            }
        }
        return records
    }

    private fun parseCsvLine(line: String): List<String> {
        return parseAllCsvRecords(line).firstOrNull() ?: emptyList()
    }
}
