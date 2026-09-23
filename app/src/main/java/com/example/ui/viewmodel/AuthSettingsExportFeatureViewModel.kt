package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.User
import com.example.data.repository.FinanceRepository
import com.example.data.security.PreferenceManager
import kotlinx.coroutines.launch
import java.io.File

class AuthSettingsExportFeatureViewModel(
    private val app: Application,
    private val repository: FinanceRepository,
    private val preferenceManager: PreferenceManager,
    private val emitEvent: (String) -> Unit = {}
) {
    suspend fun login(username: String, password: String): Result<User> {
        val result = repository.login(username, password)
        result.fold(
            onSuccess = { emitEvent("Logged in as ${it.name}") },
            onFailure = { err -> emitEvent(err.message ?: "Login failed") }
        )
        return result
    }

    suspend fun register(name: String, username: String, password: String): Result<User> {
        val result = repository.register(name, username, password)
        result.fold(
            onSuccess = { emitEvent("Account created for ${it.name}") },
            onFailure = { err -> emitEvent(err.message ?: "Registration failed") }
        )
        return result
    }

    fun logout() {
        repository.logout()
        emitEvent("Signed out")
    }

    suspend fun setCurrency(currency: String) {
        preferenceManager.setCurrency(currency)
        emitEvent("Currency set to $currency")
    }

    suspend fun setThemeMode(mode: String) {
        preferenceManager.setThemeMode(mode)
        emitEvent("Theme updated")
    }

    suspend fun setAllowNegativeBalance(allow: Boolean) {
        preferenceManager.setAllowNegativeBalance(allow)
        emitEvent(if (allow) "Negative balances permitted" else "Negative balances strictly prevented")
    }

    suspend fun recalculateBalances(userId: String): Result<Unit> {
        val result = repository.recalculateBalances(userId)
        result.fold(
            onSuccess = { emitEvent("Balances recalculated from transaction history") },
            onFailure = { err -> emitEvent(err.message ?: "Recalculate failed") }
        )
        return result
    }

    suspend fun generatePdfStatement(
        dashboardData: com.example.data.repository.DashboardData,
        transactions: List<com.example.data.local.entities.TransactionEntity>,
        accounts: List<com.example.data.local.entities.Account>,
        categories: List<com.example.data.local.entities.Category>,
        currency: String,
        onPdfReady: (File) -> Unit
    ) {
        val file = com.example.util.PdfExporter.generateStatementPdf(
            context = app,
            dashboardData = dashboardData,
            transactions = transactions,
            accounts = accounts,
            categories = categories.associateBy { it.id },
            currency = currency
        )
        onPdfReady(file)
        emitEvent("PDF Statement generated: ${file.name}")
    }

    suspend fun exportJson(userId: String): String = repository.exportFullJson(userId)
    suspend fun exportCsv(userId: String): String = repository.exportTransactionsCsv(userId)

    suspend fun importJson(userId: String, json: String): Result<Int> {
        val result = repository.importFullJson(userId, json)
        result.fold(
            onSuccess = { count -> emitEvent("Imported $count records from JSON") },
            onFailure = { err -> emitEvent("Import failed: ${err.message}") }
        )
        return result
    }

    suspend fun importCsv(userId: String, csv: String): Result<Int> {
        val result = repository.importTransactionsCsv(userId, csv)
        result.fold(
            onSuccess = { count -> emitEvent("Imported $count transactions from CSV") },
            onFailure = { err -> emitEvent("CSV import failed: ${err.message}") }
        )
        return result
    }
}
