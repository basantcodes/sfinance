package com.example.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: FinanceRepository,
    private val driveClient: DriveBackupClient = DriveBackupClient(context)
) {
    fun readSummary(encryptedBackup: ByteArray): BackupSummary {
        val envelope = JSONObject(String(encryptedBackup, Charsets.UTF_8))
        return BackupSummary(
            createdAt = envelope.getLong("createdAt"),
            appVersion = envelope.getString("appVersion"),
            formatVersion = envelope.getInt("formatVersion")
        )
    }

    suspend fun createEncryptedBackup(userId: String, passphrase: CharSequence): ByteArray = withContext(Dispatchers.IO) {
        require(passphrase.length >= 12) { "Backup passphrase must be at least 12 characters" }
        val payload = repository.exportFullJson(userId).toByteArray(Charsets.UTF_8)
        val salt = BackupCrypto.newSalt()
        val encrypted = BackupCrypto.encrypt(payload, passphrase, salt)
        JSONObject().apply {
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("databaseVersion", 5)
            put("createdAt", System.currentTimeMillis())
            put("payload", BackupCrypto.encode(encrypted.bytes))
            put("iv", BackupCrypto.encode(encrypted.iv))
            put("salt", BackupCrypto.encode(salt))
        }.toString().toByteArray(Charsets.UTF_8)
    }

    suspend fun contentHash(userId: String): String = withContext(Dispatchers.IO) {
        val payload = repository.exportFullJson(userId).toByteArray(Charsets.UTF_8)
        MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
    }

    suspend fun restoreEncryptedBackup(
        userId: String,
        encryptedBackup: ByteArray,
        passphrase: CharSequence
    ): BackupSummary = withContext(Dispatchers.IO) {
        val envelope = JSONObject(String(encryptedBackup, Charsets.UTF_8))
        val formatVersion = envelope.optInt("formatVersion", -1)
        require(formatVersion == BACKUP_FORMAT_VERSION) { "Unsupported backup format" }
        val databaseVersion = envelope.optInt("databaseVersion", -1)
        require(databaseVersion in 1..5) { "Unsupported database version" }
        val payload = BackupCrypto.decrypt(
            BackupCrypto.decode(envelope.getString("payload")),
            passphrase,
            BackupCrypto.decode(envelope.getString("salt")),
            BackupCrypto.decode(envelope.getString("iv"))
        )
        val payloadJson = String(payload, Charsets.UTF_8)
        require(JSONObject(payloadJson).optInt("version", -1) in 1..2) { "Invalid backup payload" }
        database.replaceWithBackup(repository, userId, payloadJson)
        BackupSummary(
            createdAt = envelope.getLong("createdAt"),
            appVersion = envelope.getString("appVersion"),
            formatVersion = formatVersion
        )
    }
}

private suspend fun AppDatabase.replaceWithBackup(
    repository: FinanceRepository,
    userId: String,
    payload: String
) {
    withTransaction {
        accountDao().deleteAll()
        categoryDao().deleteAll()
        transactionDao().deleteAll()
        loanInterestDao().deleteAll()
        loanDao().deleteAll()
        budgetDao().deleteAll()
        wishlistDao().deleteAll()
        journalDao().deleteAll()
        check(repository.importFullJson(userId, payload).isSuccess) { "Backup data could not be restored" }
    }
}
