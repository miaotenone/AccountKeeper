package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(entity = AssetCategoryEntity::class, parentColumns = ["id"], childColumns = ["assetCategoryId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = Transaction::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["assetCategoryId"]), Index(value = ["sourceApprovalId"], unique = true), Index(value = ["transactionId"], unique = true)]
)
data class Asset(
    @PrimaryKey val id: Long,
    val date: Long,
    val amount: Double,
    val status: AssetStatus,
    val assetCategoryId: Long? = null,
    val categoryId: Long? = null,
    val name: String = "",
    val specification: String = "",
    val quantity: Double = 1.0,
    val purchaseDate: Long? = null,
    val sourceApprovalId: Long? = null,
    val transactionId: Long? = null,
    val targetPerson: String,
    val targetAccount: String,
    val note: String,
    val isCompleted: Boolean = false,
    val attachments: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val assetRootType: String = "PHYSICAL",
    val supplier: String = "",
    val location: String = "",
    val userOrDepartment: String = "",
    val warranty: String = "",
    val serviceStartDate: Long? = null,
    val serviceEndDate: Long? = null,
    val renewalCycle: String = "",
    val accessUrl: String = ""
)
