package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.AssetDao
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AssetRepository @Inject constructor(
    private val assetDao: AssetDao
) {
    fun getAllAssets(): Flow<List<Asset>> = assetDao.getAllAssets()
    fun getAssetsBetween(startDate: Long, endDate: Long): Flow<List<Asset>> = 
        assetDao.getAssetsBetween(startDate, endDate)
    fun getAssetsByStatus(status: AssetStatus): Flow<List<Asset>> = assetDao.getAssetsByStatus(status)
    fun searchAssets(query: String): Flow<List<Asset>> = assetDao.searchAssets(query)
    suspend fun getAssetById(id: Long): Asset? = assetDao.getAssetById(id)
    suspend fun insertAsset(asset: Asset) = assetDao.insertAsset(asset)
    suspend fun updateAsset(asset: Asset) = assetDao.updateAsset(asset)
    suspend fun deleteAsset(asset: Asset) = assetDao.deleteAsset(asset)
    suspend fun deleteAssets(assets: List<Asset>) = assetDao.deleteAssets(assets)
    suspend fun deleteAllAssets() = assetDao.deleteAllAssets()
    
    // Totals
    fun getTotalAssets(): Flow<Double?> = assetDao.getTotalAssets()
    fun getTotalLiabilities(): Flow<Double?> = assetDao.getTotalLiabilities()
    fun getTotalAmountByStatus(status: AssetStatus): Flow<Double?> = assetDao.getTotalAmountByStatus(status)
}
