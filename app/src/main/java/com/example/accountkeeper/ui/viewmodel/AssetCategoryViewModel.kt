package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.local.AssetFieldDefinitionDao
import com.example.accountkeeper.data.model.AssetCategoryDefaults
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetDirection
import com.example.accountkeeper.data.model.AssetFieldDefinition
import com.example.accountkeeper.data.model.AssetFieldType
import com.example.accountkeeper.data.model.sortedAssetCategories
import com.example.accountkeeper.data.repository.AssetCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetCategoryViewModel @Inject constructor(
    private val repository: AssetCategoryRepository,
    private val fieldDao: AssetFieldDefinitionDao
) : ViewModel() {
    val assetCategories: StateFlow<List<AssetCategoryEntity>> = repository.getAll()
        .map(::sortedAssetCategories)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch { repository.ensureDefaults() } }

    fun addCategory(name: String, direction: AssetDirection = AssetDirection.POSITIVE) = viewModelScope.launch {
        repository.insert(AssetCategoryEntity(name = name.trim(), direction = direction))
    }

    fun toggleAssetCategoryDirection(category: AssetCategoryEntity) = viewModelScope.launch {
        repository.setCategoryDirection(category, AssetCategoryDefaults.opposite(category.direction))
    }

    fun fieldsFor(categoryId: Long) = fieldDao.getForCategory(categoryId)
    fun addField(categoryId: Long, name: String, type: AssetFieldType, required: Boolean, options: String = "") = viewModelScope.launch { fieldDao.insert(AssetFieldDefinition(assetCategoryId = categoryId, name = name.trim(), type = type, required = required, options = options.trim(), sortOrder = fieldDao.getForCategoryList(categoryId).size)) }
    fun updateField(field: AssetFieldDefinition) = viewModelScope.launch { fieldDao.update(field.copy(name = field.name.trim(), updatedAt = System.currentTimeMillis())) }
    fun deleteField(field: AssetFieldDefinition) = viewModelScope.launch { fieldDao.delete(field) }
    fun moveField(field: AssetFieldDefinition, direction: Int) = viewModelScope.launch {
        val fields = fieldDao.getForCategoryList(field.assetCategoryId)
        val index = fields.indexOfFirst { it.id == field.id }
        val targetIndex = (index + direction).takeIf { it in fields.indices } ?: return@launch
        val target = fields[targetIndex]
        fieldDao.update(field.copy(sortOrder = target.sortOrder, updatedAt = System.currentTimeMillis()))
        fieldDao.update(target.copy(sortOrder = field.sortOrder, updatedAt = System.currentTimeMillis()))
    }
}