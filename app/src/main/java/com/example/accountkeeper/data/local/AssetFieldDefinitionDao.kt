package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.AssetFieldDefinition
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetFieldDefinitionDao {
    @Query("SELECT * FROM asset_field_definitions WHERE assetCategoryId = :categoryId ORDER BY sortOrder, id")
    fun getForCategory(categoryId: Long): Flow<List<AssetFieldDefinition>>
    @Query("SELECT * FROM asset_field_definitions WHERE assetCategoryId = :categoryId ORDER BY sortOrder, id")
    suspend fun getForCategoryList(categoryId: Long): List<AssetFieldDefinition>
    @Query("SELECT * FROM asset_field_definitions ORDER BY assetCategoryId, sortOrder, id")
    suspend fun getAllList(): List<AssetFieldDefinition>
    @Insert suspend fun insert(field: AssetFieldDefinition): Long
    @Update suspend fun update(field: AssetFieldDefinition)
    @Delete suspend fun delete(field: AssetFieldDefinition)
    @Query("DELETE FROM asset_field_definitions WHERE assetCategoryId = :categoryId") suspend fun deleteForCategory(categoryId: Long)
}