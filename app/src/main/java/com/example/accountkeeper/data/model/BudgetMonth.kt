package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_months")
data class BudgetMonth(
    @PrimaryKey val monthKey: String,
    val initializedAt: Long = System.currentTimeMillis()
)
