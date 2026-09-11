package com.example.accountkeeper.utils

import android.app.Application
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.local.AppDatabase
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
        val count = settingsRepository.incrementAutoBackupChangeCount()
        if (count < settings.autoBackupChangeThreshold) return
        createFullBackup(settings)
        settingsRepository.resetAutoBackupChangeCount()
    }

    suspend fun backupOnSchedule() {
        createFullBackup(settingsRepository.settingsFlow.first())
    }

    private suspend fun createFullBackup(settings: com.example.accountkeeper.data.repository.AppSettings) {
        val transactions = transactionRepository.getAllTransactions().first()
        val assets = assetRepository.getAllAssets().first()
        val categories = categoryRepository.getAllCategories().first()
        val assetCategories = assetCategoryRepository.getAll().first()
        val approvals = budgetApprovalRepository.getAll().first()
        val attachments = attachmentRepository.getAll().first()
        val budgets = budgetRepository.getAll().first()
        val billFiles = billFileRepository.getAll().first()
        withContext(Dispatchers.IO) {
            backupManager.writeZipBackup(
                transactions = transactions,
                assets = assets,
                categoryMap = categories.associate { it.id to it.name },
                budgets = budgets,
                maxKeep = settings.backupRetentionLimit,
                isAuto = true,
                billFiles = billFiles,
                assetCategoryMap = assetCategories.associate { it.id to it.name },
                assetCategories = assetCategories,
                approvals = approvals,
                attachments = attachments,
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
