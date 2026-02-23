package com.example.accountkeeper.data.repository

import com.example.accountkeeper.data.local.CategoryDao
import com.example.accountkeeper.data.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun getAllCategoriesList(): List<Category> = categoryDao.getAllCategoriesList()
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
