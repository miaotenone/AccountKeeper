package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    indices = [
        Index(value = ["ownerType", "ownerId"]),
        Index(value = ["sha256"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val ownerType: AttachmentOwnerType,
    val ownerId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long,
    val sha256: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttachmentOwnerType {
    TRANSACTION,
    ASSET,
    APPROVAL,
    BILL
}
