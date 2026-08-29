package com.example.accountkeeper

import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.utils.MonthlyEntry
import com.example.accountkeeper.utils.calculateBudgetUsage
import com.example.accountkeeper.utils.calculateMonthlySummary
import com.example.accountkeeper.utils.copyBudgetsToMonth
import com.example.accountkeeper.utils.projectedCategoryExpenseForEdit
import com.example.accountkeeper.utils.projectedExpenseForEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetMathTest {
    @Test
    fun `zero budget with spending is exceeded`() {
        val usage = calculateBudgetUsage(0.0, 12.0)
        assertEquals(1.0, usage.ratio, 0.0)
        assertEquals(12.0, usage.exceeded, 0.0)
    }

    @Test
    fun `unset budget is not counted as exceeded`() {
        val usage = calculateBudgetUsage(null, 12.0)
        assertEquals(0.0, usage.exceeded, 0.0)
        assertEquals(null, usage.remaining)
    }

    @Test
    fun `editing same month removes old amount before adding new amount`() {
        assertEquals(150.0, projectedExpenseForEdit(100.0, 40.0, "2026-08", 90.0, "2026-08"), 0.0)
    }

    @Test
    fun `moving transaction to another month does not subtract from target month`() {
        assertEquals(190.0, projectedExpenseForEdit(100.0, 40.0, "2026-07", 90.0, "2026-08"), 0.0)
    }

    @Test
    fun `category projection removes old amount only for same category and month`() {
        assertEquals(150.0, projectedCategoryExpenseForEdit(100.0, 40.0, 2L, "2026-08", 90.0, 2L, "2026-08"), 0.0)
        assertEquals(190.0, projectedCategoryExpenseForEdit(100.0, 40.0, 2L, "2026-08", 90.0, 3L, "2026-08"), 0.0)
    }

    @Test
    fun `changing expense to income removes old expense from same month`() {
        assertEquals(60.0, projectedExpenseForEdit(100.0, 40.0, "2026-08", 0.0, "2026-08"), 0.0)
    }

    @Test
    fun `new income is excluded from projected monthly expense`() {
        assertEquals(100.0, projectedExpenseForEdit(100.0, null, null, 0.0, "2026-08"), 0.0)
    }

    @Test
    fun `deleting an expense restores the budget amount`() {
        assertEquals(60.0, projectedExpenseForEdit(100.0, 40.0, "2026-08", 0.0, "2026-08"), 0.0)
    }

    @Test
    fun `uncategorized expense does not change category budget projection`() {
        assertEquals(100.0, projectedCategoryExpenseForEdit(100.0, null, null, "2026-08", 40.0, null, "2026-08"), 0.0)
    }

    @Test
    fun `copying budgets to new month resets ids and timestamps`() {
        val copied = copyBudgetsToMonth(listOf(Budget(id = 3, monthKey = "2026-07", categoryId = 5L, amount = 88.0, createdAt = 1L, updatedAt = 2L)), "2026-08", now = 99L)
        assertEquals(1, copied.size)
        assertEquals(0L, copied.first().id)
        assertEquals("2026-08", copied.first().monthKey)
        assertEquals(99L, copied.first().createdAt)
        assertEquals(99L, copied.first().updatedAt)
    }

    @Test
    fun `monthly summary only includes entries inside current month`() {
        val summary = calculateMonthlySummary(listOf(MonthlyEntry(100L, 40.0, true), MonthlyEntry(200L, 15.0, false), MonthlyEntry(99L, 1000.0, true), MonthlyEntry(300L, 1000.0, false)), 100L, 300L)
        assertEquals(40.0, summary.income, 0.0)
        assertEquals(15.0, summary.expense, 0.0)
        assertEquals(25.0, summary.balance, 0.0)
    }

    @Test
    fun `monthly summary includes the start instant and excludes the next month start`() {
        val summary = calculateMonthlySummary(
            listOf(MonthlyEntry(1_000L, 10.0, true), MonthlyEntry(2_000L, 20.0, false)),
            startOfMonth = 1_000L,
            endOfMonth = 2_000L
        )
        assertEquals(10.0, summary.income, 0.0)
        assertEquals(0.0, summary.expense, 0.0)
    }

    @Test
    fun `total budget and category budgets are independent`() {
        val totalUsage = calculateBudgetUsage(10000.0, 9000.0)
        val foodUsage = calculateBudgetUsage(3000.0, 4000.0)
        assertEquals(0.0, totalUsage.exceeded, 0.0)
        assertEquals(1000.0, foodUsage.exceeded, 0.0)
    }

    @Test
    fun `positive budget with zero spending shows no exceeded`() {
        val usage = calculateBudgetUsage(5000.0, 0.0)
        assertEquals(0.0, usage.exceeded, 0.0)
        assertEquals(5000.0, usage.remaining!!, 0.0)
    }

    @Test
    fun `new expense to uncategorized does not affect category projection`() {
        assertEquals(100.0, projectedCategoryExpenseForEdit(100.0, null, null, "2026-08", 50.0, null, "2026-08"), 0.0)
    }

    @Test
    fun `editing transaction changing category does not subtract from old category`() {
        assertEquals(190.0, projectedCategoryExpenseForEdit(100.0, 40.0, 2L, "2026-08", 90.0, 3L, "2026-08"), 0.0)
    }

    @Test
    fun `editing transaction changing category adds to new category`() {
        assertEquals(90.0, projectedCategoryExpenseForEdit(0.0, null, null, "2026-08", 90.0, 3L, "2026-08"), 0.0)
    }

    @Test
    fun `income to expense projection includes old income removal`() {
        assertEquals(90.0, projectedExpenseForEdit(0.0, null, null, 90.0, "2026-08"), 0.0)
    }

    @Test
    fun `budget usage with exact limit shows no exceeded`() {
        val usage = calculateBudgetUsage(500.0, 500.0)
        assertEquals(0.0, usage.exceeded, 0.0)
        assertEquals(0.0, usage.remaining!!, 0.0)
    }

    @Test
    fun `negative spending is clamped to zero`() {
        val usage = calculateBudgetUsage(500.0, -100.0)
        assertEquals(0.0, usage.exceeded, 0.0)
        assertEquals(500.0, usage.remaining!!, 0.0)
    }

    @Test
    fun `copying multiple budgets preserves amounts and category assignments`() {
        val budgets = listOf(
            Budget(id = 1, monthKey = "2026-07", categoryId = 5L, amount = 88.0, createdAt = 1L, updatedAt = 2L),
            Budget(id = 2, monthKey = "2026-07", categoryId = null, amount = 5000.0, createdAt = 3L, updatedAt = 4L)
        )
        val copied = copyBudgetsToMonth(budgets, "2026-08", now = 99L)
        assertEquals(2, copied.size)
        assertEquals(5L, copied[0].categoryId)
        assertEquals(88.0, copied[0].amount, 0.0)
        assertNull(copied[1].categoryId)
        assertEquals(5000.0, copied[1].amount, 0.0)
    }

    @Test
    fun `category expense only counts same category and month`() {
        assertEquals(190.0, projectedCategoryExpenseForEdit(100.0, 40.0, 2L, "2026-08", 90.0, 3L, "2026-08"), 0.0)
    }
}
