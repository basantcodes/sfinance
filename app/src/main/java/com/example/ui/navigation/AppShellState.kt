package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.local.entities.Account
import com.example.data.local.entities.Budget
import com.example.data.local.entities.JournalEntry
import com.example.data.local.entities.Loan
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.WishlistItem

enum class AppScreen {
    DASHBOARD,
    TRANSACTIONS,
    ACCOUNTS,
    BUDGETS,
    LOANS,
    WISHLIST,
    JOURNAL,
    DOCS
}

@Stable
class AppShellState {
    var currentScreen by mutableStateOf(AppScreen.DASHBOARD)
    val screenStack = mutableStateListOf(AppScreen.DASHBOARD)

    var showExitConfirmDialog by mutableStateOf(false)
    var showMoreSheet by mutableStateOf(false)

    var showAddTransactionDialog by mutableStateOf(false)
    var transactionToEdit by mutableStateOf<TransactionEntity?>(null)

    var showAddAccountDialog by mutableStateOf(false)
    var accountToEdit by mutableStateOf<Account?>(null)

    var showAddBudgetDialog by mutableStateOf(false)
    var budgetToEdit by mutableStateOf<Budget?>(null)

    var showAddLoanDialog by mutableStateOf(false)
    var loanToEdit by mutableStateOf<Loan?>(null)

    var showAddWishlistDialog by mutableStateOf(false)
    var wishlistItemToEdit by mutableStateOf<WishlistItem?>(null)

    var showAddJournalDialog by mutableStateOf(false)
    var journalEntryToEdit by mutableStateOf<JournalEntry?>(null)

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.DASHBOARD) {
            screenStack.clear()
            screenStack.add(AppScreen.DASHBOARD)
            currentScreen = AppScreen.DASHBOARD
            return
        }

        currentScreen = screen
    }

    fun resetToDashboard() {
        screenStack.clear()
        screenStack.add(AppScreen.DASHBOARD)
        currentScreen = AppScreen.DASHBOARD
    }

    fun clearAllModalState() {
        showMoreSheet = false
        showAddTransactionDialog = false
        transactionToEdit = null
        showAddAccountDialog = false
        accountToEdit = null
        showAddBudgetDialog = false
        budgetToEdit = null
        showAddLoanDialog = false
        loanToEdit = null
        showAddWishlistDialog = false
        wishlistItemToEdit = null
        showAddJournalDialog = false
        journalEntryToEdit = null
    }

    fun hasOpenModal(): Boolean {
        return showMoreSheet ||
            showAddTransactionDialog || transactionToEdit != null ||
            showAddAccountDialog || accountToEdit != null ||
            showAddBudgetDialog || budgetToEdit != null ||
            showAddLoanDialog || loanToEdit != null ||
            showAddWishlistDialog || wishlistItemToEdit != null ||
            showAddJournalDialog || journalEntryToEdit != null
    }
}

@Composable
fun rememberAppShellState(): AppShellState {
    return remember { AppShellState() }
}
