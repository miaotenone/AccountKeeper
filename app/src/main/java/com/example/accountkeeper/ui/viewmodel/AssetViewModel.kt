package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetCategoryRepository
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.AutoBackupCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AssetViewModel @Inject constructor(
    application: Application,
    private val assetRepository: AssetRepository,
    private val assetCategoryRepository: AssetCategoryRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetRepository: BudgetRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : AndroidViewModel(application) {
    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()
    fun clearOperationError() { _operationError.value = null }

    val assets: StateFlow<List<Asset>> = assetRepository.getAllAssets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val assetCategories = assetCategoryRepository.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val transactionBalance: StateFlow<Double> = transactions.map { list -> list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } - list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val positiveAssetAmount: StateFlow<Double> = combine(assets, assetCategories) { list, categories ->
        val physicalIds = categories.filter { it.rootType == AssetRootType.PHYSICAL }.map { it.id }.toSet()
        list.filter { it.assetCategoryId in physicalIds && it.isAssetHeld() }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val currentAssetAmount: StateFlow<Double> = positiveAssetAmount
    val negativeAssetAmount: StateFlow<Double> = combine(assets, assetCategories) { list, categories ->
        val virtualIds = categories.filter { it.rootType == AssetRootType.VIRTUAL }.map { it.id }.toSet()
        list.filter { it.assetCategoryId in virtualIds && it.isLiability() }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val totalLiabilities: StateFlow<Double> = negativeAssetAmount
    val totalAssets: StateFlow<Double> = currentAssetAmount
    val netAssets: StateFlow<Double> = combine(currentAssetAmount, totalLiabilities) { current, liabilities -> current - liabilities }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    suspend fun getAssetById(id: Long): Asset? = assetRepository.getAssetById(id)
    fun searchAssets(query: String): Flow<List<Asset>> = assetRepository.searchAssets(query)

    suspend fun availableExpenseTransactions(asset: Asset): List<Transaction> {
        val categoryId = asset.categoryId ?: return emptyList()
        val start = monthStart(asset.purchaseDate ?: asset.date)
        return transactionRepository.getAvailableExpenseTransactions(categoryId, start, monthEnd(start))
    }

    fun confirmOwned(assetId: Long, transactionId: Long) = viewModelScope.launch {
        runCatching { assetRepository.confirmOwned(assetId, transactionId) }
            .onFailure { _operationError.value = it.message ?: "操作失败" }
            .onSuccess { autoBackupCoordinator.backupAfterDataChange() }
    }
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
            AssetStatus.NOT_OWNED -> if (isPositiveCategory) AssetStatus.LOST else AssetStatus.IN_PROGRESS
            AssetStatus.IN_PROGRESS -> if (isPositiveCategory) AssetStatus.OWNED else AssetStatus.NOT_OWNED
            AssetStatus.LOST -> AssetStatus.NONE
            AssetStatus.TEMPORARILY_WITH_OTHERS -> if (isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.NOT_OWNED
            AssetStatus.TEMPORARILY_WITH_ME -> if (isPositiveCategory) AssetStatus.OWNED else AssetStatus.IN_PROGRESS
        }
        assetRepository.updateAsset(asset.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }

    private fun launchBackup(action: suspend () -> Unit) = viewModelScope.launch { action(); autoBackupCoordinator.backupAfterDataChange() }
    private fun monthStart(time: Long): Long = java.util.Calendar.getInstance().apply {
        timeInMillis = time
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun monthEnd(start: Long): Long = java.util.Calendar.getInstance().apply { timeInMillis = start; add(java.util.Calendar.MONTH, 1) }.timeInMillis
    private fun Asset.isAssetHeld(): Boolean = !isCompleted && (status == AssetStatus.OWNED || status == AssetStatus.TEMPORARILY_WITH_ME)
    private fun Asset.isLiability(): Boolean = !isCompleted && (status == AssetStatus.NOT_OWNED || status == AssetStatus.TEMPORARILY_WITH_OTHERS || status == AssetStatus.IN_PROGRESS)
}
