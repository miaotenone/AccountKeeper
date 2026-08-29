package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.BackupManager
import com.example.accountkeeper.utils.AutoBackupCoordinator
import com.example.accountkeeper.utils.ZipImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FinancialArchiveViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : AndroidViewModel(application) {
    private val backupManager = BackupManager(application)
    val transactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val assets: StateFlow<List<Asset>> = assetRepository.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = budgetRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun export(uri: Uri, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        val categories = categoryRepository.getAllCategoriesList()
        onResult(backupManager.exportZipToUri(
            uri = uri,
            transactions = transactions.value,
            assets = assets.value,
            categoryMap = categories.associate { it.id to it.name },
            budgets = budgets.value
        ))
    }

    fun import(uri: Uri, onResult: (String) -> Unit) = viewModelScope.launch {
        try {
        val result = backupManager.readZipBackup(uri)
        if (!result.success) {
            onResult(result.errorMessage ?: "Import failed")
            return@launch
        }
        val categories = categoryRepository.getAllCategoriesList().toMutableList()
        importCategories(result, categories)
        val refreshedCategories = categoryRepository.getAllCategoriesList()
        var transactionsAdded = 0
        var assetsAdded = 0

        result.transactions.forEach { data ->
            if (transactionRepository.getTransactionById(data.id) != null) return@forEach
            val type = if (data.type.equals("Income", true)) TransactionType.INCOME else TransactionType.EXPENSE
            val categoryId = refreshedCategories.firstOrNull { it.name.equals(data.categoryName, true) && it.type == type }?.id
                ?: refreshedCategories.firstOrNull { it.type == type }?.id
            transactionRepository.insertTransaction(Transaction(data.id, type, data.amount, data.date, categoryId, data.note))
            transactionsAdded++
        }

        result.assets.forEach { data ->
            if (assetRepository.getAssetById(data.id) != null) return@forEach
            val categoryId = refreshedCategories.firstOrNull {
                it.name.equals(data.categoryName, true) && it.type == TransactionType.ASSET
            }?.id
            val attachments = data.attachments.map { attachment ->
                result.attachmentFiles[attachment.id]?.let { file ->
                    backupManager.copyAttachmentToInternalStorage(attachment.id, file, attachment.fileName)
                } ?: attachment
            }
            assetRepository.insertAsset(Asset(
                id = data.id,
                date = data.date,
                amount = data.amount,
                status = runCatching { AssetStatus.valueOf(data.status) }.getOrDefault(AssetStatus.NONE),
                categoryId = categoryId,
                targetPerson = data.targetPerson,
                targetAccount = data.targetAccount,
                note = data.note,
                isCompleted = data.isCompleted,
                attachments = AttachmentConverter.toJson(attachments),
                createdAt = data.createdAt,
                updatedAt = data.updatedAt
            ))
            assetsAdded++
        }

        importBudgets(result, refreshedCategories)
        autoBackupCoordinator.backupAfterDataChange()
        onResult("Imported $transactionsAdded transactions and $assetsAdded assets")
        } finally {
            backupManager.cleanupTempFiles()
        }
    }

    private suspend fun importCategories(result: ZipImportResult, current: MutableList<Category>) {
        result.transactions.forEach { data ->
            val type = if (data.type.equals("Income", true)) TransactionType.INCOME else TransactionType.EXPENSE
            if (data.categoryName.isBlank() || current.any { it.name.equals(data.categoryName, true) && it.type == type }) return@forEach
            categoryRepository.insertCategory(Category(name = data.categoryName, type = type))
            current += Category(name = data.categoryName, type = type)
        }
        result.assets.forEach { data ->
            val name = data.categoryName ?: return@forEach
            if (name.isBlank() || current.any { it.name.equals(name, true) && it.type == TransactionType.ASSET }) return@forEach
            categoryRepository.insertCategory(Category(name = name, type = TransactionType.ASSET))
            current += Category(name = name, type = TransactionType.ASSET)
        }
    }

    private suspend fun importBudgets(result: ZipImportResult, categories: List<Category>) {
        result.budgets.forEach { data ->
            val categoryId = data.categoryName?.let { name ->
                categories.firstOrNull { it.name.equals(name, true) && it.type == TransactionType.EXPENSE }?.id
            }
            if (data.categoryName != null && categoryId == null) return@forEach
            val existing = budgetRepository.getByMonthList(data.monthKey).firstOrNull { it.categoryId == categoryId }
            val budget = Budget(
                monthKey = data.monthKey,
                categoryId = categoryId,
                amount = data.amount.coerceAtLeast(0.0),
                createdAt = data.createdAt,
                updatedAt = data.updatedAt
            )
            if (existing == null) budgetRepository.insert(budget)
            else budgetRepository.update(budget.copy(id = existing.id, createdAt = existing.createdAt))
            budgetRepository.markMonthInitialized(data.monthKey)
        }
    }
}
