package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar
import com.example.data.local.entities.Account
import com.example.data.local.entities.Budget
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.WishlistItem
import com.example.ui.dialogs.AccountDialog
import com.example.ui.dialogs.BudgetDialog
import com.example.ui.dialogs.JournalDialog
import com.example.ui.dialogs.LoanDialog
import com.example.ui.dialogs.TransactionDialog
import com.example.ui.dialogs.WishlistDialog
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.AppShellState
import com.example.ui.navigation.rememberAppShellState
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocsScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LoansScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FinanceJournalTheme
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeModeState.collectAsState()
            val authState by viewModel.authState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { message ->
                    snackbarHostState.showSnackbar(message)
                }
            }

            FinanceJournalTheme(themeMode = themeMode) {
                if (!authState.isAuthenticated) {
                    AuthScreen(
                        isLoading = authState.isLoading,
                        errorMessage = authState.error,
                        onLogin = { email, pass -> viewModel.login(email, pass) },
                        onRegister = { name, email, pass -> viewModel.register(name, email, pass) }
                    )
                } else {
                    MainAppScaffold(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: FinanceViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val shellState = rememberAppShellState()
    val currentScreen = shellState.currentScreen

    BackHandler {
        if (shellState.showExitConfirmDialog) {
            shellState.showExitConfirmDialog = false
            return@BackHandler
        }

        if (shellState.hasOpenModal()) {
            shellState.clearAllModalState()
            return@BackHandler
        }

        val isMoreSubScreen = currentScreen in listOf(
            AppScreen.LOANS,
            AppScreen.WISHLIST,
            AppScreen.JOURNAL,
            AppScreen.DOCS
        )
        if (isMoreSubScreen) {
            shellState.resetToDashboard()
            return@BackHandler
        }

        shellState.showExitConfirmDialog = true
    }

    if (shellState.showExitConfirmDialog) {
        Dialog(
            onDismissRequest = { shellState.showExitConfirmDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Exit app?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to exit Finance Journal?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shellState.showExitConfirmDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick = {
                                shellState.showExitConfirmDialog = false
                                (context as? Activity)?.finish()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Exit", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currency by viewModel.currencyState.collectAsState()

    val isMoreActive = currentScreen in listOf(
        AppScreen.LOANS,
        AppScreen.WISHLIST,
        AppScreen.JOURNAL,
        AppScreen.DOCS
    )

    val authState by viewModel.authState.collectAsState()
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val userName = authState.currentUser?.name?.trim()?.ifBlank { null } ?: "there"
    val greetingTitle = "$greeting, $userName"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMoreActive) {
                        IconButton(
                            onClick = { shellState.resetToDashboard() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (currentScreen) {
                            AppScreen.DASHBOARD -> greetingTitle
                            AppScreen.TRANSACTIONS -> "Transactions"
                            AppScreen.ACCOUNTS -> "Accounts"
                            AppScreen.BUDGETS -> "Budgets"
                            AppScreen.LOANS -> "Loans & Debt"
                            AppScreen.WISHLIST -> "Wishlist"
                            AppScreen.JOURNAL -> "Journal"
                            AppScreen.DOCS -> "Docs & Settings"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen != AppScreen.DOCS) {
                FloatingActionButton(
                    onClick = {
                        when (currentScreen) {
                            AppScreen.DASHBOARD, AppScreen.TRANSACTIONS -> shellState.showAddTransactionDialog = true
                            AppScreen.ACCOUNTS -> shellState.showAddAccountDialog = true
                            AppScreen.BUDGETS -> shellState.showAddBudgetDialog = true
                            AppScreen.LOANS -> shellState.showAddLoanDialog = true
                            AppScreen.WISHLIST -> shellState.showAddWishlistDialog = true
                            AppScreen.JOURNAL -> shellState.showAddJournalDialog = true
                            else -> {}
                        }
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.padding(bottom = 12.dp, end = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple(AppScreen.DASHBOARD, Icons.Default.Dashboard, "Dash"),
                        Triple(AppScreen.TRANSACTIONS, Icons.Default.ReceiptLong, "Txns"),
                        Triple(AppScreen.ACCOUNTS, Icons.Default.AccountBalance, "Accounts"),
                        Triple(AppScreen.BUDGETS, Icons.Default.PieChart, "Budget"),
                        Triple(null, Icons.Default.Menu, "More")
                    )

                    tabs.forEach { (screen, icon, label) ->
                        val isSelected = if (screen != null) currentScreen == screen else isMoreActive
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (screen != null) {
                                        shellState.navigateTo(screen)
                                    } else {
                                        shellState.showMoreSheet = true
                                    }
                                }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) EmeraldPrimary else Color(0xFF6B7280),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) EmeraldPrimary else Color(0xFF6B7280),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { shellState.navigateTo(AppScreen.TRANSACTIONS) },
                    onNavigateToBudgets = { shellState.navigateTo(AppScreen.BUDGETS) },
                    onNavigateToLoans = { shellState.navigateTo(AppScreen.LOANS) },
                    onOpenAddTransaction = { shellState.showAddTransactionDialog = true }
                )
                AppScreen.TRANSACTIONS -> TransactionsScreen(
                    viewModel = viewModel,
                    onOpenAddTransaction = { shellState.showAddTransactionDialog = true },
                    onOpenEditTransaction = { txn -> shellState.transactionToEdit = txn }
                )
                AppScreen.ACCOUNTS -> AccountsScreen(
                    viewModel = viewModel,
                    onOpenAddAccount = { shellState.showAddAccountDialog = true },
                    onOpenEditAccount = { acc -> shellState.accountToEdit = acc }
                )
                AppScreen.BUDGETS -> BudgetsScreen(
                    viewModel = viewModel,
                    onOpenAddBudget = { shellState.showAddBudgetDialog = true },
                    onOpenEditBudget = { b -> shellState.budgetToEdit = b }
                )
                AppScreen.LOANS -> LoansScreen(
                    viewModel = viewModel,
                    onOpenAddLoan = { shellState.showAddLoanDialog = true }
                )
                AppScreen.WISHLIST -> WishlistScreen(
                    viewModel = viewModel,
                    onOpenAddWishlist = { shellState.showAddWishlistDialog = true }
                )
                AppScreen.JOURNAL -> JournalScreen(
                    viewModel = viewModel,
                    onOpenAddEntry = { shellState.showAddJournalDialog = true },
                    onOpenEditEntry = { entry -> shellState.journalEntryToEdit = entry }
                )
                AppScreen.DOCS -> DocsScreen(
                    viewModel = viewModel,
                    onLogout = { viewModel.logout() }
                )
            }
        }
    }

    // --- MORE BOTTOM SHEET ---
    if (shellState.showMoreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { shellState.showMoreSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "More Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 1. Loans & Debt
                MoreSheetItem(
                    icon = Icons.Default.Handshake,
                    iconBgColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "Loans & Debt",
                    subtitle = "Track lent, borrowed, interest & repayments",
                    isSelected = currentScreen == AppScreen.LOANS,
                    onClick = {
                        shellState.showMoreSheet = false
                        shellState.navigateTo(AppScreen.LOANS)
                    }
                )

                // 2. Wishlist
                MoreSheetItem(
                    icon = Icons.Default.CardGiftcard,
                    iconBgColor = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    title = "Wishlist",
                    subtitle = "Financial goals & affordability forecasting",
                    isSelected = currentScreen == AppScreen.WISHLIST,
                    onClick = {
                        shellState.showMoreSheet = false
                        shellState.navigateTo(AppScreen.WISHLIST)
                    }
                )

                // 3. Journal
                MoreSheetItem(
                    icon = Icons.Default.AutoStories,
                    iconBgColor = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB),
                    title = "Journal",
                    subtitle = "Financial thoughts, reflections & mood logs",
                    isSelected = currentScreen == AppScreen.JOURNAL,
                    onClick = {
                        shellState.showMoreSheet = false
                        shellState.navigateTo(AppScreen.JOURNAL)
                    }
                )

                // 4. Docs & Settings
                MoreSheetItem(
                    icon = Icons.Default.Description,
                    iconBgColor = Color(0xFFCCFBF1),
                    iconTint = Color(0xFF0D9488),
                    title = "Docs & Data",
                    subtitle = "PDF statements, JSON/CSV backups & settings",
                    isSelected = currentScreen == AppScreen.DOCS,
                    onClick = {
                        shellState.showMoreSheet = false
                        shellState.navigateTo(AppScreen.DOCS)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 5. Logout shortcut
                MoreSheetItem(
                    icon = Icons.Default.Logout,
                    iconBgColor = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFEF4444),
                    title = "Sign Out",
                    subtitle = "Log out from your offline account",
                    isSelected = false,
                    onClick = {
                        shellState.showMoreSheet = false
                        viewModel.logout()
                    }
                )
            }
        }
    }

    // --- TRANSACTION DIALOG ---
    if (shellState.showAddTransactionDialog || shellState.transactionToEdit != null) {
        TransactionDialog(
            transactionToEdit = shellState.transactionToEdit,
            accounts = accounts,
            categories = categories,
            currency = currency,
            onDismissRequest = {
                shellState.showAddTransactionDialog = false
                shellState.transactionToEdit = null
            },
            onSaveTransaction = { type, amount, date, dateBs, name, notes, fromAcc, toAcc, catId ->
                val old = shellState.transactionToEdit
                if (old == null) {
                    viewModel.createTransaction(type, amount, date, dateBs, name, notes, fromAcc, toAcc, catId)
                } else {
                    viewModel.updateTransaction(old, type, amount, date, dateBs, name, notes, fromAcc, toAcc, catId)
                }
                shellState.showAddTransactionDialog = false
                shellState.transactionToEdit = null
            },
            onQuickCreateCategory = { name, type ->
                viewModel.createCategory(name, type)
            }
        )
    }

    // --- ACCOUNT DIALOG ---
    if (shellState.showAddAccountDialog || shellState.accountToEdit != null) {
        AccountDialog(
            accountToEdit = shellState.accountToEdit,
            defaultCurrency = currency,
            onDismissRequest = {
                shellState.showAddAccountDialog = false
                shellState.accountToEdit = null
            },
            onSaveAccount = { name, type, balance, color, notes ->
                val old = shellState.accountToEdit
                if (old == null) {
                    viewModel.createAccount(name, type, balance, color, notes)
                } else {
                    viewModel.updateAccount(old.copy(name = name, type = type.name, color = color, notes = notes))
                }
                shellState.showAddAccountDialog = false
                shellState.accountToEdit = null
            }
        )
    }

    // --- BUDGET DIALOG ---
    if (shellState.showAddBudgetDialog || shellState.budgetToEdit != null) {
        BudgetDialog(
            budgetToEdit = shellState.budgetToEdit,
            categories = categories,
            currency = currency,
            onDismissRequest = {
                shellState.showAddBudgetDialog = false
                shellState.budgetToEdit = null
            },
            onSaveBudget = { categoryId, limit, rollover ->
                viewModel.saveBudget(categoryId, limit, rollover)
                shellState.showAddBudgetDialog = false
                shellState.budgetToEdit = null
            }
        )
    }

    // --- LOAN DIALOG ---
    if (shellState.showAddLoanDialog || shellState.loanToEdit != null) {
        LoanDialog(
            loanToEdit = shellState.loanToEdit,
            accounts = accounts,
            currency = currency,
            onDismissRequest = {
                shellState.showAddLoanDialog = false
                shellState.loanToEdit = null
            },
            onSaveLoan = { counterparty, type, principal, rate, startDate, accId, mode, freq, notes ->
                viewModel.createLoan(counterparty, type, principal, rate, startDate, accId, mode, freq, notes)
                shellState.showAddLoanDialog = false
                shellState.loanToEdit = null
            }
        )
    }

    // --- WISHLIST DIALOG ---
    if (shellState.showAddWishlistDialog || shellState.wishlistItemToEdit != null) {
        WishlistDialog(
            itemToEdit = shellState.wishlistItemToEdit,
            currency = currency,
            onDismissRequest = {
                shellState.showAddWishlistDialog = false
                shellState.wishlistItemToEdit = null
            },
            onSaveItem = { name, cost, priority, preferredDate, category, notes ->
                viewModel.saveWishlistItem(name, cost, priority, preferredDate, category, notes)
                shellState.showAddWishlistDialog = false
                shellState.wishlistItemToEdit = null
            }
        )
    }

    // --- JOURNAL DIALOG ---
    if (shellState.showAddJournalDialog || shellState.journalEntryToEdit != null) {
        JournalDialog(
            entryToEdit = shellState.journalEntryToEdit,
            onDismissRequest = {
                shellState.showAddJournalDialog = false
                shellState.journalEntryToEdit = null
            },
            onSaveEntry = { content, mood, date ->
                viewModel.saveJournalEntry(content, mood, date)
                shellState.showAddJournalDialog = false
                shellState.journalEntryToEdit = null
            }
        )
    }
}

@Composable
fun MoreSheetItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
