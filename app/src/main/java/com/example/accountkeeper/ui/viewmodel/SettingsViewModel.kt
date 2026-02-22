package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.repository.AppSettings
import com.example.accountkeeper.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
}
