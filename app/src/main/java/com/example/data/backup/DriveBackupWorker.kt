package com.example.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.data.security.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.util.concurrent.TimeUnit

class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val account = GoogleSignIn.getLastSignedInAccount(applicationContext) ?: return Result.success()
        val preferences = BackupPreferences(applicationContext)
        val passphrase = preferences.passphrase ?: return Result.success()
        if (!preferences.automaticEnabled) return Result.success()
        val userId = PreferenceManager(applicationContext).getActiveUserId() ?: return Result.success()
        return runCatching {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = FinanceRepository(database, PreferenceManager(applicationContext))
            val manager = BackupManager(applicationContext, database, repository)
            val contentHash = manager.contentHash(userId)
            if (contentHash == preferences.lastContentHash) return@runCatching
            val client = DriveBackupClient(applicationContext)
            client.upload(account, manager.createEncryptedBackup(userId, passphrase))
            preferences.lastContentHash = contentHash
            preferences.lastSuccessfulBackup = System.currentTimeMillis()
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private const val WORK_NAME = "sfinance_periodic_drive_backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
