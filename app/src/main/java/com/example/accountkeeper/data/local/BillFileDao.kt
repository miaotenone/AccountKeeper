package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.BillFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillFileDao {
    @Query("SELECT * FROM bill_files ORDER BY createdAt DESC")
    fun getAll(): Flow<List<BillFileEntity>>

    @Query("SELECT * FROM bill_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BillFileEntity?

    @Query("SELECT * FROM bill_files WHERE sha256 = :sha256 LIMIT 1")
    suspend fun getBySha256(sha256: String): BillFileEntity?

    @Insert
    suspend fun insert(billFile: BillFileEntity)

    @Update
    suspend fun update(billFile: BillFileEntity)

    @Delete
    suspend fun delete(billFile: BillFileEntity)

    @Query("DELETE FROM bill_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bill_files")
    suspend fun deleteAll()
}
