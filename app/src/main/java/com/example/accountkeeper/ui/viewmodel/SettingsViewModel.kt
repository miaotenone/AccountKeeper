package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// No ViewModel import needed anymore
import com.example.accountkeeper.data.repository.AppSettings
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.utils.BackupManager
import com.example.accountkeeper.worker.BackupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    val backupManager = BackupManager(application)

    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateTheme(isDark: Boolean) {
        viewModelScope.launch { settingsRepository.updateTheme(isDark) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch { settingsRepository.updateLanguage(lang) }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch { settingsRepository.updateCurrencySymbol(symbol) }
    }

    fun updateAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoBackup(enabled)
            settingsRepository.updateScheduledBackup(enabled)
            val context = getApplication<Application>()
            if (enabled) BackupWorker.schedule(context, appSettings.value.scheduledBackupInterval)
            else BackupWorker.cancel(context)
        }
    }

    fun updateBackupRetentionLimit(limit: Int) {
        viewModelScope.launch { settingsRepository.updateBackupRetentionLimit(limit) }
    }

    fun updateSwipeDeleteConfirm(requiresConfirm: Boolean) {
        viewModelScope.launch { settingsRepository.updateSwipeDeleteConfirm(requiresConfirm) }
    }

    fun updateScheduledBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateScheduledBackup(enabled)
            settingsRepository.updateAutoBackup(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                val interval = appSettings.value.scheduledBackupInterval
                BackupWorker.schedule(context, interval)
            } else {
                BackupWorker.cancel(context)
            }
        }
    }

    fun updateScheduledBackupInterval(intervalDays: Int) {
        viewModelScope.launch {
            settingsRepository.updateScheduledBackupInterval(intervalDays)
            // 如果定时备份已启用，重新调度
            if (appSettings.value.isScheduledBackupEnabled) {
                val context = getApplication<Application>()
                BackupWorker.schedule(context, intervalDays)
            }
        }
    }
}
