package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.Category
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
                if (list.isEmpty()) {
                    val defaults = listOf(
                        Category(name = "餐饮", type = com.example.accountkeeper.data.model.TransactionType.EXPENSE, isDefault = true),
                        Category(name = "交通", type = com.example.accountkeeper.data.model.TransactionType.EXPENSE, isDefault = true),
                        Category(name = "购物", type = com.example.accountkeeper.data.model.TransactionType.EXPENSE, isDefault = true),
                        Category(name = "医疗", type = com.example.accountkeeper.data.model.TransactionType.EXPENSE, isDefault = true),
                        Category(name = "工资", type = com.example.accountkeeper.data.model.TransactionType.INCOME, isDefault = true),
                        Category(name = "理财", type = com.example.accountkeeper.data.model.TransactionType.INCOME, isDefault = true)
                    )
                    defaults.forEach { addCategory(it) }
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
