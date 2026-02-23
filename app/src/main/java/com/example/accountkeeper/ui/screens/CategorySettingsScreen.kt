package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    val currentType = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME
    val displayCategories = categories.filter { it.type == currentType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                categoryNameInput = ""
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.expense) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.income) })
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(displayCategories, key = { it.id }) { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        supportingContent = { if (category.isDefault) Text("默认分类") else null },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { 
                                    categoryNameInput = category.name
                                    nameError = null
                                    showRenameDialog = category
                                }) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                                IconButton(onClick = { showDeleteDialog = category }) { 
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) 
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
                title = { Text("新增分类") },
                text = {
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { 
                            categoryNameInput = it 
                            nameError = null
                        },
                        label = { Text("分类名称") },
                        singleLine = true,
                        isError = nameError != null,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        supportingText = { nameError?.let { Text(it) } }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = categoryNameInput.trim()
                        if (name.isEmpty()) {
                            nameError = "名称不能为空"
                        } else if (categories.any { it.name.equals(name, ignoreCase = true) }) {
                            nameError = "此分类名称已存在"
                        } else {
                            categoryViewModel.addCategory(Category(name = name, type = currentType, isDefault = false))
                            showAddDialog = false
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
                title = { Text("重命名分类") },
                text = {
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { 
                            categoryNameInput = it 
                            nameError = null
                        },
                        label = { Text("新名称") },
                        singleLine = true,
                        isError = nameError != null,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        supportingText = { nameError?.let { Text(it) } }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val name = categoryNameInput.trim()
                        if (name.isEmpty()) {
                            nameError = "名称不能为空"
                        } else if (name != category.name && categories.any { it.name.equals(name, ignoreCase = true) }) {
                            nameError = "此分类名称已存在"
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
                title = { Text("删除分类") },
                text = { Text("确定要删除自定义分类 \"${category.name}\" 吗？此操作无法撤销。") },
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
