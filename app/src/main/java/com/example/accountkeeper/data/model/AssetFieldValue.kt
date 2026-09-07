package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "asset_field_values",
    primaryKeys = ["assetId", "fieldId"],
    foreignKeys = [
        ForeignKey(entity = Asset::class, parentColumns = ["id"], childColumns = ["assetId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AssetFieldDefinition::class, parentColumns = ["id"], childColumns = ["fieldId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["fieldId"])]
)
data class AssetFieldValue(val assetId: Long, val fieldId: Long, val value: String)