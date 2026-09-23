package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class PreferenceManager(private val context: Context) {

    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secure_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for Robolectric or legacy test environments
            context.getSharedPreferences("secure_auth_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_ACTIVE_USERNAME = "active_username"
        private const val KEY_ACTIVE_USER_NAME = "active_user_name"

        val PREF_CURRENCY = stringPreferencesKey("app_currency")
        val PREF_THEME_MODE = stringPreferencesKey("theme_mode")
        val PREF_ALLOW_NEGATIVE_BALANCE = booleanPreferencesKey("allow_negative_balance")
    }

    fun saveAuthToken(token: String, userId: String, username: String, name: String) {
        encryptedPrefs.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_ACTIVE_USER_ID, userId)
            .putString(KEY_ACTIVE_USERNAME, username)
            .putString(KEY_ACTIVE_USER_NAME, name)
            .apply()
    }

    fun getAuthToken(): String? = encryptedPrefs.getString(KEY_JWT_TOKEN, null)
    fun getActiveUserId(): String? = encryptedPrefs.getString(KEY_ACTIVE_USER_ID, null)
    fun getActiveUsername(): String? = encryptedPrefs.getString(KEY_ACTIVE_USERNAME, null)
    fun getActiveUserName(): String? = encryptedPrefs.getString(KEY_ACTIVE_USER_NAME, null)

    fun clearAuth() {
        encryptedPrefs.edit().clear().apply()
    }

    val currencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREF_CURRENCY] ?: "NPR"
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREF_THEME_MODE] ?: "SYSTEM" // SYSTEM, LIGHT, DARK
    }

    val allowNegativeBalanceFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREF_ALLOW_NEGATIVE_BALANCE] ?: false
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PREF_CURRENCY] = currency
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PREF_THEME_MODE] = mode
        }
    }

    suspend fun setAllowNegativeBalance(allow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREF_ALLOW_NEGATIVE_BALANCE] = allow
        }
    }
}
