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
import com.example.accountkeeper.data.model.SortType
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.AutoBackupCoordinator
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val assetRepository: AssetRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : AndroidViewModel(application) {

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
        selectedTimeRange, filterCategoryId, filterStartDate, filterEndDate, sortType
    ) { _, _, _, _, _ -> }
        .flatMapLatest {
            val (start, end) = getEffectiveBounds()
            val catId = filterCategoryId.value
            val sort = sortType.value
            if (catId != null) {
                transactionRepository.getFilteredPaged(start, end, catId, sort)
            } else {
                transactionRepository.getByTimeRangePaged(start, end, sort)
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

    fun searchTransactionsPaged(query: String, sortType: SortType = SortType.TIME_DESC): Flow<PagingData<Transaction>> = transactionRepository.searchTransactionsPaged(query, sortType).cachedIn(viewModelScope)
    fun getByCategoryAndTimePaged(categoryId: Long, startTime: Long, endTime: Long): Flow<PagingData<Transaction>> = transactionRepository.getByCategoryAndTimePaged(categoryId, startTime, endTime).cachedIn(viewModelScope)
    suspend fun getTransactionById(id: Long): Transaction? = transactionRepository.getTransactionById(id)
    fun searchTransactions(query: String): Flow<List<Transaction>> = transactionRepository.searchTransactions(query)

    fun addTransaction(transaction: Transaction) = launchBackup { transactionRepository.insertTransaction(transaction) }
    suspend fun insertTransactionSuspend(transaction: Transaction) {
        transactionRepository.insertTransaction(transaction)
        triggerAutoBackup()
    }
    fun updateTransaction(transaction: Transaction) = launchBackup { transactionRepository.updateTransaction(transaction) }
    fun deleteTransaction(transaction: Transaction) = launchBackup { transactionRepository.deleteTransaction(transaction) }
    fun deleteTransactions(transactions: List<Transaction>) = launchBackup { transactionRepository.deleteTransactions(transactions) }
    fun deleteAllTransactions() = launchBackup { transactionRepository.deleteAllTransactions() }

    private fun launchBackup(action: suspend () -> Unit) = viewModelScope.launch {
        action()
        triggerAutoBackup()
    }

    private suspend fun triggerAutoBackup() = autoBackupCoordinator.backupAfterDataChange()
    suspend fun saveTransaction(transaction: Transaction, isEdit: Boolean): Boolean = try {
        if (isEdit) transactionRepository.updateTransaction(transaction) else transactionRepository.insertTransaction(transaction)
        triggerAutoBackup()
        true
    } catch (_: Exception) {
        false
    }
}
