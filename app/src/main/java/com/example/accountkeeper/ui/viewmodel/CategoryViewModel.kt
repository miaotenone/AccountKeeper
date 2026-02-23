package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
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
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // 1. One-time deduplication to clean up prior bugs
            val currentList = categoryRepository.getAllCategoriesList()
            val duplicates = currentList.groupBy { it.name + "_" + it.type.name }.filter { it.value.size > 1 }
            for ((_, group) in duplicates) {
                val keep = group.firstOrNull { it.isDefault } ?: group.first()
                val toDelete = group.filter { it.id != keep.id }
                for (cat in toDelete) {
                    transactionRepository.updateTransactionCategory(cat.id, keep.id)
                    categoryRepository.deleteCategory(cat)
                }
            }
            
            // 2. Populate rich default categories if database is empty
            val updatedList = categoryRepository.getAllCategoriesList()
            if (updatedList.isEmpty()) {
                val defaultCategories = listOf(
                    Category(name = "餐饮美食", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "交通出行", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "服饰装扮", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "日用百货", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "休闲娱乐", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "文化教育", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "运动健康", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "美容美发", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "住房物业", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "水电煤气", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "数码电器", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "宠物花草", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "汽车飞机", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "家庭开支", type = TransactionType.EXPENSE, isDefault = true),
                    Category(name = "转出", type = TransactionType.EXPENSE, isDefault = true),

                    Category(name = "职业薪金", type = TransactionType.INCOME, isDefault = true),
                    Category(name = "投资理财", type = TransactionType.INCOME, isDefault = true),
                    Category(name = "兼职外快", type = TransactionType.INCOME, isDefault = true),
                    Category(name = "红包礼金", type = TransactionType.INCOME, isDefault = true),
                    Category(name = "二手闲置", type = TransactionType.INCOME, isDefault = true),
                    Category(name = "退款报销", type = TransactionType.INCOME, isDefault = true),
                    Category(name = "转入", type = TransactionType.INCOME, isDefault = true)
                )
                defaultCategories.forEach { newCat -> 
                    categoryRepository.insertCategory(newCat)
                }
            }
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
