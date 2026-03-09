package com.example.accountkeeper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY date DESC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAssetsBetween(startDate: Long, endDate: Long): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: Long): Asset?

    @Query("SELECT * FROM assets WHERE status = :status ORDER BY date DESC")
    fun getAssetsByStatus(status: AssetStatus): Flow<List<Asset>>

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()

    @Insert
    suspend fun insertAsset(asset: Asset)

    @Update
    suspend fun updateAsset(asset: Asset)

    @Delete
    suspend fun deleteAsset(asset: Asset)

    @Delete
    suspend fun deleteAssets(assets: List<Asset>)

    @Query("""
        SELECT * FROM assets 
        WHERE note LIKE '%' || :query || '%' OR targetPerson LIKE '%' || :query || '%' OR targetAccount LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    fun searchAssets(query: String): Flow<List<Asset>>

    @Query("SELECT SUM(amount) FROM assets WHERE status = :status AND isCompleted = 0")
    fun getTotalAmountByStatus(status: AssetStatus): Flow<Double?>

    @Query("SELECT SUM(amount) FROM assets WHERE (status = 'OWNED' OR status = 'TEMPORARILY_WITH_ME') AND isCompleted = 0")
    fun getTotalAssets(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM assets WHERE (status = 'NOT_OWNED' OR status = 'TEMPORARILY_WITH_OTHERS') AND isCompleted = 0")
    fun getTotalLiabilities(): Flow<Double?>
}
