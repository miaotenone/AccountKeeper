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
                if (list.isEmpty()) {
                    val defaultCategories = listOf(
                        Category(name = "Food & Drink", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Transportation", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Shopping", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Entertainment", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Housing", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Utilities", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Family", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Health", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Education", type = TransactionType.EXPENSE, isDefault = true),
                        Category(name = "Transfer Out", type = TransactionType.EXPENSE, isDefault = true),

                        Category(name = "Salary", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "Investment", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "Part-time", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "Red Packet", type = TransactionType.INCOME, isDefault = true),
                        Category(name = "Transfer In", type = TransactionType.INCOME, isDefault = true)
                    )
                    defaultCategories.forEach { addCategory(it) }
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
