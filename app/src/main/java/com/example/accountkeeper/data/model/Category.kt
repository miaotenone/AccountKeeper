package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val isDefault: Boolean = false,
    // 仅对 ASSET 类型的分类有效：true=正资产（如借出），false=负资产（如负债）
    val isPositiveAsset: Boolean = true
)
