package com.example.accountkeeper.data.repository

import androidx.room.withTransaction
import com.example.accountkeeper.data.local.AppDatabase
import com.example.accountkeeper.data.local.AssetCategoryDao
import com.example.accountkeeper.data.local.ApprovalFieldValueDao
import com.example.accountkeeper.data.local.AssetFieldValueDao
import com.example.accountkeeper.data.local.AssetDao
import com.example.accountkeeper.data.local.BudgetApprovalDao
import com.example.accountkeeper.data.local.BudgetDao
import com.example.accountkeeper.data.local.BudgetMonthDao
import com.example.accountkeeper.data.local.CategoryDao
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetFieldValue
import com.example.accountkeeper.data.model.AssetDirection
import com.example.accountkeeper.data.model.ApprovalFieldValue
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentOwnerType
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.TransactionType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class BudgetApprovalRepository @Inject constructor(
    private val database: AppDatabase,
    private val approvalDao: BudgetApprovalDao,
    private val budgetDao: BudgetDao,
    private val budgetMonthDao: BudgetMonthDao,
    private val categoryDao: CategoryDao,
    private val assetCategoryDao: AssetCategoryDao,
    private val assetDao: AssetDao,
    private val attachmentRepository: AttachmentRepository,
    private val approvalFieldValueDao: ApprovalFieldValueDao,
    private val assetFieldValueDao: AssetFieldValueDao
) {
    fun getAll(): Flow<List<BudgetApprovalRequest>> = approvalDao.getAll()
    suspend fun getById(id: Long): BudgetApprovalRequest? = approvalDao.getById(id)

    suspend fun restore(request: BudgetApprovalRequest): Long {
        val id = approvalDao.insert(request)
        attachmentRepository.replaceForOwner(AttachmentOwnerType.APPROVAL, id, AttachmentConverter.fromJson(request.attachments))
        return id
    }

    suspend fun submit(request: BudgetApprovalRequest): Long {
        validate(request)
        val now = System.currentTimeMillis()
        val normalized = request.copy(id = 0, status = BudgetApprovalStatus.PENDING, decisionNote = "", decidedAt = null, createdAt = now, updatedAt = now)
        return try {
            val id = approvalDao.insert(normalized)
            attachmentRepository.replaceForOwner(AttachmentOwnerType.APPROVAL, id, AttachmentConverter.fromJson(normalized.attachments))
            id
        } catch (e: Exception) {
            throw IllegalStateException("提交采购申请失败: ${e.message}", e)
        }
    }

    suspend fun resubmit(request: BudgetApprovalRequest) {
        val existing = approvalDao.getById(request.id) ?: error("Approval request not found")
        check(existing.status == BudgetApprovalStatus.WITHDRAWN || existing.status == BudgetApprovalStatus.REJECTED || existing.status == BudgetApprovalStatus.PENDING)
        validate(request)
        try {
            approvalDao.update(request.copy(status = BudgetApprovalStatus.PENDING, decisionNote = "", decidedAt = null, createdAt = existing.createdAt, updatedAt = System.currentTimeMillis()))
            attachmentRepository.replaceForOwner(AttachmentOwnerType.APPROVAL, request.id, AttachmentConverter.fromJson(request.attachments))
        } catch (e: Exception) {
            throw IllegalStateException("重新提交采购申请失败: ${e.message}", e)
        }
    }

    suspend fun submitWithFields(request: BudgetApprovalRequest, values: List<ApprovalFieldValue>, replacingId: Long? = null): Long = database.withTransaction {
        val id = if (replacingId == null || replacingId <= 0L) {
            submit(request)
        } else {
            resubmit(request.copy(id = replacingId))
            replacingId
        }
        approvalFieldValueDao.deleteForApproval(id)
        approvalFieldValueDao.upsertAll(values.filter { it.value.isNotBlank() }.map { it.copy(approvalId = id) })
        id
    }

    suspend fun deleteHistory(id: Long) = database.withTransaction {
        val approval = approvalDao.getById(id) ?: error("Approval request not found")
        check(approval.status != BudgetApprovalStatus.PENDING) { "Pending approvals cannot be deleted" }
        val linked = assetDao.getBySourceApprovalId(id)
        check(linked == null || linked.status == AssetStatus.LOST) { "Only approvals whose related asset is confirmed lost can be deleted" }
        if (linked != null) assetDao.clearSourceApprovalId(id, System.currentTimeMillis())
        approvalFieldValueDao.deleteForApproval(id)
        attachmentRepository.deleteForOwner(AttachmentOwnerType.APPROVAL, id)
        approvalDao.deleteById(id)
    }
    suspend fun withdraw(id: Long) {
        val existing = approvalDao.getById(id) ?: error("Approval request not found")
        check(existing.status == BudgetApprovalStatus.PENDING)
        approvalDao.update(existing.copy(status = BudgetApprovalStatus.WITHDRAWN, updatedAt = System.currentTimeMillis()))
    }

    suspend fun approve(id: Long, decisionNote: String = "") = decide(id, BudgetApprovalStatus.PURCHASE_IN_PROGRESS, decisionNote)
    suspend fun reject(id: Long, decisionNote: String = "") = decide(id, BudgetApprovalStatus.REJECTED, decisionNote)

    private suspend fun decide(id: Long, status: BudgetApprovalStatus, decisionNote: String) {
        database.withTransaction {
            val existing = approvalDao.getById(id) ?: error("Approval request not found")
            check(existing.status == BudgetApprovalStatus.PENDING)
            val now = System.currentTimeMillis()
            approvalDao.update(existing.copy(status = status, decisionNote = decisionNote.trim(), decidedAt = now, updatedAt = now))
            if (status == BudgetApprovalStatus.PURCHASE_IN_PROGRESS && existing.type == BudgetApprovalType.PURCHASE_BUDGET) {
                createAssetFromPurchase(existing, now)
            }
        }
    }

    private suspend fun createAssetFromPurchase(request: BudgetApprovalRequest, now: Long) {
        val categoryId = request.assetCategoryId ?: error("Purchase requests require an asset category")
        if (assetDao.getBySourceApprovalId(request.id) != null) return
        val purchaseDate = request.purchaseDate ?: now
        val assetId = assetDao.getMaxAssetId() + 1L
        assetDao.insertAsset(Asset(id = assetId, date = purchaseDate, amount = request.amount, status = AssetStatus.IN_PROGRESS, assetCategoryId = categoryId, categoryId = request.categoryId, name = request.itemName, specification = request.specification, quantity = request.quantity, purchaseDate = purchaseDate, sourceApprovalId = request.id, note = request.reason, attachments = request.attachments, createdAt = now, updatedAt = now, assetDirection = assetCategoryDao.getById(categoryId)?.direction?.name ?: AssetDirection.POSITIVE.name))
        val approvalValues = approvalFieldValueDao.getForApproval(request.id)
        assetFieldValueDao.upsertAll(approvalValues.map { AssetFieldValue(assetId, it.fieldId, it.value) })
        attachmentRepository.replaceForOwner(AttachmentOwnerType.ASSET, assetId, AttachmentConverter.fromJson(request.attachments))
    }

    private suspend fun validate(request: BudgetApprovalRequest) {
        require(request.amount > 0.0) { "Budget approval amount must be positive" }
        request.categoryId?.let { require(categoryDao.getById(it)?.type == TransactionType.EXPENSE) { "Approval category must be an expense category" } }
        if (request.type == BudgetApprovalType.PURCHASE_BUDGET) {
            require(request.categoryId != null) { "Purchase requests require an expense category" }
            require(request.assetCategoryId != null && assetCategoryDao.getById(request.assetCategoryId) != null) { "Purchase requests require an asset category" }
            require(request.reason.isNotBlank()) { "Purchase requests require a reason" }
            request.expectedPurchaseEndAt?.let { deadline -> require(request.purchaseDate == null || deadline >= request.purchaseDate) { "Purchase deadline must not be before purchase date" } }
        }
    }

    private fun monthKey(time: Long): String = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date(time))
}
