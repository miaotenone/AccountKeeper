package com.example.accountkeeper.data.repository

import androidx.room.withTransaction
import com.example.accountkeeper.data.local.AppDatabase
import com.example.accountkeeper.data.local.AssetDao
import com.example.accountkeeper.data.local.TransactionDao
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentOwnerType
import com.example.accountkeeper.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AssetRepository @Inject constructor(
    private val assetDao: AssetDao,
    private val attachmentRepository: AttachmentRepository,
    private val database: AppDatabase,
    private val transactionDao: TransactionDao
) {
    fun getAllAssets(): Flow<List<Asset>> = assetDao.getAllAssets()
    fun getAssetsBetween(startDate: Long, endDate: Long): Flow<List<Asset>> = assetDao.getAssetsBetween(startDate, endDate)
    fun getAssetsByStatus(status: AssetStatus): Flow<List<Asset>> = assetDao.getAssetsByStatus(status)
    fun searchAssets(query: String): Flow<List<Asset>> = assetDao.searchAssets(query)
    suspend fun getAssetById(id: Long): Asset? = assetDao.getAssetById(id)

    suspend fun getBySourceApprovalId(id: Long): Asset? = assetDao.getBySourceApprovalId(id)
    suspend fun getByTransactionId(id: Long): Asset? = assetDao.getByTransactionId(id)

    suspend fun confirmOwned(assetId: Long, transactionId: Long) {
        database.withTransaction {
            val current = assetDao.getAssetById(assetId) ?: error("Asset not found")
            check(current.status == AssetStatus.IN_PROGRESS) { "Only in-progress assets can be confirmed" }
            check(current.transactionId == null) { "Asset is already linked to a transaction" }
            val transaction = transactionDao.getTransactionById(transactionId) ?: error("Transaction not found")
            check(transaction.type == TransactionType.EXPENSE) { "Only expense transactions can be linked" }
            check(transaction.categoryId == current.categoryId) { "Transaction category does not match" }
            val start = monthStart(current.purchaseDate ?: current.date)
            check(transaction.date >= start && transaction.date < monthEnd(start)) { "Transaction month does not match" }
            check(assetDao.getByTransactionId(transactionId) == null) { "Transaction is already linked" }
            assetDao.updateAsset(current.copy(status = AssetStatus.OWNED, transactionId = transactionId, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun insertAsset(asset: Asset) {
        assetDao.insertAsset(asset)
        syncAttachments(asset)
    }

    suspend fun updateAsset(asset: Asset) {
        assetDao.updateAsset(asset)
        syncAttachments(asset)
    }

    suspend fun deleteAsset(asset: Asset) {
        attachmentRepository.deleteForOwner(AttachmentOwnerType.ASSET, asset.id)
        assetDao.deleteAsset(asset)
    }

    suspend fun deleteAssets(assets: List<Asset>) {
        assets.forEach { attachmentRepository.deleteForOwner(AttachmentOwnerType.ASSET, it.id) }
        assetDao.deleteAssets(assets)
    }

    suspend fun deleteAllAssets() {
        attachmentRepository.deleteForOwnerType(AttachmentOwnerType.ASSET)
        assetDao.deleteAllAssets()
    }

    fun getTotalAssets(): Flow<Double?> = assetDao.getTotalAssets()
    fun getTotalLiabilities(): Flow<Double?> = assetDao.getTotalLiabilities()
    fun getTotalAmountByStatus(status: AssetStatus): Flow<Double?> = assetDao.getTotalAmountByStatus(status)

    private suspend fun syncAttachments(asset: Asset) = attachmentRepository.replaceForOwner(AttachmentOwnerType.ASSET, asset.id, AttachmentConverter.fromJson(asset.attachments))
    private fun monthStart(time: Long): Long = java.util.Calendar.getInstance().apply {
        timeInMillis = time
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun monthEnd(start: Long): Long = java.util.Calendar.getInstance().apply { timeInMillis = start; add(java.util.Calendar.MONTH, 1) }.timeInMillis
}
