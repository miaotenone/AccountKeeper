package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.accountkeeper.data.model.AssetFieldValue

@Dao
interface AssetFieldValueDao {
    @Query("SELECT * FROM asset_field_values WHERE assetId = :assetId") suspend fun getForAsset(assetId: Long): List<AssetFieldValue>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(values: List<AssetFieldValue>)
    @Query("DELETE FROM asset_field_values WHERE assetId = :assetId") suspend fun deleteForAsset(assetId: Long)
    @Query("SELECT * FROM asset_field_values") suspend fun getAllList(): List<AssetFieldValue>
    @Query("DELETE FROM asset_field_values WHERE fieldId = :fieldId") suspend fun deleteForField(fieldId: Long)
    @Query("DELETE FROM asset_field_values") suspend fun deleteAll()
}