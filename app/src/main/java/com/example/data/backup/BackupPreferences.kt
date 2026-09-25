package com.example.data.backup

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class BackupPreferences(context: Context) {
    private val prefs = runCatching {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "drive_backup_prefs",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { context.getSharedPreferences("drive_backup_prefs_fallback", Context.MODE_PRIVATE) }

    var automaticEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOMATIC, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTOMATIC, value).apply()

    var passphrase: String?
        get() = prefs.getString(KEY_PASSPHRASE, null)
        set(value) = prefs.edit().putString(KEY_PASSPHRASE, value).apply()

    var lastSuccessfulBackup: Long
        get() = prefs.getLong(KEY_LAST_SUCCESS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SUCCESS, value).apply()

    var connectedAccount: String?
        get() = prefs.getString(KEY_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_ACCOUNT, value).apply()

    var lastContentHash: String?
        get() = prefs.getString(KEY_CONTENT_HASH, null)
        set(value) = prefs.edit().putString(KEY_CONTENT_HASH, value).apply()

    fun clearConnection() {
        prefs.edit().remove(KEY_ACCOUNT).remove(KEY_PASSPHRASE).remove(KEY_CONTENT_HASH).putBoolean(KEY_AUTOMATIC, false).apply()
    }

    private companion object {
        const val KEY_AUTOMATIC = "automatic_enabled"
        const val KEY_PASSPHRASE = "passphrase"
        const val KEY_LAST_SUCCESS = "last_successful_backup"
        const val KEY_ACCOUNT = "connected_account"
        const val KEY_CONTENT_HASH = "last_content_hash"
    }
}
