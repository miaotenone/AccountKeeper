package com.example.accountkeeper.utils

import com.example.accountkeeper.data.model.Budget

fun copyBudgetsToMonth(previous: List<Budget>, monthKey: String, now: Long = System.currentTimeMillis()): List<Budget> {
    return previous.map {
        it.copy(id = 0L, monthKey = monthKey, createdAt = now, updatedAt = now)
    }
}
