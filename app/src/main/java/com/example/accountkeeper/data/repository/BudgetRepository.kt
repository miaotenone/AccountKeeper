package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.BudgetDao
import com.example.accountkeeper.data.local.BudgetMonthDao
import com.example.accountkeeper.data.local.CategoryDao
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.BudgetMonth
import com.example.accountkeeper.data.model.TransactionType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository @Inject constructor(private val dao: BudgetDao, private val monthDao: BudgetMonthDao, private val categoryDao: CategoryDao) {
    fun getByMonth(monthKey: String): Flow<List<Budget>> = dao.getByMonth(monthKey)
    fun getByMonthAndPeriod(monthKey: String, periodType: String): Flow<List<Budget>> = dao.getByMonthAndPeriod(monthKey, periodType)
    suspend fun getByMonthList(monthKey: String): List<Budget> = dao.getByMonthList(monthKey)
    suspend fun getByMonthAndPeriodList(monthKey: String, periodType: String): List<Budget> = dao.getByMonthAndPeriodList(monthKey, periodType)
    fun getMonthlyExpense(start: Long, end: Long): Flow<Double> = dao.getMonthlyExpense(start, end)
    fun getCategoryExpense(categoryId: Long, start: Long, end: Long): Flow<Double> = dao.getCategoryExpense(categoryId, start, end)
    fun getUncategorizedExpense(start: Long, end: Long): Flow<Double> = dao.getUncategorizedExpense(start, end)
    fun getExpenseCategoryIds(start: Long, end: Long): Flow<List<Long>> = dao.getExpenseCategoryIds(start, end)
    fun getAllCategoryExpenses(start: Long, end: Long): Flow<Map<Long, Double>> = dao.getAllCategoryExpenses(start, end).map { list -> list.associate { it.categoryId to it.total } }
    suspend fun initializeMonthIfNeeded(monthKey: String, previousMonthKey: String, now: Long = System.currentTimeMillis()): Boolean = dao.initializeMonthIfNeeded(monthKey, previousMonthKey, now)
    suspend fun insert(budget: Budget) { requireCategoryIsExpense(budget.categoryId); val existing = if (budget.categoryId == null) dao.getTotal(budget.monthKey) else dao.getByMonthList(budget.monthKey).firstOrNull { it.categoryId == budget.categoryId }; if (existing != null) dao.update(budget.copy(id = existing.id, createdAt = existing.createdAt)) else dao.insert(budget) }
    suspend fun update(budget: Budget) { requireCategoryIsExpense(budget.categoryId); dao.update(budget) }
    suspend fun delete(budget: Budget) = dao.delete(budget)
    suspend fun deleteByCategory(categoryId: Long) = dao.deleteByCategory(categoryId)
    suspend fun deleteAllBudgets() = dao.deleteAllBudgets()
    fun getAll(): Flow<List<Budget>> = dao.getAll()
    suspend fun isMonthInitialized(monthKey: String): Boolean = monthDao.exists(monthKey)
    suspend fun markMonthInitialized(monthKey: String) = monthDao.insert(BudgetMonth(monthKey))
    private suspend fun requireCategoryIsExpense(categoryId: Long?) { if (categoryId != null) require(categoryDao.getById(categoryId)?.type == TransactionType.EXPENSE) { "Budget categories must be expense categories" } }
}
