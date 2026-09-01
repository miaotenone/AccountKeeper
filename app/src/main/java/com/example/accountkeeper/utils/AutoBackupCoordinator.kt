package com.example.accountkeeper.utils

import android.app.Application
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.AssetCategoryRepository
import com.example.accountkeeper.data.repository.AttachmentRepository
import com.example.accountkeeper.data.repository.BillFileRepository
import com.example.accountkeeper.data.repository.BudgetApprovalRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class AutoBackupCoordinator @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val assetCategoryRepository: AssetCategoryRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val billFileRepository: BillFileRepository,
    private val attachmentRepository: AttachmentRepository,
    private val budgetApprovalRepository: BudgetApprovalRepository,
    private val settingsRepository: SettingsRepository
) {
    private val backupManager = BackupManager(application)

    suspend fun resetChainFromCurrentData() {
        val transactions = transactionRepository.getAllTransactions().first()
        val assets = assetRepository.getAllAssets().first()
        val categories = categoryRepository.getAllCategories().first()
        val assetCategories = assetCategoryRepository.getAll().first()
        val approvals = budgetApprovalRepository.getAll().first()
        val attachments = attachmentRepository.getAll().first()
        val budgets = budgetRepository.getAll().first()
        val billFiles = billFileRepository.getAll().first()
        val settings = settingsRepository.settingsFlow.first()
        withContext(Dispatchers.IO) {
            backupManager.deleteBackupChain()
            backupManager.createBaseBackup(
                transactions = transactions,
                assets = assets,
                categoryMap = categories.associate { it.id to it.name },
                budgets = budgets,
                billFiles = billFiles,
                assetCategoryMap = assetCategories.associate { it.id to it.name },
                assetCategories = assetCategories,
                approvals = approvals,
                attachments = attachments,
                settings = settings.toArchiveData()
            )
        }
    }

    suspend fun backupAfterDataChange() {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isAutoBackupEnabled) return

        val transactions = transactionRepository.getAllTransactions().first()
        val assets = assetRepository.getAllAssets().first()
        val categories = categoryRepository.getAllCategories().first()
        val categoryMap = categories.associate { it.id to it.name }
        val assetCategories = assetCategoryRepository.getAll().first()
        val approvals = budgetApprovalRepository.getAll().first()
        val attachments = attachmentRepository.getAll().first()
        val budgets = budgetRepository.getAll().first()
        val billFiles = billFileRepository.getAll().first()

        withContext(Dispatchers.IO) {
            if (!backupManager.hasBackupChain()) {
                backupManager.createBaseBackup(transactions, assets, categoryMap, budgets, billFiles, assetCategories.associate { it.id to it.name }, assetCategories, approvals, attachments, settings.toArchiveData())
                return@withContext
            }

            val latest = backupManager.restoreToStep(-1)
            if (!latest.success) {
                backupManager.createBaseBackup(transactions, assets, categoryMap, budgets, billFiles, assetCategories.associate { it.id to it.name }, assetCategories, approvals, attachments, settings.toArchiveData())
                return@withContext
            }

            val previousTransactions = latest.transactions.map { data ->
                Transaction(
                    id = data.id,
                    type = if (data.type.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = data.amount,
                    date = data.date,
                    categoryId = categories.firstOrNull { it.name == data.categoryName }?.id,
                    note = data.note,
                    attachments = AttachmentConverter.toJson(data.attachments)
                )
            }
            val previousAssets = latest.assets.map { data ->
                Asset(
                    id = data.id,
                    date = data.date,
                    amount = data.amount,
                    status = runCatching { AssetStatus.valueOf(data.status) }.getOrDefault(AssetStatus.NONE),
                    assetCategoryId = data.assetCategoryId,
                    categoryId = null,
                    name = data.name,
                    specification = data.specification,
                    quantity = data.quantity,
                    purchaseDate = data.purchaseDate,
                    sourceApprovalId = data.sourceApprovalId,
                    transactionId = data.transactionId,
                    targetPerson = data.targetPerson,
                    targetAccount = data.targetAccount,
                    note = data.note,
                    isCompleted = data.isCompleted,
                    attachments = AttachmentConverter.toJson(data.attachments),
                    createdAt = data.createdAt,
                    updatedAt = data.updatedAt,
                    assetRootType = data.assetRootType,
                    supplier = data.supplier,
                    location = data.location,
                    userOrDepartment = data.userOrDepartment,
                    warranty = data.warranty,
                    serviceStartDate = data.serviceStartDate,
                    serviceEndDate = data.serviceEndDate,
                    renewalCycle = data.renewalCycle,
                    accessUrl = data.accessUrl
                )
            }
            val previousBudgets = latest.budgets.map { data ->
                Budget(
                    monthKey = data.monthKey,
                    categoryId = data.categoryName?.let { name ->
                        categories.firstOrNull { it.name == name && it.type == TransactionType.EXPENSE }?.id
                    },
                    amount = data.amount,
                    createdAt = data.createdAt,
                    updatedAt = data.updatedAt
                )
            }

            backupManager.createDeltaBackup(
                previousTransactions = previousTransactions,
                previousAssets = previousAssets,
                currentTransactions = transactions,
                currentAssets = assets,
                categoryMap = categoryMap,
                budgets = budgets,
                previousBudgets = previousBudgets,
                maxKeep = settings.backupRetentionLimit,
                billFiles = billFiles,
                previousBillFiles = latest.billFiles,
                assetCategoryMap = assetCategories.associate { it.id to it.name },
                assetCategories = assetCategories,
                previousAssetCategories = latest.assetCategories,
                approvals = approvals,
                previousApprovals = latest.approvals,
                attachments = attachments,
                previousAttachments = latest.attachments,
                settings = settings.toArchiveData()
            )
        }
    }

    private fun com.example.accountkeeper.data.repository.AppSettings.toArchiveData() = SettingsData(
        isDarkMode = isDarkMode,
        language = language,
        currencySymbol = currencySymbol,
        isAutoBackupEnabled = isAutoBackupEnabled,
        backupRetentionLimit = backupRetentionLimit,
        swipeDeleteRequiresConfirm = swipeDeleteRequiresConfirm,
        isScheduledBackupEnabled = isScheduledBackupEnabled,
        scheduledBackupInterval = scheduledBackupInterval
    )
}
