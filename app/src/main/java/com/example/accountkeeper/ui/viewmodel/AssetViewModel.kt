package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 资产状态逻辑说明：
 * 
 * 正资产（如借出的钱）：
 * - 进行中(IN_PROGRESS)：钱借出去了，还没收回 -> 相当于"没有"（不在自己手里）
 * - 完成(COMPLETED)：钱收回来了 -> 相当于"拥有"
 * - OWNED：确定拥有
 * - NOT_OWNED：确定没有
 * 
 * 负资产（如借入的钱/负债）：
 * - 进行中(IN_PROGRESS)：钱借进来了，还没还 -> 相当于"拥有"（在自己手里）
 * - 完成(COMPLETED)：钱还了 -> 相当于"失去"
 * - OWNED：确定拥有
 * - NOT_OWNED：确定没有
 * 
 * 统计逻辑：
 * - 正资产金额 = 正资产类别下，OWNED状态的金额 + 已完成的金额（收回了）
 * - 负资产金额 = 负资产类别下，OWNED状态的金额 + 进行中的金额（借入未还）
 * - 总资产 = 正资产 + 负资产 + 交易结余
 * - 净资产 = 正资产 + 综合（交易结余） - 负债（进行中）
 * - 总负债 = 负资产（进行中的借入款项）
 */
@HiltViewModel
class AssetViewModel @Inject constructor(
    private val assetRepository: AssetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val assets: StateFlow<List<Asset>> = assetRepository.getAllAssets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val categories = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 交易结余 = 收入 - 支出
    val transactionBalance: StateFlow<Double> = transactions.map { transactionList ->
        val income = transactionList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactionList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        income - expense
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    /**
     * 正资产金额计算：
     * - OWNED：确定拥有
     * - 已完成：收回了借出的钱 -> 拥有
     */
    val positiveAssetAmount: StateFlow<Double> = combine(assets, categories) { assetList, categoryList ->
        val positiveCategoryIds = categoryList
            .filter { it.type == TransactionType.ASSET && it.isPositiveAsset }
            .map { it.id }
            .toSet()
        
        assetList.filter { asset ->
            asset.categoryId in positiveCategoryIds
        }.sumOf { asset ->
            when {
                asset.status == AssetStatus.OWNED -> asset.amount
                asset.isCompleted -> asset.amount  // 正资产完成 = 收回 = 拥有
                else -> 0.0
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    /**
     * 负资产金额计算：
     * - OWNED：确定拥有（借入的钱）
     * - 进行中：借入未还 -> 拥有
     */
    val negativeAssetAmount: StateFlow<Double> = combine(assets, categories) { assetList, categoryList ->
        val negativeCategoryIds = categoryList
            .filter { it.type == TransactionType.ASSET && !it.isPositiveAsset }
            .map { it.id }
            .toSet()
        
        assetList.filter { asset ->
            asset.categoryId in negativeCategoryIds
        }.sumOf { asset ->
            when {
                asset.status == AssetStatus.OWNED -> asset.amount
                asset.status == AssetStatus.IN_PROGRESS && !asset.isCompleted -> asset.amount  // 负资产进行中 = 借入未还 = 拥有
                else -> 0.0
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    /**
     * 总负债 = 负资产进行中的金额（借入未还的款项）
     */
    val totalLiabilities: StateFlow<Double> = combine(assets, categories) { assetList, categoryList ->
        val negativeCategoryIds = categoryList
            .filter { it.type == TransactionType.ASSET && !it.isPositiveAsset }
            .map { it.id }
            .toSet()
        
        assetList.filter { asset ->
            asset.categoryId in negativeCategoryIds && 
            asset.status == AssetStatus.IN_PROGRESS && 
            !asset.isCompleted
        }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    /**
     * 总资产 = 正资产 + 负资产 + 交易结余
     */
    val totalAssets: StateFlow<Double> = combine(
        positiveAssetAmount, 
        negativeAssetAmount, 
        transactionBalance
    ) { positive, negative, balance ->
        positive + negative + balance
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    /**
     * 净资产 = 正资产 + 综合（交易结余） - 负债（进行中）
     */
    val netAssets: StateFlow<Double> = combine(
        positiveAssetAmount, 
        transactionBalance, 
        totalLiabilities
    ) { positive, balance, liabilities ->
        positive + balance - liabilities
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    suspend fun getAssetById(id: Long): Asset? {
        return assetRepository.getAssetById(id)
    }

    fun searchAssets(query: String): Flow<List<Asset>> {
        return assetRepository.searchAssets(query)
    }

    fun addAsset(asset: Asset) {
        viewModelScope.launch {
            assetRepository.insertAsset(asset)
        }
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            assetRepository.updateAsset(asset)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            assetRepository.deleteAsset(asset)
        }
    }

    fun deleteAssets(assets: List<Asset>) {
        viewModelScope.launch {
            assetRepository.deleteAssets(assets)
        }
    }

    fun deleteAllAssets() {
        viewModelScope.launch {
            assetRepository.deleteAllAssets()
        }
    }

    fun toggleAssetCompletion(asset: Asset) {
        viewModelScope.launch {
            assetRepository.updateAsset(asset.copy(
                isCompleted = !asset.isCompleted,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    /**
     * Toggle asset status based on category type (positive/negative).
     * Positive asset: OWNED <-> IN_PROGRESS
     * Negative asset: NOT_OWNED <-> IN_PROGRESS
     */
    fun toggleAssetStatus(asset: Asset, isPositiveCategory: Boolean) {
        viewModelScope.launch {
            val newStatus = when (asset.status) {
                // Positive asset cycle: OWNED <-> IN_PROGRESS
                AssetStatus.OWNED -> if (isPositiveCategory) AssetStatus.IN_PROGRESS else asset.status
                AssetStatus.IN_PROGRESS -> if (isPositiveCategory) AssetStatus.OWNED else AssetStatus.NOT_OWNED
                // Negative asset cycle: NOT_OWNED <-> IN_PROGRESS
                AssetStatus.NOT_OWNED -> if (!isPositiveCategory) AssetStatus.IN_PROGRESS else asset.status
                // Legacy statuses - convert to new system
                AssetStatus.TEMPORARILY_WITH_OTHERS -> if (isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.NOT_OWNED
                AssetStatus.TEMPORARILY_WITH_ME -> if (!isPositiveCategory) AssetStatus.IN_PROGRESS else AssetStatus.OWNED
                // NONE - set to appropriate default based on category type
                AssetStatus.NONE -> if (isPositiveCategory) AssetStatus.OWNED else AssetStatus.NOT_OWNED
            }
            
            assetRepository.updateAsset(asset.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}
