package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.BudgetMonth
import kotlinx.coroutines.flow.Flow

data class CategoryExpenseTuple(val categoryId: Long, val total: Double)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY categoryId")
    fun getByMonth(monthKey: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY categoryId")
    suspend fun getByMonthList(monthKey: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND categoryId IS NULL LIMIT 1")
    suspend fun getTotal(monthKey: String): Budget?

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND periodType = :periodType ORDER BY categoryId")
    fun getByMonthAndPeriod(monthKey: String, periodType: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND periodType = :periodType ORDER BY categoryId")
    suspend fun getByMonthAndPeriodList(monthKey: String, periodType: String): List<Budget>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey AND periodType = :periodType AND categoryId IS NULL LIMIT 1")
    suspend fun getTotalByPeriod(monthKey: String, periodType: String): Budget?

    @Query("SELECT * FROM budgets ORDER BY monthKey, categoryId")
    fun getAll(): Flow<List<Budget>>

    @Query("SELECT EXISTS(SELECT 1 FROM budget_months WHERE monthKey = :monthKey)")
    suspend fun isMonthInitialized(monthKey: String): Boolean

    @Query("SELECT COUNT(*) FROM budgets WHERE monthKey = :monthKey")
    suspend fun countForMonth(monthKey: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMonth(month: BudgetMonth)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(budgets: List<Budget>)

    @Transaction
    suspend fun initializeMonthIfNeeded(monthKey: String, previousMonthKey: String, now: Long): Boolean {
        if (isMonthInitialized(monthKey)) return false
        if (countForMonth(monthKey) == 0) {
            val copied = getByMonthList(previousMonthKey).map { budget ->
                budget.copy(id = 0, monthKey = monthKey, createdAt = now, updatedAt = now)
            }
            if (copied.isNotEmpty()) insertAll(copied)
        }
        insertMonth(BudgetMonth(monthKey = monthKey, initializedAt = now))
        return true
    }

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date >= :start AND date < :end")
    fun getMonthlyExpense(start: Long, end: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND categoryId = :categoryId AND date >= :start AND date < :end")
    fun getCategoryExpense(categoryId: Long, start: Long, end: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND categoryId IS NULL AND date >= :start AND date < :end")
    fun getUncategorizedExpense(start: Long, end: Long): Flow<Double>

    @Query("SELECT DISTINCT categoryId FROM transactions WHERE type = 'EXPENSE' AND categoryId IS NOT NULL AND date >= :start AND date < :end")
    fun getExpenseCategoryIds(start: Long, end: Long): Flow<List<Long>>

    @Query("SELECT categoryId, COALESCE(SUM(amount), 0) as total FROM transactions WHERE type = 'EXPENSE' AND categoryId IS NOT NULL AND date >= :start AND date < :end GROUP BY categoryId")
    fun getAllCategoryExpenses(start: Long, end: Long): Flow<List<CategoryExpenseTuple>>
}
