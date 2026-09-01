package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.accountkeeper.data.local.AppDatabase
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.AttachmentEntity
import com.example.accountkeeper.data.model.AttachmentOwnerType
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.BillFileEntity
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetCategoryRepository
import com.example.accountkeeper.data.repository.AttachmentRepository
import com.example.accountkeeper.data.repository.BudgetApprovalRepository
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.BillFileRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.AppSettings
import com.example.accountkeeper.utils.AutoBackupCoordinator
import com.example.accountkeeper.utils.BackupManager
import com.example.accountkeeper.utils.ZipImportResult
import com.example.accountkeeper.utils.SettingsData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FinancialArchiveViewModel @Inject constructor(
    application: Application,
    private val database: AppDatabase,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val assetCategoryRepository: AssetCategoryRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val billFileRepository: BillFileRepository,
    private val attachmentRepository: AttachmentRepository,
    private val budgetApprovalRepository: BudgetApprovalRepository,
    private val settingsRepository: SettingsRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : AndroidViewModel(application) {
    private val backupManager = BackupManager(application)

    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assets: StateFlow<List<Asset>> = assetRepository.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets: StateFlow<List<Budget>> = budgetRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val billFiles: StateFlow<List<BillFileEntity>> = billFileRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun recordBillFile(file: java.io.File, ownerType: String = "BILL", ownerId: Long? = null, categoryId: Long? = null): Boolean {
        if (!file.exists()) return false
        val sha256 = backupManager.getFileSha256(file)
        if (billFileRepository.getBySha256(sha256) != null) return false
        val billFile = BillFileEntity(
            id = "bill_${sha256.take(16)}", ownerType = ownerType, ownerId = ownerId, categoryId = categoryId,
            fileName = file.name, filePath = file.absolutePath, mimeType = backupManager.getBillFileMimeType(file.name),
            fileSize = file.length(), sha256 = sha256,
            createdAt = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        billFileRepository.insert(billFile)
        syncBillFileAttachment(billFile)
        autoBackupCoordinator.backupAfterDataChange()
        return true
    }

    suspend fun createManualBackup(customName: String? = null): java.io.File? {
        val categories = categoryRepository.getAllCategoriesList()
        val assetCategories = assetCategoryRepository.getAll().first()
        val approvals = budgetApprovalRepository.getAll().first()
        val attachments = attachmentRepository.getAll().first()
        val settings = settingsRepository.settingsFlow.first()
        return backupManager.writeZipBackup(transactionRepository.getAllTransactions().first(), assetRepository.getAllAssets().first(), categories.associate { it.id to it.name }, budgetRepository.getAll().first(), isAuto = false, billFiles = billFileRepository.getAll().first(), assetCategoryMap = assetCategories.associate { it.id to it.name }, assetCategories = assetCategories, approvals = approvals, attachments = attachments, settings = settings.toArchiveData(), customName = customName)
    }

    fun export(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val categories = categoryRepository.getAllCategoriesList()
        val assetCategories = assetCategoryRepository.getAll().first()
        onResult(backupManager.exportZipToUri(uri, transactions.value, assets.value, categories.associate { it.id to it.name }, budgets.value, billFiles = billFileRepository.getAll().first(), assetCategoryMap = assetCategories.associate { it.id to it.name }, assetCategories = assetCategories, approvals = budgetApprovalRepository.getAll().first(), attachments = attachmentRepository.getAll().first(), settings = settingsRepository.settingsFlow.first().toArchiveData()))
    }

    fun import(uri: Uri, onResult: (String) -> Unit) = viewModelScope.launch { try { val result = backupManager.readZipBackup(uri); if (!result.success) { onResult(result.errorMessage ?: "Import failed"); return@launch }; importResult(result, onResult, true) } finally { backupManager.cleanupTempFiles() } }
    fun restoreManualBackup(file: java.io.File, onResult: (String) -> Unit) = viewModelScope.launch { try { val result = backupManager.readZipBackupFromFile(file); if (!result.success) { onResult(result.errorMessage ?: "Restore failed"); return@launch }; importResult(result, onResult, true) } finally { backupManager.cleanupTempFiles() } }
    fun restoreAutoBackupStep(targetStep: Int, onResult: (String) -> Unit) = viewModelScope.launch { try { val result = backupManager.restoreToStep(targetStep); if (!result.success) { onResult(result.errorMessage ?: "Restore failed"); return@launch }; importResult(ZipImportResult(result.transactions, result.assets, result.assetTypes, result.budgets, result.attachmentFiles, result.billFiles, result.billArchiveFiles, result.assetCategories, result.approvals, result.attachments, result.settings, true), onResult, true) } finally { backupManager.cleanupTempFiles() } }

    private suspend fun importResult(result: ZipImportResult, onResult: (String) -> Unit, replaceExisting: Boolean) {
        val protection = createProtectionSnapshot()
        val createdFiles = mutableListOf<java.io.File>()
        if (!backupManager.readZipBackupFromFile(protection).success) { onResult("Restore failed: protection snapshot is invalid"); return }
        try {
            validateBillFiles(result)
            database.withTransaction {
                if (replaceExisting) clearBusinessTables()
                val transactionCategories = categoryRepository.getAllCategoriesList().toMutableList()
                importAssetCategories(result); importTransactionCategories(result, transactionCategories)
                val refreshed = categoryRepository.getAllCategoriesList()
                importApprovals(result, refreshed)
                val assetCategories = assetCategoryRepository.getAll().first().toMutableList()
                var transactionsAdded = 0; var assetsAdded = 0
                result.transactions.forEach { data ->
                    if (transactionRepository.getTransactionById(data.id) != null) return@forEach
                    val type = if (data.type.equals("Income", true)) TransactionType.INCOME else TransactionType.EXPENSE
                    val categoryId = refreshed.firstOrNull { it.name.equals(data.categoryName, true) && it.type == type }?.id ?: refreshed.firstOrNull { it.type == type }?.id
                    val restored = data.attachments.map { it.copy(filePath = result.attachmentFiles[it.id]?.let { file -> backupManager.copyAttachmentToInternalStorage(it.id, file, it.fileName, createdFiles)?.filePath } ?: it.filePath) }
                    transactionRepository.insertTransaction(Transaction(data.id, type, data.amount, data.date, categoryId, data.note, com.example.accountkeeper.data.model.TransactionSource.MANUAL, AttachmentConverter.toJson(restored))); transactionsAdded++
                }
                result.assets.forEach { data ->
                    if (assetRepository.getAssetById(data.id) != null) return@forEach
                    val categoryId = data.assetCategoryId?.takeIf { assetCategoryRepository.getById(it) != null } ?: resolveAssetCategory(data.categoryName, data.assetRootType, assetCategories)
                    val restored = data.attachments.map { it.copy(filePath = result.attachmentFiles[it.id]?.let { file -> backupManager.copyAttachmentToInternalStorage(it.id, file, it.fileName, createdFiles)?.filePath } ?: it.filePath) }
                    assetRepository.insertAsset(Asset(data.id, data.date, data.amount, runCatching { AssetStatus.valueOf(data.status) }.getOrDefault(AssetStatus.NONE), categoryId, null, data.name, data.specification, data.quantity, data.purchaseDate, data.sourceApprovalId, data.transactionId, data.targetPerson, data.targetAccount, data.note, data.isCompleted, AttachmentConverter.toJson(restored), data.createdAt, data.updatedAt, data.assetRootType, data.supplier, data.location, data.userOrDepartment, data.warranty, data.serviceStartDate, data.serviceEndDate, data.renewalCycle, data.accessUrl)); assetsAdded++
                }
                importBudgets(result, refreshed); importAttachments(result, createdFiles); val billsAdded = importBillFiles(result, createdFiles); restoreLegacyBillFiles(result, createdFiles); result.settings?.let { settingsRepository.restore(it.toAppSettings()) }
                onResult("Imported $transactionsAdded transactions, $assetsAdded assets, and $billsAdded bill files")
            }
            autoBackupCoordinator.resetChainFromCurrentData(); protection.delete(); backupManager.clearProtectionSnapshots()
        } catch (error: Exception) {
            createdFiles.filter { it.exists() }.forEach { it.delete() }
            restoreProtectionSnapshot(protection)
            onResult("Restore failed: ${error.localizedMessage ?: error::class.java.simpleName}")
        }
    }

    private suspend fun createProtectionSnapshot(): java.io.File { val file = backupManager.createProtectionSnapshotFile(); val categories = categoryRepository.getAllCategoriesList(); val assetCategories = assetCategoryRepository.getAll().first(); check(backupManager.exportZipToFile(file, transactionRepository.getAllTransactions().first(), assetRepository.getAllAssets().first(), categories.associate { it.id to it.name }, budgetRepository.getAll().first(), billFileRepository.getAll().first(), assetCategories.associate { it.id to it.name }, assetCategories = assetCategories, approvals = budgetApprovalRepository.getAll().first(), attachments = attachmentRepository.getAll().first(), settings = settingsRepository.settingsFlow.first().toArchiveData())); return file }
    private suspend fun restoreProtectionSnapshot(file: java.io.File) {
        if (!file.exists()) return
        val snapshot = backupManager.readZipBackupFromFile(file)
        if (!snapshot.success) return
        val createdFiles = mutableListOf<java.io.File>()
        try {
            importResultWithoutProtection(snapshot, createdFiles)
        } catch (error: Exception) {
            createdFiles.filter { it.exists() }.forEach { it.delete() }
        }
    }
     private suspend fun importResultWithoutProtection(result: ZipImportResult, createdFiles: MutableCollection<java.io.File>) {
         database.withTransaction {
             clearBusinessTables()
             val categories = categoryRepository.getAllCategoriesList().toMutableList()
             importAssetCategories(result)
             importTransactionCategories(result, categories)
             val refreshed = categoryRepository.getAllCategoriesList()
             importApprovals(result, refreshed)
             importTransactionsAndAssets(result, refreshed, createdFiles)
             importBudgets(result, refreshed)
             importAttachments(result, createdFiles)
             importBillFiles(result, createdFiles)
             result.settings?.let { settingsRepository.restore(it.toAppSettings()) }
         }
     }

     private suspend fun importTransactionsAndAssets(result: ZipImportResult, categories: List<Category>, createdFiles: MutableCollection<java.io.File>) {
         result.transactions.forEach { data ->
             if (transactionRepository.getTransactionById(data.id) != null) return@forEach
             val type = if (data.type.equals("Income", true)) TransactionType.INCOME else TransactionType.EXPENSE
             val categoryId = categories.firstOrNull { it.name.equals(data.categoryName, true) && it.type == type }?.id
             val restored = data.attachments.map { attachment -> attachment.copy(filePath = result.attachmentFiles[attachment.id]?.let { file -> backupManager.copyAttachmentToInternalStorage(attachment.id, file, attachment.fileName, createdFiles)?.filePath } ?: attachment.filePath) }
             transactionRepository.insertTransaction(Transaction(data.id, type, data.amount, data.date, categoryId, data.note, com.example.accountkeeper.data.model.TransactionSource.MANUAL, AttachmentConverter.toJson(restored)))
         }
         val assetCategories = assetCategoryRepository.getAll().first().toMutableList()
         result.assets.forEach { data ->
             if (assetRepository.getAssetById(data.id) != null) return@forEach
             val categoryId = data.assetCategoryId?.takeIf { assetCategoryRepository.getById(it) != null } ?: resolveAssetCategory(data.categoryName, data.assetRootType, assetCategories)
             val restored = data.attachments.map { attachment -> attachment.copy(filePath = result.attachmentFiles[attachment.id]?.let { file -> backupManager.copyAttachmentToInternalStorage(attachment.id, file, attachment.fileName, createdFiles)?.filePath } ?: attachment.filePath) }
             assetRepository.insertAsset(Asset(data.id, data.date, data.amount, runCatching { AssetStatus.valueOf(data.status) }.getOrDefault(AssetStatus.NONE), categoryId, null, data.name, data.specification, data.quantity, data.purchaseDate, data.sourceApprovalId, data.transactionId, data.targetPerson, data.targetAccount, data.note, data.isCompleted, AttachmentConverter.toJson(restored), data.createdAt, data.updatedAt, data.assetRootType, data.supplier, data.location, data.userOrDepartment, data.warranty, data.serviceStartDate, data.serviceEndDate, data.renewalCycle, data.accessUrl))
         }
     }
    private suspend fun clearBusinessTables() { database.billFileDao().deleteAll(); database.attachmentDao().deleteAll(); database.budgetApprovalDao().deleteAll(); database.assetDao().deleteAllAssets(); database.transactionDao().deleteAllTransactions(); database.budgetDao().deleteAllBudgets(); database.budgetMonthDao().deleteAll(); database.assetCategoryDao().deleteAll(); database.categoryDao().deleteAllCategories() }
    private fun AppSettings.toArchiveData() = SettingsData(isDarkMode, language, currencySymbol, isAutoBackupEnabled, backupRetentionLimit, swipeDeleteRequiresConfirm, isScheduledBackupEnabled, scheduledBackupInterval)
    private fun SettingsData.toAppSettings() = AppSettings(isDarkMode, language, currencySymbol, isAutoBackupEnabled, backupRetentionLimit, swipeDeleteRequiresConfirm, isScheduledBackupEnabled, scheduledBackupInterval)
    private suspend fun importAssetCategories(result: ZipImportResult) { val pending = result.assetCategories.toMutableList(); while (pending.isNotEmpty()) { val ready = pending.filter { it.parentCategoryId == null || assetCategoryRepository.getById(it.parentCategoryId) != null }; if (ready.isEmpty()) break; ready.forEach { data -> if (assetCategoryRepository.getById(data.id) == null) assetCategoryRepository.insert(AssetCategoryEntity(data.id, data.name, runCatching { AssetRootType.valueOf(data.rootType) }.getOrDefault(AssetRootType.PHYSICAL), data.parentCategoryId, data.isDefault, data.createdAt, data.updatedAt)); pending.remove(data) } } }
    private suspend fun importApprovals(result: ZipImportResult, categories: List<Category>) { result.approvals.forEach { data -> if (budgetApprovalRepository.getById(data.id) != null) return@forEach; budgetApprovalRepository.restore(BudgetApprovalRequest(id = data.id, type = runCatching { BudgetApprovalType.valueOf(data.type) }.getOrDefault(BudgetApprovalType.BUDGET_ADJUSTMENT), categoryId = data.categoryName?.let { n -> categories.firstOrNull { it.name.equals(n, true) && it.type == TransactionType.EXPENSE }?.id }, assetCategoryId = data.assetCategoryId, amount = data.amount, purchaseDate = data.purchaseDate, reason = data.reason, itemName = data.itemName, specification = data.specification, quantity = data.quantity, attachments = data.attachments, status = runCatching { BudgetApprovalStatus.valueOf(data.status) }.getOrDefault(BudgetApprovalStatus.PENDING), decisionNote = data.decisionNote, createdAt = data.createdAt, updatedAt = data.updatedAt, decidedAt = data.decidedAt)) } }
    private suspend fun importAttachments(result: ZipImportResult, createdFiles: MutableCollection<java.io.File>) { result.attachments.forEach { data -> if (attachmentRepository.getById(data.id) != null) return@forEach; val extracted = result.attachmentFiles[data.id] ?: error("Missing attachment file: ${data.fileName}"); val restored = backupManager.copyAttachmentToInternalStorage(data.id, extracted, data.fileName, createdFiles) ?: error("Unable to restore attachment: ${data.fileName}"); check(backupManager.getFileSha256(java.io.File(restored.filePath)) == data.sha256) { "Attachment verification failed: ${data.fileName}" }; attachmentRepository.insert(AttachmentEntity(data.id, runCatching { AttachmentOwnerType.valueOf(data.ownerType) }.getOrDefault(AttachmentOwnerType.ASSET), data.ownerId, data.fileName, restored.filePath, data.mimeType, restored.fileSize, data.sha256, data.createdAt)) } }
    private suspend fun importTransactionCategories(result: ZipImportResult, current: MutableList<Category>) { result.transactions.forEach { data -> val type = if (data.type.equals("Income", true)) TransactionType.INCOME else TransactionType.EXPENSE; if (data.categoryName.isBlank() || current.any { it.name.equals(data.categoryName, true) && it.type == type }) return@forEach; categoryRepository.insertCategory(Category(name = data.categoryName, type = type)); current += Category(name = data.categoryName, type = type) } }
    private suspend fun resolveAssetCategory(name: String?, rootTypeName: String, categories: MutableList<AssetCategoryEntity>): Long? { if (name.isNullOrBlank()) return null; val root = runCatching { AssetRootType.valueOf(rootTypeName) }.getOrDefault(AssetRootType.PHYSICAL); categories.firstOrNull { it.name.equals(name, true) && it.rootType == root }?.let { return it.id }; val entity = AssetCategoryEntity(name = name, rootType = root); val id = assetCategoryRepository.insert(entity); categories += entity.copy(id = id); return id }
    private suspend fun importBudgets(result: ZipImportResult, categories: List<Category>) { result.budgets.forEach { data -> val categoryId = data.categoryName?.let { n -> categories.firstOrNull { it.name.equals(n, true) && it.type == TransactionType.EXPENSE }?.id }; if (data.categoryName != null && categoryId == null) return@forEach; val existing = budgetRepository.getByMonthList(data.monthKey).firstOrNull { it.categoryId == categoryId }; val budget = Budget(monthKey = data.monthKey, categoryId = categoryId, amount = data.amount.coerceAtLeast(0.0), createdAt = data.createdAt, updatedAt = data.updatedAt); if (existing == null) budgetRepository.insert(budget) else budgetRepository.update(budget.copy(id = existing.id, createdAt = existing.createdAt)); budgetRepository.markMonthInitialized(data.monthKey) } }
     private suspend fun importBillFiles(result: ZipImportResult, createdFiles: MutableCollection<java.io.File>): Int {
         var imported = 0
         val categories = categoryRepository.getAllCategoriesList()
         result.billFiles.forEach { data ->
             if (billFileRepository.getById(data.id) != null) return@forEach
             val archiveName = data.archiveFileName.ifBlank { data.fileName }
             val extracted = result.billArchiveFiles[archiveName] ?: error("Missing bill file: ${data.fileName}")
             val restored = backupManager.copyBillFileToInternalStorage(extracted, data.fileName, createdFiles) ?: error("Unable to restore bill file: ${data.fileName}")
             check(backupManager.getFileSha256(restored) == data.sha256) { "Bill file verification failed: ${data.fileName}" }
             val categoryId = data.categoryName?.let { name -> categories.firstOrNull { it.name.equals(name, true) && it.type == TransactionType.EXPENSE }?.id } ?: data.categoryId?.takeIf { id -> categories.any { it.id == id } }
             val bill = BillFileEntity(data.id, data.ownerType, data.ownerId, categoryId, data.fileName, restored.absolutePath, data.mimeType.ifBlank { backupManager.getBillFileMimeType(data.fileName) }, restored.length(), data.sha256, data.createdAt)
             billFileRepository.insert(bill)
             syncBillFileAttachment(bill)
             imported++
         }
         return imported
     }
    private fun restoreLegacyBillFiles(result: ZipImportResult, createdFiles: MutableCollection<java.io.File>) { val tracked = result.billFiles.map { it.archiveFileName.ifBlank { it.fileName } }.toSet(); result.billArchiveFiles.forEach { (name, file) -> if (name !in tracked) backupManager.copyBillFileToInternalStorage(file, name, createdFiles) } }
    private suspend fun syncBillFileAttachment(bill: BillFileEntity) { attachmentRepository.insert(AttachmentEntity("BILL_${bill.id}", AttachmentOwnerType.BILL, bill.id.hashCode().toLong() and Long.MAX_VALUE, bill.fileName, bill.filePath, bill.mimeType, bill.fileSize, bill.sha256, bill.createdAt)) }
    private fun validateBillFiles(result: ZipImportResult) { result.billFiles.forEach { data -> val archiveName = data.archiveFileName.ifBlank { data.fileName }; val file = result.billArchiveFiles[archiveName] ?: error("Missing bill file: ${data.fileName}"); check(backupManager.getFileSha256(file) == data.sha256) { "Bill file verification failed: ${data.fileName}" } }
    }
    suspend fun deleteBillFile(file: java.io.File): Boolean { val indexed = billFileRepository.getAll().first().filter { it.filePath == file.absolutePath || it.filePath == file.canonicalPath }; val deleted = backupManager.deleteBillFile(file); indexed.forEach { billFileRepository.deleteById(it.id); attachmentRepository.delete(AttachmentEntity("BILL_${it.id}", AttachmentOwnerType.BILL, it.id.hashCode().toLong() and Long.MAX_VALUE, it.fileName, it.filePath, it.mimeType, it.fileSize, it.sha256, it.createdAt)) }; if (!deleted && indexed.isEmpty()) return false; autoBackupCoordinator.backupAfterDataChange(); return true }
}
