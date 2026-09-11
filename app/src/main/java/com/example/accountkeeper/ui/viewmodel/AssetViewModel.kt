package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetDirection
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetCategoryRepository
import com.example.accountkeeper.data.local.AssetFieldValueDao
import com.example.accountkeeper.data.model.AssetFieldValue
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
    private val assetFieldValueDao: AssetFieldValueDao,
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
        val positiveIds = categories.filter { it.direction == AssetDirection.POSITIVE }.map { it.id }.toSet()
        list.filter { it.assetCategoryId in positiveIds && it.status == AssetStatus.OWNED }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val currentAssetAmount: StateFlow<Double> = positiveAssetAmount
    val negativeAssetAmount: StateFlow<Double> = combine(assets, assetCategories) { list, categories ->
        val negativeIds = categories.filter { it.direction == AssetDirection.NEGATIVE }.map { it.id }.toSet()
        list.filter { it.assetCategoryId in negativeIds && it.status == AssetStatus.OWNED }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    val totalLiabilities: StateFlow<Double> = negativeAssetAmount
    val totalAssets: StateFlow<Double> = currentAssetAmount
    val netAssets: StateFlow<Double> = combine(currentAssetAmount, totalLiabilities) { current, liabilities -> current - liabilities }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)
    suspend fun getAssetById(id: Long): Asset? = assetRepository.getAssetById(id)
    suspend fun getAssetFieldValues(assetId: Long): List<AssetFieldValue> = assetFieldValueDao.getForAsset(assetId)
    fun saveAssetFieldValues(assetId: Long, values: Map<Long, String>) = viewModelScope.launch { assetFieldValueDao.deleteForAsset(assetId); assetFieldValueDao.upsertAll(values.filterValues { it.isNotBlank() }.map { (fieldId, value) -> AssetFieldValue(assetId, fieldId, value) }); autoBackupCoordinator.backupAfterDataChange() }
    fun searchAssets(query: String): Flow<List<Asset>> = assetRepository.searchAssets(query)

    suspend fun availableExpenseTransactions(asset: Asset): List<Transaction> =
        availableExpenseTransactions(asset.categoryId, asset.amount, asset.purchaseDate ?: asset.date, asset.transactionId)

    suspend fun availableExpenseTransactions(
        categoryId: Long?,
        amount: Double,
        purchaseDate: Long,
        selectedTransactionId: Long? = null
    ): List<Transaction> {
        val id = categoryId ?: return emptyList()
        val available = transactionRepository.getAvailableExpenseTransactions(id, amount, purchaseDate, Long.MAX_VALUE)
        val selected = if (selectedTransactionId == null) null else transactionRepository.getTransactionById(selectedTransactionId)
        return (available + listOfNotNull(selected)).distinctBy { it.id }
    }
    fun linkTransaction(assetId: Long, transactionId: Long) = viewModelScope.launch {
        runCatching { assetRepository.linkTransaction(assetId, transactionId) }
            .onFailure { _operationError.value = it.message ?: "操作失败" }
            .onSuccess { autoBackupCoordinator.backupAfterDataChange() }
    }

    fun confirmOwned(assetId: Long) = viewModelScope.launch {
        runCatching { assetRepository.confirmOwned(assetId) }
            .onFailure { _operationError.value = it.message ?: "操作失败" }
            .onSuccess { autoBackupCoordinator.backupAfterDataChange() }
    }
    fun addAsset(asset: Asset) = launchBackup { assetRepository.insertAsset(asset) }
    fun updateAsset(asset: Asset) = launchBackup { assetRepository.updateAsset(asset) }
    suspend fun saveAssetWithFields(asset: Asset, values: Map<Long, String>, isEditing: Boolean): Result<Unit> = runCatching {
        if (isEditing) assetRepository.updateAsset(asset) else assetRepository.insertAsset(asset)
        assetFieldValueDao.deleteForAsset(asset.id)
        assetFieldValueDao.upsertAll(values.filterValues { it.isNotBlank() }.map { (fieldId, value) -> AssetFieldValue(asset.id, fieldId, value) })
        autoBackupCoordinator.backupAfterDataChange()
    }
    fun deleteAsset(asset: Asset) = launchBackup { assetRepository.deleteAsset(asset) }
    fun deleteAssets(list: List<Asset>) = launchBackup { assetRepository.deleteAssets(list) }
    fun deleteAllAssets() = launchBackup { assetRepository.deleteAllAssets() }
    fun toggleAssetCompletion(asset: Asset) = launchBackup { assetRepository.updateAsset(asset.copy(isCompleted = !asset.isCompleted, updatedAt = System.currentTimeMillis())) }

    fun toggleAssetStatus(asset: Asset) = viewModelScope.launch {
        val next = when (asset.status) {
            AssetStatus.NONE -> AssetStatus.IN_PROGRESS
            AssetStatus.IN_PROGRESS -> AssetStatus.OWNED
            AssetStatus.OWNED, AssetStatus.NOT_OWNED -> AssetStatus.LOST
            AssetStatus.LOST, AssetStatus.TEMPORARILY_WITH_ME, AssetStatus.TEMPORARILY_WITH_OTHERS -> AssetStatus.NONE
        }
        if (asset.sourceApprovalId != null && asset.status == AssetStatus.IN_PROGRESS && next == AssetStatus.OWNED && asset.transactionId == null) {
            _operationError.value = "请先关联实际支出交易"
            return@launch
        }
        runCatching { assetRepository.updateAsset(asset.copy(status = next, updatedAt = System.currentTimeMillis())) }
            .onFailure { _operationError.value = it.message ?: "状态更新失败" }
            .onSuccess { autoBackupCoordinator.backupAfterDataChange() }
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
}
