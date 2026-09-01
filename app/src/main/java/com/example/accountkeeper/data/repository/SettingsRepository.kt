package com.example.accountkeeper.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val isDarkMode: Boolean = true,
    val language: String = "zh", // "zh" default for Chinese
    val currencySymbol: String = "¥",
    val isAutoBackupEnabled: Boolean = false,
    val backupRetentionLimit: Int = 15,
    val swipeDeleteRequiresConfirm: Boolean = true,  // 左滑是否直接弹出确认对话框
    val isScheduledBackupEnabled: Boolean = false,   // 是否启用定时备份
    val scheduledBackupInterval: Int = 24            // 定时备份间隔（小时）
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val BACKUP_LIMIT = intPreferencesKey("backup_limit")
        val SWIPE_DELETE_CONFIRM = booleanPreferencesKey("swipe_delete_confirm")
        val SCHEDULED_BACKUP = booleanPreferencesKey("scheduled_backup")
        val SCHEDULED_BACKUP_INTERVAL = intPreferencesKey("scheduled_backup_interval")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: true,
            language = preferences[PreferencesKeys.LANGUAGE] ?: "zh",
            currencySymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "¥",
            isAutoBackupEnabled = preferences[PreferencesKeys.AUTO_BACKUP] ?: false,
            backupRetentionLimit = preferences[PreferencesKeys.BACKUP_LIMIT] ?: 15,
            swipeDeleteRequiresConfirm = preferences[PreferencesKeys.SWIPE_DELETE_CONFIRM] ?: true,
            isScheduledBackupEnabled = preferences[PreferencesKeys.SCHEDULED_BACKUP] ?: false,
            scheduledBackupInterval = preferences[PreferencesKeys.SCHEDULED_BACKUP_INTERVAL] ?: 24
        )
    }

    suspend fun updateTheme(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun updateCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun updateAutoBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_BACKUP] = enabled
        }
    }

    suspend fun updateBackupRetentionLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKUP_LIMIT] = limit
        }
    }

    suspend fun updateSwipeDeleteConfirm(requiresConfirm: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SWIPE_DELETE_CONFIRM] = requiresConfirm
        }
    }

    suspend fun updateScheduledBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCHEDULED_BACKUP] = enabled
        }
    }

    suspend fun restore(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = settings.isDarkMode
            preferences[PreferencesKeys.LANGUAGE] = settings.language
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = settings.currencySymbol
            preferences[PreferencesKeys.AUTO_BACKUP] = settings.isAutoBackupEnabled
            preferences[PreferencesKeys.BACKUP_LIMIT] = settings.backupRetentionLimit
            preferences[PreferencesKeys.SWIPE_DELETE_CONFIRM] = settings.swipeDeleteRequiresConfirm
            preferences[PreferencesKeys.SCHEDULED_BACKUP] = settings.isScheduledBackupEnabled
            preferences[PreferencesKeys.SCHEDULED_BACKUP_INTERVAL] = settings.scheduledBackupInterval
        }
    }

    suspend fun updateScheduledBackupInterval(intervalHours: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCHEDULED_BACKUP_INTERVAL] = intervalHours
        }
    }
}
