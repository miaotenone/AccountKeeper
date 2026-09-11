package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetFieldType
import com.example.accountkeeper.data.model.localizedAssetFieldName
import com.example.accountkeeper.data.model.localizedAssetFieldType
import com.example.accountkeeper.data.model.AssetFieldDefinition
import com.example.accountkeeper.data.model.AssetDirection
import com.example.accountkeeper.data.model.sortedAssetCategories
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.components.AppSectionCard
import com.example.accountkeeper.ui.components.AppEmptyState

import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.AssetCategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
    onNavigateBack: () -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    assetCategoryViewModel: AssetCategoryViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.categories.collectAsState()
    val strings = LocalAppStrings.current
    val isChinese = strings.language == "界面语言" || strings.language == "zh"
    val assetCategories by assetCategoryViewModel.assetCategories.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Category?>(null) }
    
    var categoryNameInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var isNewAssetPositive by remember { mutableStateOf(true) }
    var selectedAssetCategory by remember { mutableStateOf<AssetCategoryEntity?>(null) }
    var showFieldDialog by remember { mutableStateOf(false) }
    var fieldName by remember { mutableStateOf("") }
    var fieldType by remember { mutableStateOf(AssetFieldType.TEXT) }
    var fieldRequired by remember { mutableStateOf(false) }
    var fieldOptions by remember { mutableStateOf("") }
    var editingField by remember { mutableStateOf<AssetFieldDefinition?>(null) }
    var fieldPendingDelete by remember { mutableStateOf<AssetFieldDefinition?>(null) }

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
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                categoryNameInput = ""
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.expense) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.income) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.asset) })
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (currentType == TransactionType.ASSET) {
                    items(sortedAssetCategories(assetCategories), key = { it.id }) { assetCategory ->
                        val isPositive = assetCategory.direction == AssetDirection.POSITIVE
                        ListItem(
                            headlineContent = { Text(assetCategory.name) },
                            supportingContent = {
                                SuggestionChip(
                                    onClick = { assetCategoryViewModel.toggleAssetCategoryDirection(assetCategory) },
                                    label = { Text(if (isPositive) strings.positiveAsset else strings.negativeAsset) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                        labelColor = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                            },
                            trailingContent = {
                                TextButton(onClick = { selectedAssetCategory = assetCategory; showFieldDialog = true }) {
                                    Text("编辑字段")
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                } else {
                    items(displayCategories, key = { it.id }) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            supportingContent = {
                                if (category.isDefault) Text(strings.defaultCategory, style = MaterialTheme.typography.bodySmall)
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            categoryNameInput = category.name
                                            nameError = null
                                            showRenameDialog = category
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.editTransaction, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { showDeleteDialog = category },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(strings.addCategory) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            if (currentType == TransactionType.ASSET) {
                                assetCategoryViewModel.addCategory(name, if (isNewAssetPositive) AssetDirection.POSITIVE else AssetDirection.NEGATIVE)
                            } else {
                                categoryViewModel.addCategory(Category(name = name, type = currentType, isDefault = false, isPositiveAsset = true))
                            }
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

        if (showFieldDialog) {
            selectedAssetCategory?.let { assetCategory ->
                val fields by assetCategoryViewModel.fieldsFor(assetCategory.id).collectAsState(emptyList())
                val isChinese = strings.language == "界面语言" || strings.language == "zh"
                val fieldNameLabel = if (isChinese) "字段名称" else "Field name"
                val fieldTypeLabel = if (isChinese) "字段类型" else "Field type"
                val requiredLabel = if (isChinese) "必填" else "Required"
                val optionsLabel = if (isChinese) "选项，每行一个" else "Options, one per line"
                val addFieldLabel = if (editingField == null) { if (isChinese) "新增字段" else "Add field" } else { if (isChinese) "保存字段" else "Save field" }
                val moveUpLabel = if (isChinese) "上移" else "Move up"
                val moveDownLabel = if (isChinese) "下移" else "Move down"
                AlertDialog(
                    onDismissRequest = { showFieldDialog = false },
                    title = { Text("${assetCategory.name} - 详情字段") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                fields.forEachIndexed { index, field ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${field.name} (${localizedAssetFieldType(field.type, isChinese)})${if (field.required) " *" else ""}", modifier = Modifier.weight(1f))
                                    Row {
                                        IconButton(enabled = index > 0, onClick = { assetCategoryViewModel.moveField(field, -1) }) { Icon(Icons.Default.ArrowUpward, moveUpLabel) }
                                        IconButton(enabled = index < fields.lastIndex, onClick = { assetCategoryViewModel.moveField(field, 1) }) { Icon(Icons.Default.ArrowDownward, moveDownLabel) }
                                        IconButton(onClick = { editingField = field; fieldName = field.name; fieldType = field.type; fieldRequired = field.required; fieldOptions = field.options }) { Icon(Icons.Default.Edit, strings.editTransaction) }
                                        IconButton(onClick = { fieldPendingDelete = field }) { Icon(Icons.Default.Delete, strings.delete) }
                                    }
                                }
                            }
                            }
                            OutlinedTextField(value = fieldName, onValueChange = { fieldName = it }, label = { Text(fieldNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            var typeExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                                OutlinedTextField(value = localizedAssetFieldType(fieldType, isChinese), onValueChange = {}, readOnly = true, label = { Text(fieldTypeLabel) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                    AssetFieldType.entries.forEach { option -> DropdownMenuItem(text = { Text(localizedAssetFieldType(option, isChinese)) }, onClick = { fieldType = option; typeExpanded = false }) }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = fieldRequired, onCheckedChange = { fieldRequired = it }); Text(requiredLabel) }
                            if (fieldType == AssetFieldType.SINGLE_CHOICE) OutlinedTextField(value = fieldOptions, onValueChange = { fieldOptions = it }, label = { Text(optionsLabel) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { if (fieldName.isNotBlank()) { val current = editingField; if (current == null) assetCategoryViewModel.addField(assetCategory.id, fieldName, fieldType, fieldRequired, fieldOptions) else assetCategoryViewModel.updateField(current.copy(name = fieldName, type = fieldType, required = fieldRequired, options = fieldOptions)); fieldName = ""; fieldOptions = ""; fieldRequired = false; fieldType = AssetFieldType.TEXT; editingField = null } }, modifier = Modifier.fillMaxWidth()) { Text(addFieldLabel) }
                            if (editingField != null) TextButton(onClick = { editingField = null; fieldName = ""; fieldOptions = ""; fieldRequired = false; fieldType = AssetFieldType.TEXT }) { Text(strings.cancel) }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showFieldDialog = false }) { Text(strings.ok) } }
                )
            }
        }

        fieldPendingDelete?.let { field ->
            AlertDialog(
                onDismissRequest = { fieldPendingDelete = null },
                title = { Text(if (isChinese) "删除详情字段" else "Delete detail field") },
                text = { Text(if (isChinese) "删除“${field.name}”后，已保存资产和申请中的对应字段值也会删除。" else "Deleting “${field.name}” will also remove its saved values from assets and applications.") },
                confirmButton = { TextButton(onClick = { assetCategoryViewModel.deleteField(field); fieldPendingDelete = null }) { Text(strings.ok) } },
                dismissButton = { TextButton(onClick = { fieldPendingDelete = null }) { Text(strings.cancel) } }
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

