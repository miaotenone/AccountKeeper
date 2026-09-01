package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.BudgetRepository
import com.example.accountkeeper.utils.AutoBackupCoordinator
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : ViewModel() {
    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val currentList = categoryRepository.getAllCategoriesList()
            val duplicates = currentList.groupBy { it.name + "_" + it.type.name }.filter { it.value.size > 1 }
            for ((_, group) in duplicates) {
                val keep = group.firstOrNull { it.isDefault } ?: group.first()
                for (cat in group.filter { it.id != keep.id }) {
                    transactionRepository.updateTransactionCategory(cat.id, keep.id)
                    if (cat.type == TransactionType.EXPENSE) budgetRepository.deleteByCategory(cat.id)
                    categoryRepository.deleteCategory(cat)
                }
            }
            val updatedList = categoryRepository.getAllCategoriesList()
            defaultCategories().filterNot { defaultCat -> updatedList.any { it.name == defaultCat.name && it.type == defaultCat.type } }.forEach { categoryRepository.insertCategory(it) }
        }
    }

    fun addCategory(category: Category) = launchBackup { categoryRepository.insertCategory(category) }
    fun updateCategory(category: Category) = launchBackup { categoryRepository.updateCategory(category) }
    fun deleteCategory(category: Category) = launchBackup {
        if (category.type == TransactionType.EXPENSE) budgetRepository.deleteByCategory(category.id)
        categoryRepository.deleteCategory(category)
    }

    private fun launchBackup(action: suspend () -> Unit) = viewModelScope.launch {
        action()
        autoBackupCoordinator.backupAfterDataChange()
    }

    private fun defaultCategories() = listOf(
        Category(name = "餐饮美食", type = TransactionType.EXPENSE, isDefault = true), Category(name = "交通出行", type = TransactionType.EXPENSE, isDefault = true), Category(name = "服饰装扮", type = TransactionType.EXPENSE, isDefault = true), Category(name = "日用百货", type = TransactionType.EXPENSE, isDefault = true), Category(name = "休闲娱乐", type = TransactionType.EXPENSE, isDefault = true), Category(name = "文化教育", type = TransactionType.EXPENSE, isDefault = true), Category(name = "运动健康", type = TransactionType.EXPENSE, isDefault = true), Category(name = "美容美发", type = TransactionType.EXPENSE, isDefault = true), Category(name = "住房物业", type = TransactionType.EXPENSE, isDefault = true), Category(name = "水电煤气", type = TransactionType.EXPENSE, isDefault = true), Category(name = "数码电器", type = TransactionType.EXPENSE, isDefault = true), Category(name = "宠物花草", type = TransactionType.EXPENSE, isDefault = true), Category(name = "汽车飞机", type = TransactionType.EXPENSE, isDefault = true), Category(name = "家庭开支", type = TransactionType.EXPENSE, isDefault = true), Category(name = "转出", type = TransactionType.EXPENSE, isDefault = true),
        Category(name = "职业薪金", type = TransactionType.INCOME, isDefault = true), Category(name = "投资理财", type = TransactionType.INCOME, isDefault = true), Category(name = "兼职外快", type = TransactionType.INCOME, isDefault = true), Category(name = "红包礼金", type = TransactionType.INCOME, isDefault = true), Category(name = "二手闲置", type = TransactionType.INCOME, isDefault = true), Category(name = "退款报销", type = TransactionType.INCOME, isDefault = true), Category(name = "转入", type = TransactionType.INCOME, isDefault = true),
        Category(name = "借出", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "应收款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "预付款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "押金", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "代付款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "投资债权", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "实物资产", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "虚拟资产", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = true), Category(name = "借入", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = false), Category(name = "应付款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = false), Category(name = "欠款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = false), Category(name = "信用卡", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = false), Category(name = "贷款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = false), Category(name = "分期付款", type = TransactionType.ASSET, isDefault = true, isPositiveAsset = false)
    )
}
