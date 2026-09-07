package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_field_definitions",
    foreignKeys = [ForeignKey(entity = AssetCategoryEntity::class, parentColumns = ["id"], childColumns = ["assetCategoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["assetCategoryId"])]
)
data class AssetFieldDefinition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetCategoryId: Long,
    val name: String,
    val type: AssetFieldType,
    val required: Boolean = false,
    val options: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)