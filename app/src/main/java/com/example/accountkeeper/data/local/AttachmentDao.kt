package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.AttachmentEntity
import com.example.accountkeeper.data.model.AttachmentOwnerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt DESC")
    fun getForOwner(ownerType: AttachmentOwnerType, ownerId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt DESC")
    suspend fun getForOwnerList(ownerType: AttachmentOwnerType, ownerId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AttachmentEntity?


    @Query("SELECT COUNT(*) FROM attachments WHERE filePath = :filePath")
    suspend fun countByFilePath(filePath: String): Int
    @Query("SELECT * FROM attachments WHERE sha256 = :sha256 LIMIT 1")
    suspend fun findByHash(sha256: String): AttachmentEntity?

    @Insert
    suspend fun insert(attachment: AttachmentEntity)

    @Update
    suspend fun update(attachment: AttachmentEntity)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteForOwner(ownerType: AttachmentOwnerType, ownerId: Long)

    @Query("DELETE FROM attachments WHERE ownerType = :ownerType")
    suspend fun deleteForOwnerType(ownerType: AttachmentOwnerType)

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()
}
