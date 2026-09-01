package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bill_files",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["ownerType"]), Index(value = ["categoryId"])]
)
data class BillFileEntity(
    @PrimaryKey val id: String,
    val ownerType: String = "BILL",
    val ownerId: Long? = null,
    val categoryId: Long? = null,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long,
    val sha256: String,
    val createdAt: Long = System.currentTimeMillis()
)
