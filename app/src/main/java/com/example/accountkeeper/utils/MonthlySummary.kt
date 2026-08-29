package com.example.accountkeeper.utils

data class MonthlyEntry(
    val date: Long,
    val amount: Double,
    val income: Boolean
)

data class MonthlySummary(
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
}

fun calculateMonthlySummary(
    entries: Iterable<MonthlyEntry>,
    startOfMonth: Long,
    endOfMonth: Long
): MonthlySummary {
    var income = 0.0
    var expense = 0.0
    entries.forEach { entry ->
        if (entry.date < startOfMonth || entry.date >= endOfMonth) return@forEach
        if (entry.income) income += entry.amount else expense += entry.amount
    }
    return MonthlySummary(income, expense)
}
