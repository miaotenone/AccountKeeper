package com.example.accountkeeper.utils

data class BudgetUsage(
    val limit: Double?,
    val spent: Double,
    val remaining: Double?,
    val ratio: Double,
    val exceeded: Double
)

fun calculateBudgetUsage(limit: Double?, spent: Double): BudgetUsage {
    val normalizedSpent = spent.coerceAtLeast(0.0)
    val remaining = limit?.minus(normalizedSpent)
    val ratio = when {
        limit == null -> 0.0
        limit <= 0.0 -> if (normalizedSpent > 0.0) 1.0 else 0.0
        else -> (normalizedSpent / limit).coerceAtLeast(0.0)
    }
    return BudgetUsage(limit, normalizedSpent, remaining, ratio, if (remaining != null && remaining < 0.0) -remaining else 0.0)
}

fun projectedExpenseForEdit(
    currentMonthExpense: Double,
    oldAmount: Double?,
    oldMonth: String?,
    newAmount: Double,
    newMonth: String
): Double {
    val base = if (oldAmount != null && oldMonth == newMonth) currentMonthExpense - oldAmount else currentMonthExpense
    return (base + newAmount).coerceAtLeast(0.0)
}

fun projectedCategoryExpenseForEdit(
    currentCategoryExpense: Double,
    oldAmount: Double?,
    oldCategoryId: Long?,
    oldMonth: String?,
    newAmount: Double,
    newCategoryId: Long?,
    newMonth: String
): Double {
    // Uncategorized expenses belong to the monthly total only, never to a category budget.
    val withoutOld = if (oldAmount != null && newCategoryId != null && oldCategoryId == newCategoryId && oldMonth == newMonth) {
        currentCategoryExpense - oldAmount
    } else {
        currentCategoryExpense
    }
    return (withoutOld + if (newCategoryId != null) newAmount else 0.0).coerceAtLeast(0.0)
}
