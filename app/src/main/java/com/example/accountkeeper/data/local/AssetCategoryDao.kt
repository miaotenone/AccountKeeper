package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetRootType
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetCategoryDao {
    @Query("SELECT * FROM asset_categories ORDER BY rootType, name")
    fun getAll(): Flow<List<AssetCategoryEntity>>

    @Query("SELECT * FROM asset_categories WHERE rootType = :rootType ORDER BY name")
    fun getByRootType(rootType: AssetRootType): Flow<List<AssetCategoryEntity>>

    @Query("SELECT * FROM asset_categories WHERE rootType = :rootType AND parentCategoryId IS NOT NULL ORDER BY name")
    fun getChildrenByRootType(rootType: AssetRootType): Flow<List<AssetCategoryEntity>>

    @Query("SELECT * FROM asset_categories WHERE rootType = :rootType AND parentCategoryId IS NOT NULL ORDER BY name")
    suspend fun getChildrenByRootTypeList(rootType: AssetRootType): List<AssetCategoryEntity>

    @Query("SELECT * FROM asset_categories WHERE rootType = :rootType AND parentCategoryId IS NULL LIMIT 1")
    suspend fun getRootCategory(rootType: AssetRootType): AssetCategoryEntity?

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
