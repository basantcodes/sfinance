package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.local.entities.LoanStatus
import com.example.data.local.entities.LoanType
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TransactionType
import com.example.data.local.entities.User
import com.example.data.local.entities.WishlistItem
import com.example.data.repository.DashboardData
import com.example.data.repository.FinanceRepository
import com.example.data.repository.WishlistItemWithAffordability
import com.example.data.security.PreferenceManager
import com.example.util.PdfExporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.TimeZone

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getDatabase(application)
    val preferenceManager = PreferenceManager(application)
    val repository = FinanceRepository(database, preferenceManager)

    // Auth State
    private val _authState = MutableStateFlow(AuthUiState(isLoading = true))
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    // Currency & Theme
    val currencyState: StateFlow<String> = preferenceManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NPR")

    val themeModeState: StateFlow<String> = preferenceManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val allowNegativeBalanceState: StateFlow<Boolean> = preferenceManager.allowNegativeBalanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Dashboard
    private val _dashboardYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val dashboardYear = _dashboardYear.asStateFlow()

    private val _dashboardMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val dashboardMonth = _dashboardMonth.asStateFlow()

    private val _dashboardData = MutableStateFlow(DashboardData())
    val dashboardData: StateFlow<DashboardData> = _dashboardData.asStateFlow()

    // Accounts, Categories, Budgets
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    // Transactions
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    // Transaction Filters
    val transactionFilterType = MutableStateFlow<String?>("ALL")
    val transactionFilterAccountId = MutableStateFlow<String?>(null)
    val transactionFilterCategoryId = MutableStateFlow<String?>(null)
    val transactionSearchQuery = MutableStateFlow("")

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        _transactions,
        transactionFilterType,
        transactionFilterAccountId,
        transactionFilterCategoryId,
        transactionSearchQuery
    ) { txns, type, accId, catId, query ->
        txns.filter { txn ->
            val matchType = type == null || type == "ALL" || txn.type == type
            val matchAcc = accId == null || txn.accountFromId == accId || txn.accountToId == accId
            val matchCat = catId == null || txn.categoryId == catId
            val matchQuery = query.isBlank() || (txn.name?.contains(query, ignoreCase = true) == true) ||
                    (txn.notes?.contains(query, ignoreCase = true) == true) ||
                    (txn.dateBs.contains(query))
            matchType && matchAcc && matchCat && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Loans
    private val _loans = MutableStateFlow<List<Loan>>(emptyList())
    val loans: StateFlow<List<Loan>> = _loans.asStateFlow()

    // Wishlist with Affordability
    private val _wishlistWithAffordability = MutableStateFlow<List<WishlistItemWithAffordability>>(emptyList())
    val wishlistWithAffordability: StateFlow<List<WishlistItemWithAffordability>> = _wishlistWithAffordability.asStateFlow()

    // Journal
    private val _journalEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val journalEntries: StateFlow<List<JournalEntry>> = _journalEntries.asStateFlow()

    // Event Messages (Snackbar / Alerts)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        checkCurrentAuth()
    }

    private fun checkCurrentAuth() {
        viewModelScope.launch {
            val user = repository.getActiveUser()
            if (user != null) {
                val effectiveUser = if (user.name == "Prashant Sharma") {
                    val renamed = user.copy(name = "Sarmila Adhikari")
                    repository.updateUser(renamed)
                    renamed
                } else {
                    user
                }
                _authState.value = AuthUiState(isAuthenticated = true, currentUser = effectiveUser, isLoading = false)
                onUserLoggedIn(effectiveUser)
            } else {
                // Auto create demo / primary user if empty for instant first-time delight
                val demoResult = repository.register("Sarmila Adhikari", "demo@finance.np", "password123")
                if (demoResult.isSuccess) {
                    val demoUser = demoResult.getOrThrow()
                    _authState.value = AuthUiState(isAuthenticated = true, currentUser = demoUser, isLoading = false)
                    onUserLoggedIn(demoUser)
                } else {
                    _authState.value = AuthUiState(isAuthenticated = false, isLoading = false)
                }
            }
        }
    }

    private fun onUserLoggedIn(user: User) {
        viewModelScope.launch {
            repository.checkAndGenerateAutoInterests(user.id)
            refreshAllData(user.id)
        }
    }

    fun refreshAllData(targetUserId: String? = null) {
        val userId = targetUserId ?: _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            // Collect Accounts
            repository.getAccountsFlow(userId).collect { accList ->
                _accounts.value = accList
                refreshDashboard(userId)
                refreshWishlist(userId)
            }
        }
        viewModelScope.launch {
            // Collect Categories
            repository.getCategoriesFlow(userId).collect { catList ->
                _categories.value = catList
            }
        }
        viewModelScope.launch {
            // Collect Budgets
            repository.getBudgetsFlow(userId).collect { bList ->
                _budgets.value = bList
                refreshDashboard(userId)
            }
        }
        viewModelScope.launch {
            // Collect Transactions
            repository.getTransactionsFlow(userId).collect { txns ->
                if (_transactions.value.isEmpty() && txns.isNotEmpty()) {
                    val latest = txns.maxByOrNull { it.date }
                    if (latest != null) {
                        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = latest.date }
                        _dashboardYear.value = c.get(Calendar.YEAR)
                        _dashboardMonth.value = c.get(Calendar.MONTH) + 1
                    }
                }
                _transactions.value = txns
                refreshDashboard(userId)
                refreshWishlist(userId)
            }
        }
        viewModelScope.launch {
            // Collect Loans
            repository.getLoansFlow(userId).collect { lList ->
                _loans.value = lList
                refreshDashboard(userId)
            }
        }
        viewModelScope.launch {
            // Collect Journal
            repository.getJournalEntriesFlow(userId).collect { jList ->
                _journalEntries.value = jList
            }
        }
        viewModelScope.launch {
            // Collect Wishlist
            repository.getWishlistFlow(userId).collect {
                refreshWishlist(userId)
            }
        }
    }

    fun refreshDashboard(targetUserId: String? = null) {
        val userId = targetUserId ?: _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val data = repository.getDashboardData(userId, _dashboardYear.value, _dashboardMonth.value)
            _dashboardData.value = data
        }
    }

    fun setDashboardPeriod(year: Int, month: Int) {
        _dashboardYear.value = year
        _dashboardMonth.value = month
        _authState.value.currentUser?.let { refreshDashboard(it.id) }
    }

    private fun refreshWishlist(targetUserId: String? = null) {
        val userId = targetUserId ?: _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val rawItems = database.wishlistDao().getAll(userId)
            val computed = rawItems.map { item ->
                repository.calculateAffordability(item)
            }
            _wishlistWithAffordability.value = computed
        }
    }


    // --- AUTH ACTIONS ---
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.login(email, pass)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthUiState(isAuthenticated = true, currentUser = user, isLoading = false)
                    onUserLoggedIn(user)
                    _uiEvent.emit("Logged in as ${user.name}")
                },
                onFailure = { err ->
                    _authState.value = AuthUiState(isAuthenticated = false, error = err.message, isLoading = false)
                    _uiEvent.emit(err.message ?: "Login failed")
                }
            )
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = repository.register(name, email, pass)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthUiState(isAuthenticated = true, currentUser = user, isLoading = false)
                    onUserLoggedIn(user)
                    _uiEvent.emit("Account created for ${user.name}")
                },
                onFailure = { err ->
                    _authState.value = AuthUiState(isAuthenticated = false, error = err.message, isLoading = false)
                    _uiEvent.emit(err.message ?: "Registration failed")
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthUiState(isAuthenticated = false, isLoading = false)
    }

    // --- TRANSACTION ACTIONS ---
    fun createTransaction(
        type: TransactionType,
        amount: Double,
        date: Long,
        dateBs: String,
        name: String?,
        notes: String?,
        fromAccountId: String?,
        toAccountId: String?,
        categoryId: String?
    ) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val result = repository.createTransaction(
                userId = userId,
                type = type,
                amount = amount,
                date = date,
                dateBs = dateBs,
                name = name,
                notes = notes,
                accountFromId = fromAccountId,
                accountToId = toAccountId,
                categoryId = categoryId
            )
            result.fold(
                onSuccess = {
                    _uiEvent.emit("Transaction added successfully")
                },
                onFailure = { err ->
                    _uiEvent.emit(err.message ?: "Failed to add transaction")
                }
            )
        }
    }

    fun updateTransaction(
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
    ) {
        viewModelScope.launch {
            val result = repository.updateTransaction(
                oldTransaction,
                newType,
                newAmount,
                newDate,
                newDateBs,
                newName,
                newNotes,
                newAccountFromId,
                newAccountToId,
                newCategoryId
            )
            result.fold(
                onSuccess = { _uiEvent.emit("Transaction updated") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Update failed") }
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(transaction)
            result.fold(
                onSuccess = { _uiEvent.emit("Transaction deleted") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Delete failed") }
            )
        }
    }

    // --- ACCOUNT ACTIONS ---
    fun createAccount(name: String, type: AccountType, balance: Double, color: String, notes: String?) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val acc = repository.createAccount(
                userId = userId,
                name = name,
                type = type,
                initialBalance = balance,
                color = color,
                currency = currencyState.value,
                notes = notes
            )
            _uiEvent.emit("Account '${acc.name}' created")
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
            _uiEvent.emit("Account updated")
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            val res = repository.deleteAccount(account)
            res.fold(
                onSuccess = { _uiEvent.emit("Account deleted") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Cannot delete account") }
            )
        }
    }

    // --- CATEGORY ACTIONS ---
    fun createCategory(name: String, type: CategoryType, icon: String? = null) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.createCategory(userId, name, type, icon)
            _uiEvent.emit("Category '$name' created")
        }
    }

    // --- LOAN ACTIONS ---
    fun createLoan(
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
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val res = repository.createLoan(
                userId, counterparty, type, principal, rate, startDate, accountId, mode, frequency, notes
            )
            res.fold(
                onSuccess = { _uiEvent.emit("Loan with $counterparty recorded") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Failed to record loan") }
            )
        }
    }

    fun markLoanRepaid(loan: Loan, accountId: String?) {
        viewModelScope.launch {
            val res = repository.markLoanRepaid(loan, accountId)
            res.fold(
                onSuccess = { _uiEvent.emit("Loan with ${loan.counterparty} marked as repaid") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Repayment failed") }
            )
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            _uiEvent.emit("Loan deleted")
        }
    }

    // --- BUDGET ACTIONS ---
    fun saveBudget(categoryId: String, monthlyLimit: Double, rollover: Boolean) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.setBudget(userId, categoryId, monthlyLimit, rollover)
            _uiEvent.emit("Budget updated")
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
            _uiEvent.emit("Budget removed")
        }
    }

    // --- WISHLIST ACTIONS ---
    fun saveWishlistItem(
        name: String,
        cost: Double,
        priority: String,
        preferredDate: Long?,
        category: String?,
        notes: String?
    ) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.createWishlistItem(userId, name, cost, preferredDate, priority, category, notes)
            _uiEvent.emit("Wishlist item '$name' added")
        }
    }

    fun purchaseWishlistItem(item: WishlistItem, createExpense: Boolean, accountId: String?) {
        viewModelScope.launch {
            val res = repository.purchaseWishlistItem(
                item = item,
                createTransaction = createExpense,
                accountId = accountId,
                categoryId = _categories.value.firstOrNull { it.type == CategoryType.EXPENSE.name }?.id
            )
            res.fold(
                onSuccess = { _uiEvent.emit("Purchased '${item.name}'") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Purchase failed") }
            )
        }
    }

    fun deleteWishlistItem(item: WishlistItem) {
        viewModelScope.launch {
            repository.deleteWishlistItem(item)
            _uiEvent.emit("Wishlist item removed")
        }
    }

    // --- JOURNAL ACTIONS ---
    fun saveJournalEntry(content: String, mood: String?, date: Long) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.createJournalEntry(userId, content, mood, date)
            _uiEvent.emit("Journal entry saved")
        }
    }

    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entry)
            _uiEvent.emit("Journal entry deleted")
        }
    }

    // --- SETTINGS ACTIONS ---
    fun setCurrency(currency: String) {
        viewModelScope.launch {
            preferenceManager.setCurrency(currency)
            _uiEvent.emit("Currency set to $currency")
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferenceManager.setThemeMode(mode)
            _uiEvent.emit("Theme updated")
        }
    }

    fun setAllowNegativeBalance(allow: Boolean) {
        viewModelScope.launch {
            preferenceManager.setAllowNegativeBalance(allow)
            _uiEvent.emit(if (allow) "Negative balances permitted" else "Negative balances strictly prevented")
        }
    }

    fun recalculateBalances() {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val res = repository.recalculateBalances(userId)
            res.fold(
                onSuccess = { _uiEvent.emit("Balances recalculated from transaction history") },
                onFailure = { err -> _uiEvent.emit(err.message ?: "Recalculate failed") }
            )
        }
    }

    // --- EXPORT & PDF ---
    fun generatePdfStatement(onPdfReady: (File) -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val file = PdfExporter.generateStatementPdf(
                context = app,
                dashboardData = _dashboardData.value,
                transactions = _transactions.value,
                accounts = _accounts.value,
                categories = _categories.value.associateBy { it.id },
                currency = currencyState.value
            )
            onPdfReady(file)
            _uiEvent.emit("PDF Statement generated: ${file.name}")
        }
    }

    suspend fun getExportJson(): String {
        val userId = _authState.value.currentUser?.id ?: return "{}"
        return repository.exportFullJson(userId)
    }

    suspend fun getExportCsv(): String {
        val userId = _authState.value.currentUser?.id ?: return ""
        return repository.exportTransactionsCsv(userId)
    }

    fun importJson(json: String) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val res = repository.importFullJson(userId, json)
            res.fold(
                onSuccess = { count ->
                    val txns = repository.getAllTransactions(userId)
                    val latest = txns.maxByOrNull { it.date }
                    if (latest != null) {
                        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = latest.date }
                        _dashboardYear.value = c.get(Calendar.YEAR)
                        _dashboardMonth.value = c.get(Calendar.MONTH) + 1
                    }
                    refreshAllData(userId)
                    refreshDashboard(userId)
                    refreshWishlist(userId)
                    _uiEvent.emit("Imported $count records from JSON")
                },
                onFailure = { err -> _uiEvent.emit("Import failed: ${err.message}") }
            )
        }
    }

    fun importCsv(csv: String) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val res = repository.importTransactionsCsv(userId, csv)
            res.fold(
                onSuccess = { count ->
                    val txns = repository.getAllTransactions(userId)
                    val latest = txns.maxByOrNull { it.date }
                    if (latest != null) {
                        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = latest.date }
                        _dashboardYear.value = c.get(Calendar.YEAR)
                        _dashboardMonth.value = c.get(Calendar.MONTH) + 1
                    }
                    refreshAllData(userId)
                    refreshDashboard(userId)
                    refreshWishlist(userId)
                    _uiEvent.emit("Imported $count transactions from CSV")
                },
                onFailure = { err -> _uiEvent.emit("CSV Import failed: ${err.message}") }
            )
        }
    }
}
