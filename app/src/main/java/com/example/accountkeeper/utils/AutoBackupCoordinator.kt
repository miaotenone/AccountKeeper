package com.example.accountkeeper.utils

import android.app.Application
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
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
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository
) {
    private val backupManager = BackupManager(application)

    suspend fun backupAfterDataChange() {
        val settings = settingsRepository.settingsFlow.first()
        if (!settings.isAutoBackupEnabled) return

        val transactions = transactionRepository.getAllTransactions().first()
        val assets = assetRepository.getAllAssets().first()
        val categories = categoryRepository.getAllCategories().first()
        val categoryMap = categories.associate { it.id to it.name }
        val budgets = budgetRepository.getAll().first()

        withContext(Dispatchers.IO) {
            if (!backupManager.hasBackupChain()) {
                backupManager.createBaseBackup(transactions, assets, categoryMap, budgets)
                return@withContext
            }

            val latest = backupManager.restoreToStep(-1)
            if (!latest.success) {
                backupManager.createBaseBackup(transactions, assets, categoryMap, budgets)
                return@withContext
            }

            val previousTransactions = latest.transactions.map { data ->
                Transaction(
                    id = data.id,
                    type = if (data.type.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE,
                    amount = data.amount,
                    date = data.date,
                    categoryId = categories.firstOrNull { it.name == data.categoryName }?.id,
                    note = data.note
                )
            }
            val previousAssets = latest.assets.map { data ->
                Asset(
                    id = data.id,
                    date = data.date,
                    amount = data.amount,
                    status = runCatching { AssetStatus.valueOf(data.status) }.getOrDefault(AssetStatus.NONE),
                    categoryId = categories.firstOrNull { it.name == data.categoryName }?.id,
                    targetPerson = data.targetPerson,
                    targetAccount = data.targetAccount,
                    note = data.note,
                    isCompleted = data.isCompleted,
                    attachments = AttachmentConverter.toJson(data.attachments),
                    createdAt = data.createdAt,
                    updatedAt = data.updatedAt
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
                maxKeep = settings.backupRetentionLimit
            )
        }
    }
}
