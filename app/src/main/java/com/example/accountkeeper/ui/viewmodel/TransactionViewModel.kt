package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// No ViewModel import needed anymore
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)

    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionRepository.getTransactionById(id)
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
            triggerAutoBackup()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            triggerAutoBackup()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            triggerAutoBackup()
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions()
            triggerAutoBackup()
        }
    }
    
    private suspend fun triggerAutoBackup() {
        val settings = settingsRepository.settingsFlow.first()
        if (settings.isAutoBackupEnabled) {
            val txList = transactionRepository.getAllTransactions().first()
            val catList = categoryRepository.getAllCategories().first()
            
            val safeCsvSequence = sequence {
                yield("ID,Date,Type,Amount,Category,Note")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                for (tx in txList) {
                    val categoryName = catList.find { it.id == tx.categoryId }?.name ?: "Other"
                    val typeString = if (tx.type == TransactionType.INCOME) "Income" else "Expense"
                    val safeNote = tx.note.replace("\"", "\"\"")
                    yield("${tx.id},${dateFormat.format(Date(tx.date))},${typeString},${tx.amount},${categoryName},\"${safeNote}\"")
                }
            }
            // Switch to IO since file operations
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                backupManager.writeNewBackup(safeCsvSequence, settings.backupRetentionLimit)
            }
        }
    }
}
