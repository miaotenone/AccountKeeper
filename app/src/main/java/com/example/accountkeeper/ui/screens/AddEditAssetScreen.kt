package com.example.accountkeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetDirection
import com.example.accountkeeper.data.model.sortedAssetLeafCategories
import com.example.accountkeeper.data.model.AssetFieldDefinition
import com.example.accountkeeper.data.model.localizedAssetFieldName
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.data.model.AssetFieldType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.components.AppSectionCard
import com.example.accountkeeper.ui.components.AppPrimaryButton

import com.example.accountkeeper.ui.viewmodel.AssetCategoryViewModel
import com.example.accountkeeper.ui.viewmodel.AssetViewModel
import com.example.accountkeeper.ui.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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
    var direction by remember { mutableStateOf(AssetDirection.POSITIVE) }
    var assetCategoryId by remember { mutableStateOf<Long?>(null) }
    var sourceApprovalId by remember { mutableStateOf<Long?>(null) }
    var transactionId by remember { mutableStateOf<Long?>(null) }
    var useSourceApproval by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(emptyList<Attachment>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }
    var fieldValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val fieldDefinitions by remember(assetCategoryId) { assetCategoryId?.let { assetCategoryViewModel.fieldsFor(it) } ?: flowOf(emptyList()) }.collectAsState(emptyList())

    var directionExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    var approvalExpanded by remember { mutableStateOf(false) }
    var transactionExpanded by remember { mutableStateOf(false) }
    var candidateTransactions by remember { mutableStateOf(emptyList<com.example.accountkeeper.data.model.Transaction>()) }
    val scope = rememberCoroutineScope()

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
                transactionId = asset.transactionId
                useSourceApproval = asset.sourceApprovalId != null
                direction = runCatching {
                    AssetDirection.fromStoredValue(asset.assetDirection)
                }.getOrDefault(AssetDirection.POSITIVE)
                note = asset.note
                attachments = AttachmentConverter.fromJson(asset.attachments)
                fieldValues = viewModel.getAssetFieldValues(asset.id).associate { it.fieldId to it.value }
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
            (request.status == BudgetApprovalStatus.APPROVED || request.status == BudgetApprovalStatus.PURCHASE_IN_PROGRESS) &&
            request.id !in linkedApprovalIds
    }
    val selectedApproval = approvalRequests.firstOrNull { it.id == sourceApprovalId }
    val selectedTransaction = candidateTransactions.firstOrNull { it.id == transactionId }
    LaunchedEffect(assetCategoryId, amount, date, existingAsset?.purchaseDate, transactionId, status) {
        candidateTransactions = if (status == AssetStatus.IN_PROGRESS) {
            viewModel.availableExpenseTransactions(
                existingAsset?.categoryId ?: selectedApproval?.categoryId,
                amount.toDoubleOrNull() ?: 0.0,
                existingAsset?.purchaseDate ?: selectedApproval?.purchaseDate ?: date,
                transactionId
            )
        } else emptyList()
    }
    LaunchedEffect(sourceApprovalId) {
        sourceApprovalId?.let { approvalId -> fieldValues = budgetViewModel.getApprovalFieldValues(approvalId).associate { it.fieldId to it.value } }
    }
    val visibleCategories = sortedAssetLeafCategories(assetCategories)

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            if (isEditing) strings.editAsset else strings.addAsset,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.back)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PremiumDateSelector(
                    date = date,
                    onClick = { showDatePicker = true },
                    strings = strings,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                PremiumAmountInput(
                    amountText = amount,
                    onAmountChange = { amount = it; error = null },
                    currency = currency,
                    strings = strings,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                AssetFormCard {
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                            listOf(AssetStatus.NONE, AssetStatus.IN_PROGRESS, AssetStatus.OWNED, AssetStatus.LOST).forEach { option ->
                                DropdownMenuItem(text = { Text(assetStatusName(option, strings)) }, onClick = {
                                    status = option
                                    statusExpanded = false
                                })
                            }
                        }
                    }
                }
            }

            item {
                AssetFormCard {
                    Text(strings.assetCategory, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                        direction = category.direction
                                        categoryExpanded = false
                                        fieldValues = emptyMap()
                                        error = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (assetCategoryId != null && fieldDefinitions.isNotEmpty()) {
                item {
                    AssetDetailsCard(
                        fields = fieldDefinitions,
                        values = fieldValues,
                        onValueChange = { fieldId, value -> fieldValues = fieldValues + (fieldId to value) }
                    )
                }
            }
            item {
                AssetFormCard {
                    Text("来源审批", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("是否选择来源审批")
                        Switch(
                            checked = useSourceApproval,
                            onCheckedChange = { enabled ->
                                useSourceApproval = enabled
                                if (!enabled) sourceApprovalId = null
                            }
                        )
                    }
                    if (useSourceApproval) {
                        ExposedDropdownMenuBox(
                            expanded = approvalExpanded,
                            onExpandedChange = { approvalExpanded = !approvalExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedApproval?.let { "#${it.id} ${it.reason.ifBlank { strings.approvalPurchaseBudget }}" } ?: strings.selectCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.approvalCenter) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(approvalExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = approvalExpanded,
                                onDismissRequest = { approvalExpanded = false }
                            ) {
                                approvedPurchaseApprovals.forEach { approval ->
                                    DropdownMenuItem(
                                        text = { Text("#${approval.id} ${approval.reason.ifBlank { strings.approvalPurchaseBudget }}") },
                                        onClick = {
                                            sourceApprovalId = approval.id
                                            amount = approval.amount.toString()
                                            date = approval.purchaseDate ?: date
                                            assetCategoryId = approval.assetCategoryId
                                            direction = assetCategories.firstOrNull { it.id == approval.assetCategoryId }?.direction ?: direction
                                            status = AssetStatus.IN_PROGRESS
                                            error = null
                                            approvalExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }


            item {
                if (isEditing && status == AssetStatus.IN_PROGRESS) {
                    AssetFormCard {
                        ExposedDropdownMenuBox(
                            expanded = transactionExpanded,
                            onExpandedChange = { transactionExpanded = !transactionExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedTransaction?.let { transaction ->
                                    "${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.date))}  $currency${String.format(Locale.US, "%.2f", transaction.amount)}"
                                } ?: "选择关联交易",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("关联交易") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(transactionExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = transactionExpanded,
                                onDismissRequest = { transactionExpanded = false }
                            ) {
                                candidateTransactions.forEach { transaction ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.date))}  $currency${String.format(Locale.US, "%.2f", transaction.amount)}")
                                        },
                                        onClick = {
                                            transactionId = transaction.id
                                            transactionExpanded = false
                                            error = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                PremiumNoteInput(
                    note = note,
                    onNoteChange = { note = it },
                    strings = strings,
                    modifier = Modifier.fillMaxWidth()
                )
            }


            item {
                AttachmentSection(
                    title = strings.attachments,
                    addLabel = strings.addAttachment,
                    emptyText = strings.noAttachments,
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
                PremiumSaveButton(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull()
                        val quantityValue = quantity.toDoubleOrNull()
                        error = when {
                            amountValue == null || amountValue <= 0.0 -> strings.enterValidAmount
                            assetCategoryId == null -> strings.selectCategory
                            fieldDefinitions.any { it.required && fieldValues[it.id].isNullOrBlank() } -> "请完整填写资产详细信息"
                            useSourceApproval && sourceApprovalId == null -> "请选择审批单"
                            else -> null
                        }
                        if (error == null && amountValue != null) {
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
                                quantity = quantityValue ?: 1.0,
                                purchaseDate = selectedApproval?.purchaseDate
                                    ?: existingAsset?.purchaseDate
                                    ?: date,
                                sourceApprovalId = sourceApprovalId,
                                transactionId = transactionId,
                                note = note,
                                isCompleted = existingAsset?.isCompleted ?: false,
                                attachments = AttachmentConverter.toJson(attachments),
                                createdAt = existingAsset?.createdAt ?: now,
                                updatedAt = now,
                                assetDirection = selectedCategory?.direction?.name ?: direction.name
                            )
                            scope.launch {
                                viewModel.saveAssetWithFields(asset, fieldValues, isEditing)
                                    .onSuccess { onNavigateBack() }
                                    .onFailure { error = it.message ?: "保存资产失败" }
                            }
                        }
                    },
                    strings = strings,
                    modifier = Modifier.fillMaxWidth()
                )
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


@Composable
private fun AssetFormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailsCard(
    fields: List<AssetFieldDefinition>,
    values: Map<Long, String>,
    onValueChange: (Long, String) -> Unit
) {
    val chinese = LocalAppStrings.current.language == "zh"
    AssetFormCard {
        Text("资产详细信息", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        fields.forEach { field ->
                val value = values[field.id].orEmpty()
                when (field.type) {
                    AssetFieldType.BOOLEAN -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        Text(localizedAssetFieldName(field.name, chinese))
                        androidx.compose.material3.Switch(checked = value == "true", onCheckedChange = { onValueChange(field.id, it.toString()) })
                    }
                    AssetFieldType.SINGLE_CHOICE -> {
                        var expanded by remember(field.id) { mutableStateOf(false) }
                        val options = field.options.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(localizedAssetFieldName(field.name, chinese) + if (field.required) " *" else "") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(field.id, option); expanded = false }) }
                            }
                        }
                    }
                    AssetFieldType.DATE -> {
                        var showFieldDatePicker by remember(field.id) { mutableStateOf(false) }
                        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(localizedAssetFieldName(field.name, chinese) + if (field.required) " *" else "") }, trailingIcon = { IconButton(onClick = { showFieldDatePicker = true }) { Icon(Icons.Default.CalendarToday, localizedAssetFieldName(field.name, chinese)) } }, modifier = Modifier.fillMaxWidth())
                        if (showFieldDatePicker) {
                            val pickerState = rememberDatePickerState(initialSelectedDateMillis = value.toLongOrNull() ?: System.currentTimeMillis())
                            DatePickerDialog(onDismissRequest = { showFieldDatePicker = false }, confirmButton = { TextButton(onClick = { onValueChange(field.id, (pickerState.selectedDateMillis ?: System.currentTimeMillis()).toString()); showFieldDatePicker = false }) { Text("确定") } }, dismissButton = { TextButton(onClick = { showFieldDatePicker = false }) { Text("取消") } }) { DatePicker(state = pickerState) }
                        }
                    }
                    else -> OutlinedTextField(value = value, onValueChange = { input -> if (field.type != AssetFieldType.NUMBER || input.isEmpty() || input.toDoubleOrNull() != null) onValueChange(field.id, input) }, label = { Text(localizedAssetFieldName(field.name, chinese) + if (field.required) " *" else "") }, keyboardOptions = KeyboardOptions(keyboardType = if (field.type == AssetFieldType.NUMBER) KeyboardType.Decimal else KeyboardType.Text), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun directionLabel(type: AssetDirection): String = when (type) {
    AssetDirection.POSITIVE -> "正资产"
    AssetDirection.NEGATIVE -> "负资产"
}

private fun assetStatusName(status: AssetStatus, strings: com.example.accountkeeper.ui.theme.AppStrings): String = when (status) {
    AssetStatus.NONE -> strings.none
    AssetStatus.OWNED -> strings.owned
    AssetStatus.NOT_OWNED -> strings.lost
    AssetStatus.LOST -> strings.lost
    AssetStatus.IN_PROGRESS -> strings.inProgress
    AssetStatus.TEMPORARILY_WITH_ME -> strings.owned
    AssetStatus.TEMPORARILY_WITH_OTHERS -> strings.lost
}
