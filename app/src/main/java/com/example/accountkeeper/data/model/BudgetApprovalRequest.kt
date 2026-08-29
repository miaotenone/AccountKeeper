package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BudgetApprovalType {
    BUDGET_ADJUSTMENT,
    PURCHASE_BUDGET
}

enum class BudgetApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
}

@Entity(
    tableName = "budget_approval_requests",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["monthKey", "periodType"]),
        Index(value = ["categoryId"])
    ]
)
data class BudgetApprovalRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BudgetApprovalType,
    val monthKey: String,
    val periodType: String = BudgetPeriodType.MONTHLY.name,
    val categoryId: Long? = null,
    val amount: Double,
    val purchaseDate: Long? = null,
    val reason: String = "",
    val attachments: String = "",
    val status: BudgetApprovalStatus = BudgetApprovalStatus.PENDING,
    val decisionNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val decidedAt: Long? = null
)
