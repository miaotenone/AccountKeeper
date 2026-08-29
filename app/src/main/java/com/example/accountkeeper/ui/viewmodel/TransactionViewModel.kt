package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TimeRange { MONTH, YEAR, ALL }
enum class SortType { TIME_DESC, TIME_ASC, AMOUNT_DESC, AMOUNT_ASC }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val assetRepository: AssetRepository
) : AndroidViewModel(application) {
    private val backupManager = BackupManager(application)

    // Full transaction list for DataManagement/backup usage
    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    val filterCategoryId = MutableStateFlow<Long?>(null)
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)
    val sortType = MutableStateFlow(SortType.TIME_DESC)

    private fun getTimeRangeBounds(range: TimeRange): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis + 1
        val start = when (range) {
            TimeRange.MONTH -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TimeRange.YEAR -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TimeRange.ALL -> 0L
        }
        return start to end
    }

    private fun getEffectiveBounds(): Pair<Long, Long> {
        val fs = filterStartDate.value
        val fe = filterEndDate.value
        if (fs != null && fe != null) return fs to (fe + 24 * 60 * 60 * 1000 - 1)
        if (fs != null) return fs to System.currentTimeMillis()
        if (fe != null) return 0L to (fe + 24 * 60 * 60 * 1000 - 1)
        return getTimeRangeBounds(selectedTimeRange.value)
    }

    val pagedTransactions: Flow<PagingData<Transaction>> = combine(
        selectedTimeRange, filterCategoryId, filterStartDate, filterEndDate
    ) { _, _, _, _ -> }
        .flatMapLatest {
            val (start, end) = getEffectiveBounds()
            val catId = filterCategoryId.value
            if (catId != null) {
                transactionRepository.getFilteredPaged(start, end, catId)
            } else {
                transactionRepository.getByTimeRangePaged(start, end)
            }
        }
        .cachedIn(viewModelScope)

    val monthlyIncome: StateFlow<Double> = selectedTimeRange.flatMapLatest { range ->
        val (start, end) = getTimeRangeBounds(range)
        transactionRepository.getIncomeBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyExpense: StateFlow<Double> = selectedTimeRange.flatMapLatest { range ->
        val (start, end) = getTimeRangeBounds(range)
        transactionRepository.getExpenseBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyBalance: StateFlow<Double> = combine(monthlyIncome, monthlyExpense) { income, expense -> income - expense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Keep total stats for all-time (used in some contexts)
    val totalIncome: StateFlow<Double> = transactionRepository.getIncomeBetween(0L, Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalExpense: StateFlow<Double> = transactionRepository.getExpenseBetween(0L, Long.MAX_VALUE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense -> income - expense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun searchTransactionsPaged(query: String): Flow<PagingData<Transaction>> = transactionRepository.searchTransactionsPaged(query).cachedIn(viewModelScope)
    fun getByCategoryAndTimePaged(categoryId: Long, startTime: Long, endTime: Long): Flow<PagingData<Transaction>> = transactionRepository.getByCategoryAndTimePaged(categoryId, startTime, endTime).cachedIn(viewModelScope)
    suspend fun getTransactionById(id: Long): Transaction? = transactionRepository.getTransactionById(id)
    fun searchTransactions(query: String): Flow<List<Transaction>> = transactionRepository.searchTransactions(query)

    fun addTransaction(transaction: Transaction) = viewModelScope.launch { transactionRepository.insertTransaction(transaction); triggerAutoBackup() }
    suspend fun insertTransactionSuspend(transaction: Transaction) = transactionRepository.insertTransaction(transaction)
    fun updateTransaction(transaction: Transaction) = viewModelScope.launch { transactionRepository.updateTransaction(transaction); triggerAutoBackup() }
    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch { transactionRepository.deleteTransaction(transaction); triggerAutoBackup() }
    fun deleteTransactions(transactions: List<Transaction>) = viewModelScope.launch { transactionRepository.deleteTransactions(transactions); triggerAutoBackup() }
    fun deleteAllTransactions() = viewModelScope.launch { transactionRepository.deleteAllTransactions(); triggerAutoBackup() }

    private suspend fun triggerAutoBackup() {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isAutoBackupEnabled) return

        val txList = transactionRepository.getAllTransactions().first()
        val assetList = assetRepository.getAllAssets().first()
        val catList = categoryRepository.getAllCategories().first()
        val categoryMap = catList.associate { it.id to it.name }
        val budgets = budgetRepository.getAll().first()

        withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!backupManager.hasBackupChain()) {
                backupManager.createBaseBackup(txList, assetList, categoryMap, budgets = budgets)
                return@withContext
            }

            val latestState = backupManager.restoreToStep(-1)
            if (!latestState.success) {
                backupManager.createBaseBackup(txList, assetList, categoryMap, budgets = budgets)
                return@withContext
            }

            val previousTransactions = latestState.transactions.map { data ->
                Transaction(
                    id = data.id,
                    date = data.date,
                    type = if (data.type.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = data.amount,
                    note = data.note,
                    categoryId = catList.find { it.name == data.categoryName }?.id ?: 0L
                )
            }
            val previousAssets = latestState.assets.map { data ->
                Asset(
                    id = data.id,
                    date = data.date,
                    amount = data.amount,
                    status = try { AssetStatus.valueOf(data.status) } catch (_: Exception) { AssetStatus.NONE },
                    categoryId = catList.find { it.name == data.categoryName }?.id,
                    targetPerson = data.targetPerson,
                    targetAccount = data.targetAccount,
                    note = data.note,
                    isCompleted = data.isCompleted,
                    attachments = AttachmentConverter.toJson(data.attachments),
                    createdAt = data.createdAt,
                    updatedAt = data.updatedAt
                )
            }
            val previousBudgets = latestState.budgets.map { data ->
                Budget(
                    monthKey = data.monthKey,
                    categoryId = data.categoryName?.let { name -> catList.firstOrNull { it.name == name }?.id },
                    amount = data.amount,
                    createdAt = data.createdAt,
                    updatedAt = data.updatedAt
                )
            }

            backupManager.createDeltaBackup(
                previousTransactions = previousTransactions,
                previousAssets = previousAssets,
                currentTransactions = txList,
                currentAssets = assetList,
                categoryMap = categoryMap,
                budgets = budgets,
                previousBudgets = previousBudgets,
                maxKeep = settings.backupRetentionLimit
            )
        }
    }
    suspend fun saveTransaction(transaction: Transaction, isEdit: Boolean): Boolean = try {
        if (isEdit) transactionRepository.updateTransaction(transaction) else transactionRepository.insertTransaction(transaction)
        triggerAutoBackup()
        true
    } catch (_: Exception) {
        false
    }
}
