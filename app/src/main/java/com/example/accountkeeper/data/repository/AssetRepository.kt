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

    suspend fun linkTransaction(assetId: Long, transactionId: Long) {
        database.withTransaction {
            val current = assetDao.getAssetById(assetId) ?: error("Asset not found")
            check(current.status == AssetStatus.IN_PROGRESS) { "Only in-progress assets can be linked" }
            check(current.transactionId == null) { "Asset is already linked to a transaction" }
            val transaction = transactionDao.getTransactionById(transactionId) ?: error("Transaction not found")
            check(transaction.type == TransactionType.EXPENSE) { "Only expense transactions can be linked" }
            check(transaction.categoryId == current.categoryId) { "Transaction category does not match" }
            check(kotlin.math.abs(transaction.amount - current.amount) < 0.000001) { "Transaction amount does not match" }
            check(transaction.date >= (current.purchaseDate ?: current.date)) { "Transaction date is before purchase date" }
            check(assetDao.getByTransactionId(transactionId) == null) { "Transaction is already linked" }
            assetDao.updateAsset(current.copy(transactionId = transactionId, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun confirmOwned(assetId: Long) {
        database.withTransaction {
            val current = assetDao.getAssetById(assetId) ?: error("Asset not found")
            check(current.status == AssetStatus.IN_PROGRESS) { "Only in-progress assets can be confirmed" }
            val transactionId = current.transactionId ?: error("Link an expense transaction first")
            val transaction = transactionDao.getTransactionById(transactionId) ?: error("Transaction not found")
            check(transaction.type == TransactionType.EXPENSE && transaction.categoryId == current.categoryId) { "Transaction no longer matches" }
            check(kotlin.math.abs(transaction.amount - current.amount) < 0.000001) { "Transaction amount no longer matches" }
            check(transaction.date >= (current.purchaseDate ?: current.date)) { "Transaction date no longer matches" }
            assetDao.updateAsset(current.copy(status = AssetStatus.OWNED, isCompleted = true, updatedAt = System.currentTimeMillis()))
            current.sourceApprovalId?.let { approvalId ->
                database.budgetApprovalDao().getById(approvalId)?.let { approval ->
                    database.budgetApprovalDao().update(approval.copy(status = com.example.accountkeeper.data.model.BudgetApprovalStatus.PURCHASE_COMPLETED, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    suspend fun insertAsset(asset: Asset) = database.withTransaction {
        validateLinkedTransaction(asset)
        assetDao.insertAsset(asset)
        syncAttachments(asset)
    }

    suspend fun updateAsset(asset: Asset) = database.withTransaction {
        validateLinkedTransaction(asset)
        assetDao.updateAsset(asset)
        syncAttachments(asset)
    }

    private suspend fun validateLinkedTransaction(asset: Asset) {
        val transactionId = asset.transactionId ?: return
        check(asset.status == AssetStatus.IN_PROGRESS) { "Only in-progress assets can link a transaction" }
        val transaction = transactionDao.getTransactionById(transactionId) ?: error("Transaction not found")
        check(transaction.type == TransactionType.EXPENSE) { "Only expense transactions can be linked" }
        check(transaction.categoryId == asset.categoryId) { "Transaction category does not match" }
        check(kotlin.math.abs(transaction.amount - asset.amount) < 0.000001) { "Transaction amount does not match" }
        check(transaction.date >= (asset.purchaseDate ?: asset.date)) { "Transaction date is before purchase date" }
        val linked = assetDao.getByTransactionId(transactionId)
        check(linked == null || linked.id == asset.id) { "Transaction is already linked" }
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
