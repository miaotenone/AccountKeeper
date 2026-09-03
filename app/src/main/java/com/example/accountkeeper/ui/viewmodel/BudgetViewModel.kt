package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetPeriodType
import com.example.accountkeeper.data.repository.BudgetApprovalRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.utils.AutoBackupCoordinator
import com.example.accountkeeper.utils.BudgetData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ApprovalSubmitState {
    data object Idle : ApprovalSubmitState()
    data object Submitting : ApprovalSubmitState()
    data class Success(val id: Long) : ApprovalSubmitState()
    data class Error(val message: String) : ApprovalSubmitState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val approvalRepository: BudgetApprovalRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(monthKey(System.currentTimeMillis()))
    val monthKey: StateFlow<String> = selectedMonth

    val selectedPeriodType = MutableStateFlow(BudgetPeriodType.MONTHLY)

    val budgets: StateFlow<List<Budget>> = selectedMonth.flatMapLatest { key ->
        selectedPeriodType.flatMapLatest { period -> repository.getByMonthAndPeriod(key, period.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyExpense: StateFlow<Double> = selectedMonth.flatMapLatest { key ->
        selectedPeriodType.flatMapLatest { period ->
            val (start, end) = periodRange(key, period)
            repository.getMonthlyExpense(start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val uncategorizedExpense: StateFlow<Double> = selectedMonth.flatMapLatest { key ->
        selectedPeriodType.flatMapLatest { period ->
            val (start, end) = periodRange(key, period)
            repository.getUncategorizedExpense(start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val expenseCategoryIds: StateFlow<List<Long>> = selectedMonth.flatMapLatest { key ->
        selectedPeriodType.flatMapLatest { period ->
            val (start, end) = periodRange(key, period)
            repository.getExpenseCategoryIds(start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryExpenses: StateFlow<Map<Long, Double>> = selectedMonth.flatMapLatest { key ->
        selectedPeriodType.flatMapLatest { period ->
            val (start, end) = periodRange(key, period)
            repository.getAllCategoryExpenses(start, end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val approvalRequests: StateFlow<List<BudgetApprovalRequest>> = approvalRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val submitState = MutableStateFlow<ApprovalSubmitState>(ApprovalSubmitState.Idle)

    init { ensureMonth(selectedMonth.value) }

    fun previousPeriod() {
        val period = selectedPeriodType.value
        val newKey = when (period) {
            BudgetPeriodType.MONTHLY -> shiftMonth(selectedMonth.value, -1)
            BudgetPeriodType.SEMI_ANNUAL -> shiftMonth(selectedMonth.value, -6)
            BudgetPeriodType.ANNUAL -> shiftMonth(selectedMonth.value, -12)
        }
        selectMonth(newKey)
    }

    fun nextPeriod() {
        val period = selectedPeriodType.value
        val newKey = when (period) {
            BudgetPeriodType.MONTHLY -> shiftMonth(selectedMonth.value, 1)
            BudgetPeriodType.SEMI_ANNUAL -> shiftMonth(selectedMonth.value, 6)
            BudgetPeriodType.ANNUAL -> shiftMonth(selectedMonth.value, 12)
        }
        selectMonth(newKey)
    }

    fun setPeriodType(type: BudgetPeriodType) { selectedPeriodType.value = type }

    fun save(budget: Budget) = viewModelScope.launch {
        try {
            val normalized = budget.copy(
                monthKey = selectedMonth.value,
                periodType = selectedPeriodType.value.name,
                amount = budget.amount.coerceAtLeast(0.0),
                updatedAt = System.currentTimeMillis()
            )
            if (normalized.id == 0L) repository.insert(normalized) else repository.update(normalized)
            repository.markMonthInitialized(normalized.monthKey)
            autoBackupCoordinator.backupAfterDataChange()
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
    }

    fun delete(budget: Budget) = viewModelScope.launch {
        repository.delete(budget)
        repository.markMonthInitialized(budget.monthKey)
        autoBackupCoordinator.backupAfterDataChange()
    }

    suspend fun importBudgets(imported: List<BudgetData>, categories: List<com.example.accountkeeper.data.model.Category>) {
        imported.forEach { data ->
            try {
                val categoryId = data.categoryName?.let { name ->
                    categories.firstOrNull {
                        it.name.equals(name, ignoreCase = true) &&
                            it.type == com.example.accountkeeper.data.model.TransactionType.EXPENSE
                    }?.id
                }
                if (data.categoryName != null && categoryId == null) return@forEach
                val existing = repository.getByMonthList(data.monthKey).firstOrNull { it.categoryId == categoryId }
                val budget = Budget(
                    monthKey = data.monthKey,
                    categoryId = categoryId,
                    amount = data.amount.coerceAtLeast(0.0),
                    createdAt = data.createdAt,
                    updatedAt = data.updatedAt
                )
                if (existing == null) repository.insert(budget)
                else repository.update(budget.copy(id = existing.id, createdAt = existing.createdAt))
                repository.markMonthInitialized(data.monthKey)
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        }
    }

    fun expenseFor(categoryId: Long, start: Long, end: Long): StateFlow<Double> =
        repository.getCategoryExpense(categoryId, start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun expenseForPeriod(month: String, periodType: BudgetPeriodType): StateFlow<Double> {
        val (start, end) = periodRange(month, periodType)
        return repository.getMonthlyExpense(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    fun budgetFor(month: String, categoryId: Long?): StateFlow<Budget?> =
        repository.getByMonth(month)
            .map { list -> list.firstOrNull { it.categoryId == categoryId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun budgetFor(month: String, periodType: BudgetPeriodType, categoryId: Long?): StateFlow<Budget?> =
        repository.getByMonthAndPeriod(month, periodType.name)
            .map { list -> list.firstOrNull { it.categoryId == categoryId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun expenseForMonth(month: String): StateFlow<Double> {
        val (start, end) = monthRange(month)
        return repository.getMonthlyExpense(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    fun submitApproval(request: BudgetApprovalRequest, replacingId: Long? = null) {
        submitState.value = ApprovalSubmitState.Submitting
        viewModelScope.launch {
            try {
                val id = if (replacingId == null) {
                    approvalRepository.submit(request)
                } else {
                    approvalRepository.resubmit(request.copy(id = replacingId))
                    replacingId
                }
                autoBackupCoordinator.backupAfterDataChange()
                submitState.value = ApprovalSubmitState.Success(id)
            } catch (e: Exception) {
                submitState.value = ApprovalSubmitState.Error(e.message ?: "提交失败")
            }
        }
    }

    fun withdrawApproval(id: Long) = launchBackup { approvalRepository.withdraw(id) }

    fun approveApproval(id: Long, note: String = "") = launchBackup { approvalRepository.approve(id, note) }

    fun rejectApproval(id: Long, note: String = "") = launchBackup { approvalRepository.reject(id, note) }

    private fun launchBackup(action: suspend () -> Unit) = viewModelScope.launch {
        action()
        autoBackupCoordinator.backupAfterDataChange()
    }

    private fun selectMonth(key: String) {
        selectedMonth.value = key
        ensureMonth(key)
    }

    private fun ensureMonth(key: String) = viewModelScope.launch {
        val initialized = repository.initializeMonthIfNeeded(key, shiftMonth(key, -1))
    }

    companion object {
        fun monthKey(time: Long): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(time))

        fun monthRange(key: String): Pair<Long, Long> {
            val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date()
            val start = Calendar.getInstance().apply {
                time = parsed
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            return start.timeInMillis to end.timeInMillis
        }

        fun periodRange(key: String, period: BudgetPeriodType): Pair<Long, Long> {
            val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date()
            val start = Calendar.getInstance().apply {
                time = parsed
                when (period) {
                    BudgetPeriodType.MONTHLY -> set(Calendar.DAY_OF_MONTH, 1)
                    BudgetPeriodType.SEMI_ANNUAL -> {
                        val month = get(Calendar.MONTH)
                        set(Calendar.MONTH, (month / 6) * 6)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    BudgetPeriodType.ANNUAL -> {
                        set(Calendar.MONTH, Calendar.JANUARY)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = (start.clone() as Calendar).apply {
                when (period) {
                    BudgetPeriodType.MONTHLY -> add(Calendar.MONTH, 1)
                    BudgetPeriodType.SEMI_ANNUAL -> add(Calendar.MONTH, 6)
                    BudgetPeriodType.ANNUAL -> add(Calendar.YEAR, 1)
                }
            }
            return start.timeInMillis to end.timeInMillis
        }

        fun periodLabel(key: String, period: BudgetPeriodType): String {
            val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date()
            val cal = Calendar.getInstance().apply { time = parsed }
            return when (period) {
                BudgetPeriodType.MONTHLY -> SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
                BudgetPeriodType.SEMI_ANNUAL -> {
                    val month = cal.get(Calendar.MONTH)
                    val half = if (month < 6) "H1" else "H2"
                    "${cal.get(Calendar.YEAR)} $half"
                }
                BudgetPeriodType.ANNUAL -> "${cal.get(Calendar.YEAR)}"
            }
        }

        private fun shiftMonth(key: String, amount: Int): String {
            val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date()
            return Calendar.getInstance().apply {
                time = parsed
                add(Calendar.MONTH, amount)
            }.let { monthKey(it.timeInMillis) }
        }
    }
}
