package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AssetViewModel @Inject constructor(
    application: Application,
    private val assetRepository: AssetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetRepository: BudgetRepository
) : AndroidViewModel(application) {
    private val backupManager = BackupManager(application)
    val assets: StateFlow<List<Asset>> = assetRepository.getAllAssets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val categories = categoryRepository.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun currentPositive(asset: Asset, categoryList: List<com.example.accountkeeper.data.model.Category>): Boolean {
        val category = categoryList.firstOrNull { it.id == asset.categoryId }
        return category?.type == TransactionType.ASSET && category.isPositiveAsset && (asset.status == AssetStatus.OWNED || asset.isCompleted)
    }

    val transactionBalance: StateFlow<Double> = transactions.map { list -> list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } - list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val positiveAssetAmount: StateFlow<Double> = combine(assets, categories) { list, cats -> list.filter { currentPositive(it, cats) }.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val currentAssetAmount: StateFlow<Double> = positiveAssetAmount
    val negativeAssetAmount: StateFlow<Double> = combine(assets, categories) { list, cats -> val ids = cats.filter { it.type == TransactionType.ASSET && !it.isPositiveAsset }.map { it.id }.toSet(); list.filter { it.categoryId in ids }.sumOf { if (it.status == AssetStatus.OWNED || (it.status == AssetStatus.IN_PROGRESS && !it.isCompleted)) it.amount else 0.0 } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalLiabilities: StateFlow<Double> = combine(assets, categories) { list, cats -> val ids = cats.filter { it.type == TransactionType.ASSET && !it.isPositiveAsset }.map { it.id }.toSet(); list.filter { it.categoryId in ids && it.status == AssetStatus.IN_PROGRESS && !it.isCompleted }.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalAssets: StateFlow<Double> = currentAssetAmount
    val netAssets: StateFlow<Double> = combine(currentAssetAmount, totalLiabilities) { current, liabilities -> current - liabilities }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    suspend fun getAssetById(id: Long): Asset? = assetRepository.getAssetById(id)
    fun searchAssets(query: String): Flow<List<Asset>> = assetRepository.searchAssets(query)
    fun addAsset(asset: Asset) = launchBackup { assetRepository.insertAsset(asset) }
    fun updateAsset(asset: Asset) = launchBackup { assetRepository.updateAsset(asset) }
    fun deleteAsset(asset: Asset) = launchBackup { assetRepository.deleteAsset(asset) }
    fun deleteAssets(list: List<Asset>) = launchBackup { assetRepository.deleteAssets(list) }
    fun deleteAllAssets() = launchBackup { assetRepository.deleteAllAssets() }
    fun toggleAssetCompletion(asset: Asset) = launchBackup { assetRepository.updateAsset(asset.copy(isCompleted = !asset.isCompleted, updatedAt = System.currentTimeMillis())) }
    fun toggleAssetStatus(asset: Asset, isPositiveCategory: Boolean) = launchBackup {
        val newStatus = when (asset.status) {
            AssetStatus.NONE -> if (isPositiveCategory) AssetStatus.OWNED else AssetStatus.NOT_OWNED
            AssetStatus.OWNED -> if (isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.LOST
            AssetStatus.NOT_OWNED -> if (!isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.LOST
            AssetStatus.IN_PROGRESS -> if (isPositiveCategory) AssetStatus.OWNED else AssetStatus.NOT_OWNED
            AssetStatus.LOST -> AssetStatus.NONE
            AssetStatus.TEMPORARILY_WITH_OTHERS -> if (isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.NOT_OWNED
            AssetStatus.TEMPORARILY_WITH_ME -> if (!isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.OWNED
        }
        assetRepository.updateAsset(asset.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }
    private fun launchBackup(action: suspend () -> Unit) = viewModelScope.launch { action(); triggerAutoBackup() }

    private suspend fun triggerAutoBackup() {
        val settings = settingsRepository.settingsFlow.first(); if (!settings.isAutoBackupEnabled) return
        val txList = transactionRepository.getAllTransactions().first(); val assetList = assetRepository.getAllAssets().first(); val catList = categoryRepository.getAllCategories().first(); val categoryMap = catList.associate { it.id to it.name }; val budgets = budgetRepository.getAll().first()
        withContext(Dispatchers.IO) {
            if (!backupManager.hasBackupChain()) backupManager.createBaseBackup(txList, assetList, categoryMap, budgets = budgets) else {
                val latest = backupManager.restoreToStep(-1)
                if (latest.success) {
                    val previousTransactions = latest.transactions.map { data -> Transaction(id = data.id, type = if (data.type == "Income") TransactionType.INCOME else TransactionType.EXPENSE, amount = data.amount, date = data.date, categoryId = catList.firstOrNull { c -> c.name == data.categoryName }?.id, note = data.note) }
                    val previousAssets = latest.assets.map { data -> Asset(id = data.id, date = data.date, amount = data.amount, status = try { AssetStatus.valueOf(data.status) } catch (_: Exception) { AssetStatus.NONE }, categoryId = catList.firstOrNull { c -> c.name == data.categoryName }?.id, targetPerson = data.targetPerson, targetAccount = data.targetAccount, note = data.note, isCompleted = data.isCompleted, attachments = AttachmentConverter.toJson(data.attachments), createdAt = data.createdAt, updatedAt = data.updatedAt) }
                    val previousBudgets = latest.budgets.map { data -> com.example.accountkeeper.data.model.Budget(monthKey = data.monthKey, categoryId = data.categoryName?.let { name -> catList.firstOrNull { it.name == name }?.id }, amount = data.amount, createdAt = data.createdAt, updatedAt = data.updatedAt) }
                    backupManager.createDeltaBackup(previousTransactions, previousAssets, txList, assetList, categoryMap, budgets = budgets, previousBudgets = previousBudgets, maxKeep = settings.backupRetentionLimit)
                } else backupManager.createBaseBackup(txList, assetList, categoryMap, budgets = budgets)
            }
        }
    }
}
