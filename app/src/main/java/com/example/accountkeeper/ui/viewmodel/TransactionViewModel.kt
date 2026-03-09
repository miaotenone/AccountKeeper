package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val assetRepository: AssetRepository
) : AndroidViewModel(application) {

    private val backupManager = BackupManager(application)

    // 用于统计计算（保留完整数据）
    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 用于列表分页显示
    val pagedTransactions: Flow<PagingData<Transaction>> = transactionRepository.getAllTransactionsPaged()
        .cachedIn(viewModelScope)

    // ===== 统计计算 StateFlow（缓存优化）=====
    
    // 总收入
    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 总支出
    val totalExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 总余额
    val totalBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 本月起始时间
    private val currentMonthStart: Long
        get() = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    // 本月收入
    val monthlyIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { it.date >= currentMonthStart && it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 本月支出
    val monthlyExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.date >= currentMonthStart && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // 本月余额
    val monthlyBalance: StateFlow<Double> = combine(monthlyIncome, monthlyExpense) { income, expense ->
        income - expense
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // ===== 分页方法 =====
    
    // 搜索分页
    fun searchTransactionsPaged(query: String): Flow<PagingData<Transaction>> =
        transactionRepository.searchTransactionsPaged(query).cachedIn(viewModelScope)

    // 分类时间范围分页
    fun getByCategoryAndTimePaged(categoryId: Long, startTime: Long, endTime: Long): Flow<PagingData<Transaction>> =
        transactionRepository.getByCategoryAndTimePaged(categoryId, startTime, endTime).cachedIn(viewModelScope)

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionRepository.getTransactionById(id)
    }

    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionRepository.searchTransactions(query)
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

    fun deleteTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            transactionRepository.deleteTransactions(transactions)
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
            val assetList = assetRepository.getAllAssets().first()
            val catList = categoryRepository.getAllCategories().first()
            
            // 创建分类 ID -> 名称的映射
            val categoryMap = catList.associate { it.id to it.name }
            
            // Switch to IO since file operations
            withContext(Dispatchers.IO) {
                backupManager.writeZipBackup(
                    transactions = txList,
                    assets = assetList,
                    categoryMap = categoryMap,
                    maxKeep = settings.backupRetentionLimit,
                    isAuto = true
                )
            }
        }
    }
}
