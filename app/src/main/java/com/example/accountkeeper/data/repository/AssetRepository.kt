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

    suspend fun linkTransaction(assetId: Long, transactionId: Long) = database.withTransaction {
        val current = assetDao.getAssetById(assetId) ?: error("Asset not found")
        check(current.sourceApprovalId != null) { "Only approval purchases can link a transaction" }
        check(current.status == AssetStatus.IN_PROGRESS) { "Only in-progress approval purchases can link a transaction" }
        check(current.transactionId == null) { "Asset is already linked to a transaction" }
        validateTransaction(transactionId, current, null)
        assetDao.updateAsset(current.copy(transactionId = transactionId, updatedAt = System.currentTimeMillis()))
    }

    suspend fun confirmOwned(assetId: Long) = database.withTransaction {
        val current = assetDao.getAssetById(assetId) ?: error("Asset not found")
        check(current.sourceApprovalId != null) { "Only approval purchases need purchase confirmation" }
        check(current.status == AssetStatus.IN_PROGRESS) { "Only in-progress assets can be confirmed" }
        val transactionId = current.transactionId ?: error("Link an expense transaction first")
        validateTransaction(transactionId, current, current.id)
        assetDao.updateAsset(current.copy(status = AssetStatus.OWNED, updatedAt = System.currentTimeMillis()))
        database.budgetApprovalDao().getById(current.sourceApprovalId)?.let { approval ->
            database.budgetApprovalDao().update(approval.copy(status = com.example.accountkeeper.data.model.BudgetApprovalStatus.PURCHASE_COMPLETED, updatedAt = System.currentTimeMillis()))
        }
    }
    suspend fun insertAsset(asset: Asset) = database.withTransaction {
        validateAssetForSave(asset, null)
        assetDao.insertAsset(asset)
        syncAttachments(asset)
    }

    suspend fun updateAsset(asset: Asset) = database.withTransaction {
        val existing = assetDao.getAssetById(asset.id)
        validateAssetForSave(asset, existing)
        assetDao.updateAsset(asset)
        syncAttachments(asset)
    }

    private suspend fun validateAssetForSave(asset: Asset, existing: Asset?) {
        asset.transactionId?.let { validateTransaction(it, asset, asset.id) }
        if (existing?.sourceApprovalId != null && existing.status == AssetStatus.IN_PROGRESS && asset.status == AssetStatus.OWNED) {
            check(asset.transactionId != null) { "Link an expense transaction before confirming an approval purchase" }
        }
    }

    private suspend fun validateTransaction(transactionId: Long, asset: Asset, selfId: Long?) {
        val transaction = transactionDao.getTransactionById(transactionId) ?: error("Transaction not found")
        check(transaction.type == TransactionType.EXPENSE) { "Only expense transactions can be linked" }
        check(transaction.categoryId == asset.categoryId) { "Transaction category does not match" }
        check(kotlin.math.abs(transaction.amount - asset.amount) < 0.000001) { "Transaction amount does not match" }
        check(transaction.date >= (asset.purchaseDate ?: asset.date)) { "Transaction date is before purchase date" }
        val linked = assetDao.getByTransactionId(transactionId)
        check(linked == null || linked.id == selfId) { "Transaction is already linked" }
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
