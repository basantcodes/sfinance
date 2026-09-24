package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.R
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val accountFeatureViewModel = AccountFeatureViewModel(application, repository) { msg ->
        viewModelScope.launch { _uiEvent.emit(msg) }
    }

    private val transactionFeatureViewModel = TransactionFeatureViewModel(application, repository) { msg ->
        viewModelScope.launch { _uiEvent.emit(msg) }
    }

    private val budgetCategoryFeatureViewModel = BudgetCategoryFeatureViewModel(application, repository) { msg ->
        viewModelScope.launch { _uiEvent.emit(msg) }
    }

    private val loanWishlistJournalFeatureViewModel = LoanWishlistJournalFeatureViewModel(application, repository) { msg ->
        viewModelScope.launch { _uiEvent.emit(msg) }
    }

    private val authSettingsExportFeatureViewModel = AuthSettingsExportFeatureViewModel(
        app = getApplication(),
        repository = repository,
        preferenceManager = preferenceManager
    ) { msg ->
        viewModelScope.launch { _uiEvent.emit(msg) }
    }

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

    val languageState: StateFlow<String> = preferenceManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), preferenceManager.getLanguage())

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

    private var derivedRefreshJob: Job? = null
    private val userDataJobs = mutableListOf<Job>()

    private fun checkCurrentAuth() {
        viewModelScope.launch {
            val user = repository.getActiveUser()
            if (user != null) {
                _authState.value = AuthUiState(isAuthenticated = true, currentUser = user, isLoading = false)
                onUserLoggedIn(user)
            } else {
                _authState.value = AuthUiState(isAuthenticated = false, isLoading = false)
            }
        }
    }

    private fun stopUserDataCollectors() {
        userDataJobs.forEach { it.cancel() }
        userDataJobs.clear()
    }

    private fun scheduleDerivedRefresh(userId: String) {
        if (derivedRefreshJob?.isActive == true) return
        derivedRefreshJob = viewModelScope.launch {
            delay(100)
            refreshDashboard(userId)
            refreshWishlist(userId)
            derivedRefreshJob = null
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
        stopUserDataCollectors()

        userDataJobs += viewModelScope.launch {
            repository.getAccountsFlow(userId).collect { accList ->
                _accounts.value = accList
                scheduleDerivedRefresh(userId)
            }
        }
        userDataJobs += viewModelScope.launch {
            repository.getCategoriesFlow(userId).collect { catList ->
                _categories.value = catList
            }
        }
        userDataJobs += viewModelScope.launch {
            repository.getBudgetsFlow(userId).collect { bList ->
                _budgets.value = bList
                scheduleDerivedRefresh(userId)
            }
        }
        userDataJobs += viewModelScope.launch {
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
                scheduleDerivedRefresh(userId)
            }
        }
        userDataJobs += viewModelScope.launch {
            repository.getLoansFlow(userId).collect { lList ->
                _loans.value = lList
                scheduleDerivedRefresh(userId)
            }
        }
        userDataJobs += viewModelScope.launch {
            repository.getJournalEntriesFlow(userId).collect { jList ->
                _journalEntries.value = jList
            }
        }
        userDataJobs += viewModelScope.launch {
            repository.getWishlistFlow(userId).collect { wishlistItems ->
                _wishlistWithAffordability.value = repository.getWishlistWithAffordability(userId)
                scheduleDerivedRefresh(userId)
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
            val computed = repository.getWishlistWithAffordability(userId)
            _wishlistWithAffordability.value = computed
        }
    }


    // --- AUTH ACTIONS ---
    fun login(username: String, pass: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authSettingsExportFeatureViewModel.login(username, pass)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthUiState(isAuthenticated = true, currentUser = user, isLoading = false)
                    onUserLoggedIn(user)
                },
                onFailure = { err ->
                    _authState.value = AuthUiState(isAuthenticated = false, error = err.message, isLoading = false)
                }
            )
        }
    }

    fun register(name: String, username: String, pass: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            val result = authSettingsExportFeatureViewModel.register(name, username, pass)
            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthUiState(isAuthenticated = true, currentUser = user, isLoading = false)
                    onUserLoggedIn(user)
                },
                onFailure = { err ->
                    _authState.value = AuthUiState(isAuthenticated = false, error = err.message, isLoading = false)
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthUiState(isAuthenticated = false, isLoading = false)
        viewModelScope.launch { _uiEvent.emit(getApplication<Application>().getString(R.string.signed_out)) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            authSettingsExportFeatureViewModel.setLanguage(language)
        }
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
        categoryId: String?,
        feeAmount: Double = 0.0
    ) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            transactionFeatureViewModel.createTransaction(
                userId = userId,
                type = type,
                amount = amount,
                date = date,
                dateBs = dateBs,
                name = name,
                notes = notes,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                feeAmount = feeAmount
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
        newCategoryId: String?,
        newFeeAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            transactionFeatureViewModel.updateTransaction(
                oldTransaction = oldTransaction,
                newType = newType,
                newAmount = newAmount,
                newDate = newDate,
                newDateBs = newDateBs,
                newName = newName,
                newNotes = newNotes,
                newAccountFromId = newAccountFromId,
                newAccountToId = newAccountToId,
                newCategoryId = newCategoryId,
                newFeeAmount = newFeeAmount
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionFeatureViewModel.deleteTransaction(transaction)
        }
    }

    // --- ACCOUNT ACTIONS ---
    fun createAccount(name: String, type: AccountType, balance: Double, color: String, notes: String?) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            accountFeatureViewModel.createAccount(
                userId = userId,
                name = name,
                type = type,
                balance = balance,
                color = color,
                currency = currencyState.value,
                notes = notes
            )
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountFeatureViewModel.updateAccount(account)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            accountFeatureViewModel.deleteAccount(account)
        }
    }

    // --- CATEGORY ACTIONS ---
    fun createCategory(name: String, type: CategoryType, icon: String? = null) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            budgetCategoryFeatureViewModel.createCategory(userId, name, type, icon)
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
            loanWishlistJournalFeatureViewModel.createLoan(
                userId,
                counterparty,
                type,
                principal,
                rate,
                startDate,
                accountId,
                mode,
                frequency,
                notes
            )
        }
    }

    fun markLoanRepaid(loan: Loan, accountId: String?, amount: Double) {
        viewModelScope.launch {
            loanWishlistJournalFeatureViewModel.markLoanRepaid(loan, accountId, amount)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanWishlistJournalFeatureViewModel.deleteLoan(loan)
        }
    }

    // --- BUDGET ACTIONS ---
    fun saveBudget(categoryId: String, monthlyLimit: Double, rollover: Boolean) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            budgetCategoryFeatureViewModel.saveBudget(userId, categoryId, monthlyLimit, rollover)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetCategoryFeatureViewModel.deleteBudget(budget)
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
            loanWishlistJournalFeatureViewModel.saveWishlistItem(
                userId,
                name,
                cost,
                priority,
                preferredDate,
                category,
                notes
            )
        }
    }

    fun purchaseWishlistItem(item: WishlistItem, createExpense: Boolean, accountId: String?) {
        viewModelScope.launch {
            loanWishlistJournalFeatureViewModel.purchaseWishlistItem(
                item = item,
                createExpense = createExpense,
                accountId = accountId,
                categoryId = _categories.value.firstOrNull { it.type == CategoryType.EXPENSE.name }?.id
            )
        }
    }

    fun deleteWishlistItem(item: WishlistItem) {
        viewModelScope.launch {
            loanWishlistJournalFeatureViewModel.deleteWishlistItem(item)
        }
    }

    // --- JOURNAL ACTIONS ---
    fun saveJournalEntry(content: String, mood: String?, date: Long) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            loanWishlistJournalFeatureViewModel.saveJournalEntry(userId, content, mood, date)
        }
    }

    fun deleteJournalEntry(entry: JournalEntry) {
        viewModelScope.launch {
            loanWishlistJournalFeatureViewModel.deleteJournalEntry(entry)
        }
    }

    // --- SETTINGS ACTIONS ---
    fun setCurrency(currency: String) {
        viewModelScope.launch {
            authSettingsExportFeatureViewModel.setCurrency(currency)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            authSettingsExportFeatureViewModel.setThemeMode(mode)
        }
    }

    fun setAllowNegativeBalance(allow: Boolean) {
        viewModelScope.launch {
            authSettingsExportFeatureViewModel.setAllowNegativeBalance(allow)
        }
    }

    fun recalculateBalances() {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            authSettingsExportFeatureViewModel.recalculateBalances(userId)
        }
    }

    // --- EXPORT & PDF ---
    fun generatePdfStatement(onPdfReady: (File) -> Unit) {
        viewModelScope.launch {
            authSettingsExportFeatureViewModel.generatePdfStatement(
                dashboardData = _dashboardData.value,
                transactions = _transactions.value,
                accounts = _accounts.value,
                categories = _categories.value,
                currency = currencyState.value,
                onPdfReady = onPdfReady
            )
        }
    }

    suspend fun getExportJson(): String {
        val userId = _authState.value.currentUser?.id ?: return "{}"
        return authSettingsExportFeatureViewModel.exportJson(userId)
    }

    suspend fun getExportCsv(): String {
        val userId = _authState.value.currentUser?.id ?: return ""
        return authSettingsExportFeatureViewModel.exportCsv(userId)
    }

    fun importJson(json: String, onResult: (Result<Int>) -> Unit = {}) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val res = authSettingsExportFeatureViewModel.importJson(userId, json)
            onResult(res)
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
                },
                onFailure = { }
            )
        }
    }

    fun importCsv(csv: String) {
        val userId = _authState.value.currentUser?.id ?: return
        viewModelScope.launch {
            val res = authSettingsExportFeatureViewModel.importCsv(userId, csv)
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
                },
                onFailure = { }
            )
        }
    }
}
