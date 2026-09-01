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
        ),
        ForeignKey(
            entity = AssetCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetCategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["assetCategoryId"])
    ]
)
data class BudgetApprovalRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BudgetApprovalType,
    val categoryId: Long? = null,
    val assetCategoryId: Long? = null,
    val amount: Double,
    val purchaseDate: Long? = null,
    val reason: String = "",
    val itemName: String = "",
    val specification: String = "",
    val quantity: Double = 1.0,
    val attachments: String = "",
    val status: BudgetApprovalStatus = BudgetApprovalStatus.PENDING,
    val decisionNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val decidedAt: Long? = null
)
