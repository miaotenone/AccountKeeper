package com.example.accountkeeper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.repository.AssetCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetCategoryViewModel @Inject constructor(
    private val repository: AssetCategoryRepository
) : ViewModel() {
    val assetCategories: StateFlow<List<AssetCategoryEntity>> =
        repository.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
        }
    }

    fun getByRootType(rootType: AssetRootType) = repository.getChildrenByRootType(rootType)
}
