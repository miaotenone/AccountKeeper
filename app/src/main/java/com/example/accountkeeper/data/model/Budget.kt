package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BudgetPeriodType {
    MONTHLY, SEMI_ANNUAL, ANNUAL
}

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["monthKey", "categoryId"], unique = true), Index(value = ["categoryId"])]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val categoryId: Long? = null,
    val amount: Double,
    val periodType: String = BudgetPeriodType.MONTHLY.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
