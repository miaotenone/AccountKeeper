package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.AttachmentDao
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentEntity
import com.example.accountkeeper.data.model.AttachmentOwnerType
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AttachmentRepository @Inject constructor(private val dao: AttachmentDao) {
    fun getAll(): Flow<List<AttachmentEntity>> = dao.getAll()

    fun getForOwner(ownerType: AttachmentOwnerType, ownerId: Long): Flow<List<AttachmentEntity>> =
        dao.getForOwner(ownerType, ownerId)

    suspend fun getById(id: String): AttachmentEntity? = dao.getById(id)
    suspend fun findByHash(sha256: String): AttachmentEntity? = dao.findByHash(sha256)
    suspend fun countByFilePath(filePath: String): Int = dao.countByFilePath(filePath)
    suspend fun getForOwnerList(ownerType: AttachmentOwnerType, ownerId: Long): List<AttachmentEntity> =
        dao.getForOwnerList(ownerType, ownerId)

    suspend fun insert(attachment: AttachmentEntity) = dao.insert(attachment)
    suspend fun deleteForOwner(ownerType: AttachmentOwnerType, ownerId: Long) =
        dao.deleteForOwner(ownerType, ownerId)
    suspend fun deleteForOwnerType(ownerType: AttachmentOwnerType) =
        dao.deleteForOwnerType(ownerType)

    suspend fun replaceForOwner(ownerType: AttachmentOwnerType, ownerId: Long, attachments: List<Attachment>) {
        dao.deleteForOwner(ownerType, ownerId)
        attachments.forEach { attachment ->
            dao.insert(
                AttachmentEntity(
                    id = "${ownerType.name}_${ownerId}_${attachment.id}",
                    ownerType = ownerType,
                    ownerId = ownerId,
                    fileName = attachment.fileName,
                    filePath = attachment.filePath,
                    mimeType = attachment.mimeType,
                    fileSize = attachment.fileSize,
                    sha256 = attachmentSha256(ownerType, ownerId, attachment),
                    createdAt = attachment.createdAt
                )
            )
        }
    }

    private fun attachmentSha256(ownerType: AttachmentOwnerType, ownerId: Long, attachment: Attachment): String {
        val file = File(attachment.filePath)
        val bytes = if (file.exists()) file.readBytes() else attachment.filePath.toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    suspend fun update(attachment: AttachmentEntity) = dao.update(attachment)

    suspend fun delete(attachment: AttachmentEntity) = dao.delete(attachment)
    suspend fun deleteAll() = dao.deleteAll()
}
