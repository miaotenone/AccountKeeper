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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.AppStrings
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.AssetCategoryViewModel
import com.example.accountkeeper.ui.viewmodel.AssetViewModel
import com.example.accountkeeper.ui.viewmodel.ApprovalSubmitState
import com.example.accountkeeper.ui.viewmodel.BudgetViewModel
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
    assetCategoryViewModel: AssetCategoryViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    assetViewModel: AssetViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrencySymbol.current
    val requests by viewModel.approvalRequests.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val assetCategories by assetCategoryViewModel.assetCategories.collectAsState()
    val assets by assetViewModel.assets.collectAsState()
    val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }

    var approver by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BudgetApprovalRequest?>(null) }
    var reviewing by remember { mutableStateOf<BudgetApprovalRequest?>(null) }
    val submitState by viewModel.submitState.collectAsState()

    // Auto-dismiss form on successful submission
    LaunchedEffect(submitState) {
        if (submitState is ApprovalSubmitState.Success) {
            editing = null
            viewModel.submitState.value = ApprovalSubmitState.Idle
        }
    }

    if (editing != null) {
        ApprovalFormScreen(
            existing = editing,
            categories = expenseCategories,
            assetCategories = assetCategories,
            strings = strings,
            currency = currency,
            isSubmitting = submitState is ApprovalSubmitState.Submitting,
            submitError = (submitState as? ApprovalSubmitState.Error)?.message,
            onDismiss = { editing = null; viewModel.submitState.value = ApprovalSubmitState.Idle },
            onSubmit = { request ->
                viewModel.submitApproval(request, editing?.id)
            }
        )
        return
    }

    val visible = requests.filter { request ->
        when {
            history -> request.status != BudgetApprovalStatus.PENDING
            approver -> request.status == BudgetApprovalStatus.PENDING
            else -> request.status == BudgetApprovalStatus.PENDING ||
                request.status == BudgetApprovalStatus.REJECTED ||
                request.status == BudgetApprovalStatus.WITHDRAWN
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.approvalCenter, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            if (!approver && !history) {
                FloatingActionButton(
                    onClick = {
                        editing = BudgetApprovalRequest(
                            type = BudgetApprovalType.PURCHASE_BUDGET,
                            amount = 0.0
                        )
                    },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, strings.add)
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !approver,
                    onClick = { approver = false },
                    label = { Text(strings.approvalApplicant) }
                )
                FilterChip(
                    selected = approver,
                    onClick = { approver = true },
                    label = { Text(strings.approvalApprover) }
                )
            }
            TabRow(selectedTabIndex = if (history) 1 else 0) {
                Tab(
                    selected = !history,
                    onClick = { history = false },
                    text = { Text(strings.approvalTodo) }
                )
                Tab(
                    selected = history,
                    onClick = { history = true },
                    text = { Text(strings.approvalHistory) }
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            strings.approvalNoRequests,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(visible, key = { it.id }) { request ->
                    ApprovalCard(
                        request = request,
                        categoryName = expenseCategories.firstOrNull { it.id == request.categoryId }?.name,
                        currency = currency,
                        strings = strings,
                        canManage = !approver && !history,
                        onView = { reviewing = request },
                        onWithdraw = { viewModel.withdrawApproval(request.id) },
                        onEdit = { editing = request }
                    )
                }
            }
        }
    }

    reviewing?.let { request ->
        ApprovalDetail(
            request = request,
            categoryName = expenseCategories.firstOrNull { it.id == request.categoryId }?.name,
            assetCategoryName = assetCategories.firstOrNull { it.id == request.assetCategoryId }?.name,
            linkedAsset = assets.firstOrNull { it.sourceApprovalId == request.id },
            currency = currency,
            strings = strings,
            canDecide = approver && request.status == BudgetApprovalStatus.PENDING,
            onDismiss = { reviewing = null },
            onApprove = { note ->
                viewModel.approveApproval(request.id, note)
                reviewing = null
            },
            onReject = { note ->
                viewModel.rejectApproval(request.id, note)
                reviewing = null
            }
        )
    }
}

@Composable
private fun ApprovalCard(
    request: BudgetApprovalRequest,
    categoryName: String?,
    currency: String,
    strings: AppStrings,
    canManage: Boolean,
    onView: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (request.type == BudgetApprovalType.PURCHASE_BUDGET) {
                        strings.approvalPurchaseBudget
                    } else {
                        strings.approvalBudgetAdjustment
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    statusText(request.status, strings),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text("$categoryName ${strings.amount}: $currency${String.format(Locale.US, "%.2f", request.amount)}")
            if (request.itemName.isNotBlank()) {
                Text(
                    "${strings.itemName}: ${request.itemName}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onView) {
                    Icon(Icons.Default.Visibility, null)
                    Spacer(Modifier.width(4.dp))
                    Text(strings.approvalView)
                }
                if (canManage && request.status == BudgetApprovalStatus.PENDING) {
                    TextButton(onClick = onWithdraw) {
                        Text(strings.approvalWithdraw)
                    }
                }
                if (canManage && request.status in listOf(
                        BudgetApprovalStatus.REJECTED,
                        BudgetApprovalStatus.WITHDRAWN
                    )
                ) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.approvalResubmit)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApprovalFormScreen(
    existing: BudgetApprovalRequest?,
    categories: List<Category>,
    assetCategories: List<AssetCategoryEntity>,
    strings: AppStrings,
    currency: String,
    isSubmitting: Boolean = false,
    submitError: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (BudgetApprovalRequest) -> Unit
) {
    val context = LocalContext.current
    val isResubmit = existing?.id != null && existing.id > 0L

    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: BudgetApprovalType.PURCHASE_BUDGET) }
    var categoryId by remember(existing?.id) { mutableStateOf(existing?.categoryId) }
    var assetCategoryId by remember(existing?.id) { mutableStateOf(existing?.assetCategoryId) }
    var rootType by remember(existing?.id) {
        mutableStateOf(
            assetCategories.firstOrNull { it.id == existing?.assetCategoryId }?.rootType ?: AssetRootType.PHYSICAL
        )
    }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var itemName by remember(existing?.id) { mutableStateOf(existing?.itemName ?: "") }
    var specification by remember(existing?.id) { mutableStateOf(existing?.specification ?: "") }
    var quantity by remember(existing?.id) { mutableStateOf(existing?.quantity?.toString() ?: "1") }
    var reason by remember(existing?.id) { mutableStateOf(existing?.reason ?: "") }
    var purchaseDate by remember(existing?.id) { mutableStateOf(existing?.purchaseDate) }
    var attachments by remember(existing?.id) {
        mutableStateOf(existing?.let { AttachmentConverter.fromJson(it.attachments) } ?: emptyList())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var assetCategoryExpanded by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Attachment?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            copyAttachmentToInternalStorage(context, it)?.let { file ->
                attachments = attachments + file
            }
        }
    }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val selectedAssetCategory = assetCategories.firstOrNull { it.id == assetCategoryId }
    val visibleAssetCategories = assetCategories.filter {
        it.rootType == rootType && it.parentCategoryId != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isResubmit) strings.approvalResubmit else strings.approvalNewRequest,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
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
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text(strings.amount, color = MaterialTheme.colorScheme.error.takeIf { error != null } ?: MaterialTheme.colorScheme.onSurfaceVariant) },
                    prefix = { Text(currency) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: strings.selectCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.expenseCategory) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name) },
                                onClick = {
                                    categoryId = item.id
                                    error = null
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (type == BudgetApprovalType.PURCHASE_BUDGET) {
                item {
                    Text(strings.assetCategory, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssetRootType.entries.forEach { option ->
                            FilterChip(
                                selected = rootType == option,
                                onClick = {
                                    rootType = option
                                    assetCategoryId = null
                                    error = null
                                },
                                label = { Text(assetRootTypeName(option, strings)) }
                            )
                        }
                    }
                    Spacer(Modifier.padding(top = 4.dp))
                    ExposedDropdownMenuBox(
                        expanded = assetCategoryExpanded,
                        onExpandedChange = { assetCategoryExpanded = !assetCategoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAssetCategory?.name ?: strings.selectCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.assetCategory) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(assetCategoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = assetCategoryExpanded,
                            onDismissRequest = { assetCategoryExpanded = false }
                        ) {
                            visibleAssetCategories.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        assetCategoryId = item.id
                                        error = null
                                        assetCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text(strings.itemName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = specification,
                        onValueChange = { specification = it },
                        label = { Text(strings.specification) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(strings.quantity) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = purchaseDate?.let(::formatApprovalDate) ?: strings.approvalNotSet,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.approvalPurchaseDate) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, strings.approvalPurchaseDate)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(strings.approvalReason) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                AttachmentSection(
                    title = strings.attachments,
                    attachments = attachments,
                    onAddAttachment = { picker.launch(arrayOf("*/*")) },
                    onRemoveAttachment = { file ->
                        attachments = attachments.filterNot { it.id == file.id }
                    },
                    onPreviewAttachment = { file -> preview = file }
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

            submitError?.let { message ->
                item {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull()
                        val parsedQuantity = quantity.toDoubleOrNull()
                        error = when {
                            parsedAmount == null || parsedAmount <= 0.0 -> strings.approvalInvalidAmount
                            categoryId == null -> strings.approvalMissingCategory
                            assetCategoryId == null -> strings.approvalMissingAssetCategory
                            itemName.isBlank() -> strings.approvalMissingItemName
                            (parsedQuantity == null || parsedQuantity <= 0.0) -> strings.approvalInvalidQuantity
                            else -> null
                        }
                        if (error == null && parsedAmount != null) {
                            val now = System.currentTimeMillis()
                            onSubmit(
                                BudgetApprovalRequest(
                                    id = existing?.id ?: 0L,
                                    type = type,
                                    categoryId = categoryId,
                                    assetCategoryId = assetCategoryId,
                                    amount = parsedAmount,
                                    purchaseDate = purchaseDate,
                                    reason = reason.trim(),
                                    itemName = itemName.trim(),
                                    specification = specification.trim(),
                                    quantity = parsedQuantity ?: 1.0,
                                    attachments = AttachmentConverter.toJson(attachments),
                                    status = BudgetApprovalStatus.PENDING,
                                    createdAt = existing?.createdAt ?: now
                                )
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(strings.approvalSubmit)
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = purchaseDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchaseDate = state.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
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

    preview?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            onDismiss = { preview = null }
        )
    }
}

@Composable
private fun ApprovalDetail(
    request: BudgetApprovalRequest,
    categoryName: String?,
    assetCategoryName: String?,
    linkedAsset: Asset?,
    currency: String,
    strings: AppStrings,
    canDecide: Boolean,
    onDismiss: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var note by remember(request.id, canDecide) { mutableStateOf(request.decisionNote) }
    var preview by remember { mutableStateOf<Attachment?>(null) }
    val attachments = remember(request.id) { AttachmentConverter.fromJson(request.attachments) }
    val progress = when {
        request.type != BudgetApprovalType.PURCHASE_BUDGET -> strings.approvalNotApplicable
        linkedAsset?.status == AssetStatus.OWNED -> strings.approvalConfirmedOwned
        linkedAsset?.status == AssetStatus.IN_PROGRESS -> strings.approvalPurchaseInProgress
        request.status == BudgetApprovalStatus.APPROVED -> strings.approvalAwaitingPurchase
        else -> strings.approvalNotStarted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.approvalDetailTitle, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 580.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { DetailLine(strings.approvalRequestId, request.id.toString()) }
                item {
                    DetailLine(
                        strings.approvalRequestType,
                        if (request.type == BudgetApprovalType.PURCHASE_BUDGET) {
                            strings.approvalPurchaseBudget
                        } else {
                            strings.approvalBudgetAdjustment
                        }
                    )
                }
                item { DetailLine(strings.approvalCreatedAt, formatApprovalDate(request.createdAt)) }
                item { DetailLine(strings.expenseCategory, categoryName ?: strings.approvalNotSet) }
                item { DetailLine(strings.assetCategory, assetCategoryName ?: strings.approvalNotSet) }
                item {
                    DetailLine(
                        strings.amount,
                        "$currency${String.format(Locale.US, "%.2f", request.amount)}"
                    )
                }
                item { DetailLine(strings.itemName, request.itemName.ifBlank { strings.approvalNotSet }) }
                item { DetailLine(strings.specification, request.specification.ifBlank { strings.approvalNotSet }) }
                item { DetailLine(strings.quantity, request.quantity.toString()) }
                item {
                    DetailLine(
                        strings.approvalPurchaseDate,
                        request.purchaseDate?.let(::formatApprovalDate) ?: strings.approvalNotSet
                    )
                }
                item { DetailLine(strings.approvalReason, request.reason.ifBlank { strings.approvalNotSet }) }
                item { DetailLine(strings.approvalCurrentStatus, statusText(request.status, strings)) }
                item { DetailLine(strings.approvalPurchaseProgress, progress) }
                item {
                    DetailLine(
                        strings.approvalRelatedAsset,
                        linkedAsset?.let { "#${it.id}" + if (it.name.isBlank()) "" else " ${it.name}" }
                            ?: strings.approvalNotSet
                    )
                }
                item {
                    DetailLine(
                        strings.approvalRealTransaction,
                        linkedAsset?.transactionId?.let { "#$it" } ?: strings.approvalNotSet
                    )
                }
                item {
                    DetailLine(
                        strings.approvalDecisionNote,
                        request.decisionNote.ifBlank { strings.approvalNotSet }
                    )
                }
                item {
                    DetailLine(
                        strings.approvalDecidedAt,
                        request.decidedAt?.let(::formatApprovalDate) ?: strings.approvalNotSet
                    )
                }
                if (attachments.isNotEmpty()) {
                    item { Text(strings.attachments, fontWeight = FontWeight.Bold) }
                    items(attachments, key = { it.id }) { file ->
                        AttachmentRow(
                            attachment = file,
                            onClick = { preview = file }
                        )
                    }
                }
                if (canDecide) {
                    item {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(strings.approvalDecisionNote) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (canDecide) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onApprove(note) }) {
                        Text(strings.approvalApprove)
                    }
                    TextButton(onClick = { onReject(note) }) {
                        Text(strings.approvalReject)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                }
            }
        },
        dismissButton = {
            if (!canDecide) {
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        }
    )

    preview?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            onDismiss = { preview = null }
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value)
    }
}

private fun statusText(status: BudgetApprovalStatus, strings: AppStrings): String = when (status) {
    BudgetApprovalStatus.PENDING -> strings.approvalPending
    BudgetApprovalStatus.APPROVED -> strings.approvalApproved
    BudgetApprovalStatus.REJECTED -> strings.approvalRejected
    BudgetApprovalStatus.WITHDRAWN -> strings.approvalWithdrawn
}

private fun assetRootTypeName(type: AssetRootType, strings: AppStrings): String = when (type) {
    AssetRootType.PHYSICAL -> "实物资产"
    AssetRootType.VIRTUAL -> "虚拟资产"
}

private fun formatApprovalDate(time: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
