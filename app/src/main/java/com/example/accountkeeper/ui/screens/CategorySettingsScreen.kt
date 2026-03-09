package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
    onNavigateBack: () -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.categories.collectAsState()
    val strings = LocalAppStrings.current
    var selectedTab by remember { mutableStateOf(0) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }
    
    var categoryNameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isNewAssetPositive by remember { mutableStateOf(true) }

    val currentType = when (selectedTab) {
        0 -> TransactionType.EXPENSE
        1 -> TransactionType.INCOME
        else -> TransactionType.ASSET
    }
    val displayCategories = categories.filter { it.type == currentType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.categoryManagement) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                categoryNameInput = ""
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.expense) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.income) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.asset) })
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(displayCategories, key = { it.id }) { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        supportingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (category.isDefault) Text(strings.defaultCategory)
                                // Show asset type badge for ASSET categories
                                if (currentType == TransactionType.ASSET) {
                                    SuggestionChip(
                                        onClick = {
                                            // Toggle asset type
                                            categoryViewModel.updateCategory(
                                                category.copy(isPositiveAsset = !category.isPositiveAsset)
                                            )
                                        },
                                        label = { 
                                            Text(
                                                if (category.isPositiveAsset) strings.positiveAsset 
                                                else strings.negativeAsset
                                            ) 
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (category.isPositiveAsset) 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.errorContainer,
                                            labelColor = if (category.isPositiveAsset) 
                                                MaterialTheme.colorScheme.onPrimaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { 
                                    categoryNameInput = category.name
                                    nameError = null
                                    showRenameDialog = category
                                }) { Icon(Icons.Default.Edit, contentDescription = strings.editTransaction) }
                                IconButton(onClick = { showDeleteDialog = category }) { 
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error) 
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(strings.addCategory) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { 
                                categoryNameInput = it 
                                nameError = null
                            },
                            label = { Text(strings.categoryName) },
                            singleLine = true,
                            isError = nameError != null,
                            shape = RoundedCornerShape(12.dp),
                            supportingText = { nameError?.let { Text(it) } }
                        )
                        
                        // Asset type selection for ASSET categories
                        if (currentType == TransactionType.ASSET) {
                            Text(
                                text = strings.assetStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isNewAssetPositive,
                                    onClick = { isNewAssetPositive = true },
                                    label = { Text(strings.positiveAsset) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                                FilterChip(
                                    selected = !isNewAssetPositive,
                                    onClick = { isNewAssetPositive = false },
                                    label = { Text(strings.negativeAsset) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = categoryNameInput.trim()
                        if (name.isEmpty()) {
                            nameError = strings.nameEmptyError
                        } else if (categories.any { it.name.equals(name, ignoreCase = true) }) {
                            nameError = strings.nameExistsError
                        } else {
                            categoryViewModel.addCategory(Category(
                                name = name, 
                                type = currentType, 
                                isDefault = false,
                                isPositiveAsset = if (currentType == TransactionType.ASSET) isNewAssetPositive else true
                            ))
                            showAddDialog = false
                            isNewAssetPositive = true // Reset for next use
                        }
                    }) { Text(strings.ok) }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
                }
            )
        }

        showRenameDialog?.let { category ->
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text(strings.renameCategory) },
                text = {
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { 
                            categoryNameInput = it 
                            nameError = null
                        },
                        label = { Text(strings.newName) },
                        singleLine = true,
                        isError = nameError != null,
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { nameError?.let { Text(it) } }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = categoryNameInput.trim()
                        if (name.isEmpty()) {
                            nameError = strings.nameEmptyError
                        } else if (name != category.name && categories.any { it.name.equals(name, ignoreCase = true) }) {
                            nameError = strings.nameExistsError
                        } else if (name != category.name) {
                            categoryViewModel.updateCategory(category.copy(name = name))
                            showRenameDialog = null
                        } else {
                            showRenameDialog = null // Name didn't change
                        }
                    }) { Text(strings.ok) }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) { Text(strings.cancel) }
                }
            )
        }

        showDeleteDialog?.let { category ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text(strings.deleteCategory) },
                text = { Text(strings.deleteCategoryConfirm.replace("{name}", category.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        categoryViewModel.deleteCategory(category)
                        showDeleteDialog = null
                    }) { Text(strings.ok) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text(strings.cancel) }
                }
            )
        }
    }
}