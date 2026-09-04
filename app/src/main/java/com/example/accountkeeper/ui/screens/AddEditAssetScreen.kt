package com.example.accountkeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.AssetCategoryViewModel
import com.example.accountkeeper.ui.viewmodel.AssetViewModel
import com.example.accountkeeper.ui.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetScreen(
    assetId: Long = -1L,
    onNavigateBack: () -> Unit,
    viewModel: AssetViewModel = hiltViewModel(),
    assetCategoryViewModel: AssetCategoryViewModel = hiltViewModel(),
    budgetViewModel: BudgetViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrencySymbol.current
    val context = LocalContext.current
    val assetCategories by assetCategoryViewModel.assetCategories.collectAsState()
    val approvalRequests by budgetViewModel.approvalRequests.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val isEditing = assetId != -1L

    var existingAsset by remember { mutableStateOf<Asset?>(null) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var status by remember { mutableStateOf(AssetStatus.NONE) }
    var name by remember { mutableStateOf("") }
    var specification by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var rootType by remember { mutableStateOf(AssetRootType.PHYSICAL) }
    var assetCategoryId by remember { mutableStateOf<Long?>(null) }
    var sourceApprovalId by remember { mutableStateOf<Long?>(null) }
    var targetPerson by remember { mutableStateOf("") }
    var targetAccount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(emptyList<Attachment>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }

    var rootTypeExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var approvalExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(assetId) {
        if (isEditing) {
            viewModel.getAssetById(assetId)?.let { asset ->
                existingAsset = asset
                amount = asset.amount.toString()
                date = asset.date
                status = asset.status
                name = asset.name
                specification = asset.specification
                quantity = asset.quantity.toString()
                assetCategoryId = asset.assetCategoryId ?: asset.categoryId
                sourceApprovalId = asset.sourceApprovalId
                rootType = runCatching {
                    AssetRootType.valueOf(asset.assetRootType)
                }.getOrDefault(AssetRootType.PHYSICAL)
                targetPerson = asset.targetPerson
                targetAccount = asset.targetAccount
                note = asset.note
                attachments = AttachmentConverter.fromJson(asset.attachments)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            copyAttachmentToInternalStorage(context, it)?.let { attachment ->
                attachments = attachments + attachment
            }
        }
    }
    val selectedCategory = assetCategories.firstOrNull { it.id == assetCategoryId }
    val linkedApprovalIds = assets
        .filter { it.id != existingAsset?.id }
        .mapNotNull { it.sourceApprovalId }
        .toSet()
    val approvedPurchaseApprovals = approvalRequests.filter { request ->
        request.type == BudgetApprovalType.PURCHASE_BUDGET &&
            request.status == BudgetApprovalStatus.APPROVED &&
            request.id !in linkedApprovalIds
    }
    val selectedApproval = approvalRequests.firstOrNull { it.id == sourceApprovalId }
    val visibleCategories = assetCategories.filter {
        it.rootType == rootType && it.parentCategoryId != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) strings.editAsset else strings.addAsset,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.back)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.assets, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded }
                        ) {
                            OutlinedTextField(
                                value = assetStatusName(status, strings),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.assetStatus) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                listOf(AssetStatus.NONE, AssetStatus.OWNED, AssetStatus.NOT_OWNED, AssetStatus.IN_PROGRESS).forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(assetStatusName(option, strings)) },
                                        onClick = {
                                            status = option
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it; error = null },
                            label = { Text(strings.amount) },
                            prefix = { Text(currency) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = error != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(strings.itemName) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = specification,
                                onValueChange = { specification = it },
                                label = { Text(strings.specification) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = { Text(strings.quantity) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                        OutlinedTextField(
                            value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.date) },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, strings.date)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.assetCategory, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        ExposedDropdownMenuBox(
                            expanded = rootTypeExpanded,
                            onExpandedChange = { rootTypeExpanded = !rootTypeExpanded }
                        ) {
                            OutlinedTextField(
                                value = rootTypeLabel(rootType),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("一级类型") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rootTypeExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = rootTypeExpanded,
                                onDismissRequest = { rootTypeExpanded = false }
                            ) {
                                AssetRootType.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(rootTypeLabel(option)) },
                                        onClick = {
                                            rootType = option
                                            assetCategoryId = null
                                            rootTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: strings.selectCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.assetCategory) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                visibleCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            assetCategoryId = category.id
                                            error = null
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("来源审批", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(
                            expanded = approvalExpanded,
                            onExpandedChange = { approvalExpanded = !approvalExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedApproval?.let { "#${it.id} ${it.itemName.ifBlank { strings.approvalPurchaseBudget }}" }
                                    ?: strings.none,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.approvalCenter) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(approvalExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = approvalExpanded,
                                onDismissRequest = { approvalExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.none) },
                                    onClick = {
                                        sourceApprovalId = null
                                        approvalExpanded = false
                                    }
                                )
                                approvedPurchaseApprovals.forEach { approval ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("#${approval.id} ${approval.itemName.ifBlank { strings.approvalPurchaseBudget }}")
                                        },
                                        onClick = {
                                            sourceApprovalId = approval.id
                                            amount = approval.amount.toString()
                                            name = approval.itemName
                                            specification = approval.specification
                                            quantity = approval.quantity.toString()
                                            assetCategoryId = approval.assetCategoryId
                                            rootType = assetCategories.firstOrNull { it.id == approval.assetCategoryId }?.rootType
                                                ?: rootType
                                            status = AssetStatus.IN_PROGRESS
                                            error = null
                                            approvalExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = targetPerson,
                            onValueChange = { targetPerson = it },
                            label = { Text(strings.targetPerson) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = targetAccount,
                            onValueChange = { targetAccount = it },
                            label = { Text(strings.targetAccount) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(strings.note) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                AttachmentSection(
                    title = strings.attachments,
                    attachments = attachments,
                    onAddAttachment = { picker.launch(arrayOf("*/*")) },
                    onRemoveAttachment = { attachment ->
                        attachments = attachments.filterNot { it.id == attachment.id }
                    },
                    onPreviewAttachment = { attachment ->
                        previewAttachment = attachment
                    }
                )
            }

            error?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        val quantityValue = quantity.toDoubleOrNull()
                        error = when {
                            amountValue == null || amountValue <= 0.0 -> strings.enterValidAmount
                            quantityValue == null || quantityValue <= 0.0 -> strings.enterValidAmount
                            assetCategoryId == null -> strings.selectCategory
                            else -> null
                        }
                        if (error == null && amountValue != null && quantityValue != null) {
                            val now = System.currentTimeMillis()
                            val asset = Asset(
                                id = existingAsset?.id ?: now,
                                date = date,
                                amount = amountValue,
                                status = status,
                                assetCategoryId = assetCategoryId,
                                categoryId = selectedApproval?.categoryId ?: existingAsset?.categoryId,
                                name = name.trim(),
                                specification = specification.trim(),
                                quantity = quantityValue,
                                purchaseDate = selectedApproval?.purchaseDate
                                    ?: existingAsset?.purchaseDate
                                    ?: date,
                                sourceApprovalId = sourceApprovalId,
                                targetPerson = targetPerson,
                                targetAccount = targetAccount,
                                note = note,
                                isCompleted = existingAsset?.isCompleted ?: false,
                                attachments = AttachmentConverter.toJson(attachments),
                                createdAt = existingAsset?.createdAt ?: now,
                                updatedAt = now,
                                assetRootType = selectedCategory?.rootType?.name ?: rootType.name
                            )
                            if (isEditing) {
                                viewModel.updateAsset(asset)
                            } else {
                                viewModel.addAsset(asset)
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.save)
                }
            }
        }
    }

    previewAttachment?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            onDismiss = { previewAttachment = null }
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = state.selectedDateMillis ?: date
                    showDatePicker = false
                }) {
                    Text(strings.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun rootTypeLabel(type: AssetRootType): String = when (type) {
    AssetRootType.PHYSICAL -> "实物资产"
    AssetRootType.VIRTUAL -> "虚拟资产"
}

private fun assetStatusName(status: AssetStatus, strings: com.example.accountkeeper.ui.theme.AppStrings): String = when (status) {
    AssetStatus.NONE -> strings.none
    AssetStatus.OWNED -> strings.owned
    AssetStatus.NOT_OWNED -> strings.notOwned
    AssetStatus.LOST -> strings.lost
    AssetStatus.IN_PROGRESS -> strings.inProgress
    AssetStatus.TEMPORARILY_WITH_ME -> strings.owned
    AssetStatus.TEMPORARILY_WITH_OTHERS -> strings.notOwned
}
