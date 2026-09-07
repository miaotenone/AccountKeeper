package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetDirection
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetCategoryDao {
    @Query("SELECT * FROM asset_categories ORDER BY direction, isDefault DESC, createdAt, name")
    fun getAll(): Flow<List<AssetCategoryEntity>>

    @Query("SELECT * FROM asset_categories WHERE direction = :direction ORDER BY isDefault DESC, createdAt, name")
    fun getByDirection(direction: AssetDirection): Flow<List<AssetCategoryEntity>>

    @Query("SELECT * FROM asset_categories WHERE name = :name AND direction = :direction LIMIT 1")
    suspend fun getByNameAndDirection(name: String, direction: AssetDirection): AssetCategoryEntity?

    @Query("SELECT * FROM asset_categories ORDER BY createdAt, id")
    suspend fun getAllList(): List<AssetCategoryEntity>

    @Query("SELECT * FROM asset_categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AssetCategoryEntity?

    @Insert
    suspend fun insert(entity: AssetCategoryEntity): Long

    @Update
    suspend fun update(entity: AssetCategoryEntity)

    @Delete
    suspend fun delete(entity: AssetCategoryEntity)

    @Query("DELETE FROM asset_categories")
    suspend fun deleteAll()
}
