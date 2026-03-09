package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Asset(
    @PrimaryKey
    val id: Long,
    val date: Long,              // 日期（必填）
    val amount: Double,          // 金额（必填）
    val status: AssetStatus,     // 资产状态（必填）
    val categoryId: Long?,       // 资产分类（从分类配置读取）
    val targetPerson: String,    // 目标对象
    val targetAccount: String,   // 目标账户
    val note: String,            // 备注
    val isCompleted: Boolean = false,  // 是否已完成（用于复选框状态）
    val attachments: String = "",      // 附件列表(JSON字符串)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
