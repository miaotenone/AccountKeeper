package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            categories.collect { list ->
                // Clean up any previously inserted English defaults that are cluttering the UI
                val defaultDeleted = list.filter { it.isDefault && it.name.matches(Regex(".*[a-zA-Z].*")) }
                defaultDeleted.forEach { categoryRepository.deleteCategory(it) }

                if (list.isEmpty() || defaultDeleted.isNotEmpty()) {
                    val defaultCategories = listOf(
                        Category(name = "餐饮美食", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "交通出行", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "日用购物", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "休闲娱乐", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "住房物业", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "水电煤气", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "家庭开支", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "医疗健康", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "教育培训", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "转出", type = TransactionType.EXPENSE, isDefault = true),

                        Category(name = "职业薪金", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "投资理财", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "兼职外快", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "红包礼金", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "转入", type = TransactionType.INCOME, isDefault = true)
                    )
                    defaultCategories.forEach { newCat -> 
                        if (list.none { it.name == newCat.name }) {
                            addCategory(newCat)
                        }
                    }
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
