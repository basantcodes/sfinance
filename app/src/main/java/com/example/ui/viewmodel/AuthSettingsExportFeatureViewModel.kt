package com.example.ui.viewmodel

import android.app.Application
import com.example.R
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
            onSuccess = { emitEvent(app.getString(R.string.logged_in_as, it.name)) },
            onFailure = { err -> emitEvent(err.message ?: app.getString(R.string.login_failed)) }
        )
        return result
    }

    suspend fun register(name: String, username: String, password: String): Result<User> {
        val result = repository.register(name, username, password)
        result.fold(
            onSuccess = { emitEvent(app.getString(R.string.account_created_for, it.name)) },
            onFailure = { err -> emitEvent(err.message ?: app.getString(R.string.registration_failed)) }
        )
        return result
    }

    fun logout() {
        repository.logout()
        emitEvent(app.getString(R.string.signed_out))
    }

    suspend fun setCurrency(currency: String) {
        preferenceManager.setCurrency(currency)
        emitEvent(app.getString(R.string.currency_set, currency))
    }

    suspend fun setThemeMode(mode: String) {
        preferenceManager.setThemeMode(mode)
        emitEvent(app.getString(R.string.theme_updated))
    }

    suspend fun setAllowNegativeBalance(allow: Boolean) {
        preferenceManager.setAllowNegativeBalance(allow)
        emitEvent(app.getString(if (allow) R.string.negative_balances_permitted else R.string.negative_balances_prevented))
    }

    suspend fun setLanguage(language: String) {
        preferenceManager.setLanguage(language)
    }

    suspend fun recalculateBalances(userId: String): Result<Unit> {
        val result = repository.recalculateBalances(userId)
        result.fold(
            onSuccess = { emitEvent(app.getString(R.string.balances_recalculated)) },
            onFailure = { err -> emitEvent(err.message ?: app.getString(R.string.recalculate_failed)) }
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
        emitEvent(app.getString(R.string.pdf_generated, file.name))
    }

    suspend fun exportJson(userId: String): String = repository.exportFullJson(userId)
    suspend fun exportCsv(userId: String): String = repository.exportTransactionsCsv(userId)

    suspend fun importJson(userId: String, json: String): Result<Int> {
        val result = repository.importFullJson(userId, json)
        result.fold(
            onSuccess = { count -> emitEvent(app.getString(R.string.records_imported, count)) },
            onFailure = { err -> emitEvent(app.getString(R.string.import_failed, err.message ?: "")) }
        )
        return result
    }

    suspend fun importCsv(userId: String, csv: String): Result<Int> {
        val result = repository.importTransactionsCsv(userId, csv)
        result.fold(
            onSuccess = { count -> emitEvent(app.getString(R.string.transactions_imported, count)) },
            onFailure = { err -> emitEvent(app.getString(R.string.csv_import_failed, err.message ?: "")) }
        )
        return result
    }
}
