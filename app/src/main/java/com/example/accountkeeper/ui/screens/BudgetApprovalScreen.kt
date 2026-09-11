package com.example.accountkeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetFieldDefinition
import com.example.accountkeeper.data.model.AssetCategoryEntity
import com.example.accountkeeper.data.model.sortedAssetLeafCategories
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
import com.example.accountkeeper.ui.components.AppSectionCard
import com.example.accountkeeper.ui.components.AppAddFab
import com.example.accountkeeper.ui.components.AppPrimaryButton

import com.example.accountkeeper.ui.viewmodel.AssetCategoryViewModel
import kotlinx.coroutines.flow.flowOf
import com.example.accountkeeper.data.model.AssetFieldType
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
    onNavigateToNewRequest: () -> Unit = {},
    startNewRequest: Boolean = false,
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
    var historyStartAt by remember { mutableStateOf<Long?>(null) }
    var historyEndAt by remember { mutableStateOf<Long?>(null) }
    var historyCategoryId by remember { mutableStateOf<Long?>(null) }
    var historyAssetCategoryId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isHistorySearchExpanded by remember { mutableStateOf(false) }
    var showHistoryFilterDialog by remember { mutableStateOf(false) }
    var historySelectionMode by remember { mutableStateOf(false) }
    val selectedHistoryIds = remember { mutableStateListOf<Long>() }
    var deleteHistoryTarget by remember { mutableStateOf<List<BudgetApprovalRequest>?>(null) }
    var showHistoryStartPicker by remember { mutableStateOf(false) }
    var showHistoryEndPicker by remember { mutableStateOf(false) }
    var historyCategoryExpanded by remember { mutableStateOf(false) }
    var historyAssetCategoryExpanded by remember { mutableStateOf(false) }
    var editing by remember(startNewRequest) {
        mutableStateOf(
            if (startNewRequest) {
                BudgetApprovalRequest(
                    type = BudgetApprovalType.PURCHASE_BUDGET,
                    amount = 0.0
                )
            } else null
        )
    }
    var reviewing by remember { mutableStateOf<BudgetApprovalRequest?>(null) }
    val submitState by viewModel.submitState.collectAsState()

    // Auto-dismiss form on successful submission
    LaunchedEffect(submitState) {
        if (submitState is ApprovalSubmitState.Success) {
            editing = null
            viewModel.submitState.value = ApprovalSubmitState.Idle
            if (startNewRequest) onNavigateBack()
        }
    }

    if (editing != null) {
        ApprovalFormScreen(
            existing = editing,
            categories = expenseCategories,
            assetCategories = assetCategories,
            assetCategoryViewModel = assetCategoryViewModel,
            strings = strings,
            currency = currency,
            isSubmitting = submitState is ApprovalSubmitState.Submitting,
            loadExistingFieldValues = viewModel::getApprovalFieldValues,
            submitError = (submitState as? ApprovalSubmitState.Error)?.message,
            onDismiss = { editing = null; viewModel.submitState.value = ApprovalSubmitState.Idle; if (startNewRequest) onNavigateBack() },
            onSubmit = { request, values ->
                viewModel.submitApprovalWithFields(request, values, editing?.id)
            }
        )
        return
    }

    val visible = requests.filter { request ->
        val tabMatch = when {
            history -> request.status != BudgetApprovalStatus.PENDING
            approver -> request.status == BudgetApprovalStatus.PENDING
            else -> request.status == BudgetApprovalStatus.PENDING || request.status == BudgetApprovalStatus.REJECTED || request.status == BudgetApprovalStatus.WITHDRAWN
        }
        val query = searchQuery.trim().lowercase()
        val searchMatch = query.isBlank() || listOf(request.id.toString(), request.reason, request.itemName, request.specification, request.decisionNote).any { it.lowercase().contains(query) }
        val dateMatch = (historyStartAt == null || request.createdAt >= historyStartAt!!) && (historyEndAt == null || request.createdAt < historyEndAt!!)
        tabMatch && searchMatch && dateMatch && (historyCategoryId == null || request.categoryId == historyCategoryId) && (historyAssetCategoryId == null || request.assetCategoryId == historyAssetCategoryId)
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
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (historySelectionMode && selectedHistoryIds.isNotEmpty()) {
                        TextButton(onClick = { deleteHistoryTarget = visible.filter { it.id in selectedHistoryIds } }) { Text("${selectedHistoryIds.size} ${strings.delete}") }
                        TextButton(onClick = { selectedHistoryIds.clear(); historySelectionMode = false }) { Text(strings.cancel) }
                    } else if (history) {
                        IconButton(onClick = { isHistorySearchExpanded = !isHistorySearchExpanded }) { Icon(Icons.Default.Search, strings.search) }
                        IconButton(onClick = { showHistoryFilterDialog = true }) { Icon(Icons.Default.FilterList, strings.filter) }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!approver && !history) {
                AppAddFab(onClick = onNavigateToNewRequest, contentDescription = strings.approvalNewRequest)
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
            if (history && isHistorySearchExpanded) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text(strings.approvalSearch) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
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
                        canManage = !approver && (!history || request.status in listOf(BudgetApprovalStatus.REJECTED, BudgetApprovalStatus.WITHDRAWN, BudgetApprovalStatus.PENDING)),
                        onView = { reviewing = request },
                        onWithdraw = { viewModel.withdrawApproval(request.id) },
                        onEdit = { editing = request },
                        allowDelete = history,
                        selected = request.id in selectedHistoryIds,
                        onSelect = { if (request.id in selectedHistoryIds) selectedHistoryIds.remove(request.id) else selectedHistoryIds.add(request.id) },
                        onDelete = { deleteHistoryTarget = listOf(request) },
                        onLongClick = { if (history) { historySelectionMode = true; if (request.id in selectedHistoryIds) selectedHistoryIds.remove(request.id) else selectedHistoryIds.add(request.id) } }                    )
                }
            }
        }
    }

    if (showHistoryFilterDialog) {
        AlertDialog(onDismissRequest = { showHistoryFilterDialog = false }, title = { Text(strings.filter) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { showHistoryStartPicker = true }) { Text(historyStartAt?.let(::formatApprovalDate) ?: strings.startDate) }; OutlinedButton(onClick = { showHistoryEndPicker = true }) { Text(historyEndAt?.let(::formatApprovalDate) ?: strings.endDate) } }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HistoryCategoryFilter(selectedId = historyCategoryId, categories = expenseCategories, allLabel = strings.all, onSelected = { historyCategoryId = it })
                    HistoryCategoryFilter(selectedId = historyAssetCategoryId, categories = assetCategories.map { Category(it.id, it.name, TransactionType.ASSET) }, allLabel = strings.all, onSelected = { historyAssetCategoryId = it }) } } }, confirmButton = { TextButton(onClick = { showHistoryFilterDialog = false }) { Text(strings.ok) } }, dismissButton = { TextButton(onClick = { searchQuery = ""; historyCategoryId = null; historyAssetCategoryId = null; historyStartAt = null; historyEndAt = null }) { Text(strings.approvalClearFilters) } })
    }
    deleteHistoryTarget?.let { targets ->
        AlertDialog(
            onDismissRequest = { deleteHistoryTarget = null },
            title = { Text("删除审批历史") },
            text = { Text("将删除 ${targets.size} 条审批历史。关联资产未确定失去的记录将不会被删除。") },
            confirmButton = { Button(onClick = { targets.forEach { viewModel.deleteHistoryApproval(it.id) }; selectedHistoryIds.removeAll(targets.map { it.id }.toSet()); historySelectionMode = false; deleteHistoryTarget = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { deleteHistoryTarget = null }) { Text(strings.cancel) } }
        )
    }
    if (showHistoryStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = historyStartAt ?: System.currentTimeMillis())
        DatePickerDialog(onDismissRequest = { showHistoryStartPicker = false }, confirmButton = { TextButton(onClick = { historyStartAt = state.selectedDateMillis; showHistoryStartPicker = false }) { Text(strings.ok) } }, dismissButton = { TextButton(onClick = { showHistoryStartPicker = false }) { Text(strings.cancel) } }) { DatePicker(state = state) }
    }
    if (showHistoryEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = historyEndAt ?: System.currentTimeMillis())
        DatePickerDialog(onDismissRequest = { showHistoryEndPicker = false }, confirmButton = { TextButton(onClick = { historyEndAt = state.selectedDateMillis?.plus(86_400_000L); showHistoryEndPicker = false }) { Text(strings.ok) } }, dismissButton = { TextButton(onClick = { showHistoryEndPicker = false }) { Text(strings.cancel) } }) { DatePicker(state = state) }
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
@OptIn(ExperimentalFoundationApi::class)
private fun ApprovalCard(
    request: BudgetApprovalRequest,
    categoryName: String?,
    currency: String,
    strings: AppStrings,
    canManage: Boolean,
    onView: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    allowDelete: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (allowDelete) Button(onClick = onDelete, modifier = Modifier.align(Alignment.CenterEnd).width(72.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.Delete, strings.delete) }
        AppSectionCard(modifier = Modifier.fillMaxWidth().offset { IntOffset(dragOffset.roundToInt(), 0) }.pointerInput(allowDelete) { if (allowDelete) detectHorizontalDragGestures(onHorizontalDrag = { _, amount -> dragOffset = (dragOffset + amount).coerceIn(-240f, 0f) }, onDragEnd = { if (dragOffset < -120f) dragOffset = -240f else dragOffset = 0f }) }.then(Modifier.combinedClickable(onClick = { if (selected) onSelect() else onView() }, onLongClick = onLongClick))) {
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
}    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApprovalFormScreen(
    existing: BudgetApprovalRequest?,
    categories: List<Category>,
    assetCategories: List<AssetCategoryEntity>,
    assetCategoryViewModel: AssetCategoryViewModel,
    strings: AppStrings,
    currency: String,
    isSubmitting: Boolean = false,
    loadExistingFieldValues: suspend (Long) -> List<com.example.accountkeeper.data.model.ApprovalFieldValue>,
    submitError: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (BudgetApprovalRequest, Map<Long, String>) -> Unit
) {
    val context = LocalContext.current
    val isResubmit = existing?.id != null && existing.id > 0L

    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: BudgetApprovalType.PURCHASE_BUDGET) }
    var categoryId by remember(existing?.id) { mutableStateOf(existing?.categoryId) }
    var assetCategoryId by remember(existing?.id) { mutableStateOf(existing?.assetCategoryId) }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amount?.takeIf { it > 0 }?.toString() ?: "")
    }
    var reason by remember(existing?.id) { mutableStateOf(existing?.reason ?: "") }
    var purchaseDate by remember(existing?.id) { mutableStateOf(existing?.purchaseDate) }
    var expectedPurchaseEndAt by remember(existing?.id) { mutableStateOf(existing?.expectedPurchaseEndAt) }
    var attachments by remember(existing?.id) {
        mutableStateOf(existing?.let { AttachmentConverter.fromJson(it.attachments) } ?: emptyList())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var assetCategoryExpanded by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Attachment?>(null) }
    var fieldValues by remember(existing?.id) { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val fieldDefinitions by remember(assetCategoryId) { assetCategoryId?.let { assetCategoryViewModel.fieldsFor(it) } ?: flowOf(emptyList()) }.collectAsState(emptyList())

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            copyAttachmentToInternalStorage(context, it)?.let { file ->
                attachments = attachments + file
            }
        }
    }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val selectedAssetCategory = assetCategories.firstOrNull { it.id == assetCategoryId }
    val visibleAssetCategories = sortedAssetLeafCategories(assetCategories)

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            if (isResubmit) strings.approvalResubmit else strings.approvalNewRequest,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
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
            if (type == BudgetApprovalType.PURCHASE_BUDGET) {
                item {
                    OutlinedTextField(
                        value = existing?.createdAt?.let(::formatApprovalDate) ?: strings.approvalNotSet,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.approvalCreatedAt) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(value = purchaseDate?.let(::formatApprovalDate) ?: strings.approvalNotSet, onValueChange = {}, readOnly = true, label = { Text(strings.approvalPurchaseDate) }, trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, strings.approvalPurchaseDate) } }, modifier = Modifier.fillMaxWidth())
                }
            }

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
                    ExposedDropdownMenuBox(expanded = assetCategoryExpanded, onExpandedChange = { assetCategoryExpanded = !assetCategoryExpanded }) {
                        OutlinedTextField(value = selectedAssetCategory?.name ?: strings.selectCategory, onValueChange = {}, readOnly = true, label = { Text(strings.assetCategory) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(assetCategoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = assetCategoryExpanded, onDismissRequest = { assetCategoryExpanded = false }) {
                            visibleAssetCategories.forEach { item ->
                                DropdownMenuItem(text = { Text(item.name) }, onClick = { assetCategoryId = item.id; fieldValues = emptyMap(); error = null; assetCategoryExpanded = false })
                            }
                        }
                    }
                }
                if (fieldDefinitions.isNotEmpty()) {
                    item { AssetDetailsCard(fields = fieldDefinitions, values = fieldValues, onValueChange = { id, value -> fieldValues = fieldValues + (id to value); error = null }) }
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
                    addLabel = strings.addAttachment,
                    emptyText = strings.noAttachments,
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
                AppPrimaryButton(
                    text = strings.approvalSubmit,
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull()
                        error = when {
                            parsedAmount == null || parsedAmount <= 0.0 -> strings.approvalInvalidAmount
                            categoryId == null -> strings.approvalMissingCategory
                            assetCategoryId == null -> strings.approvalMissingAssetCategory
                            reason.isBlank() -> strings.approvalMissingReason
                            fieldDefinitions.any { it.required && fieldValues[it.id].isNullOrBlank() } -> strings.approvalMissingAssetDetails
                            else -> null
                        }
                        if (error == null && parsedAmount != null) {
                            val now = System.currentTimeMillis()
                            onSubmit(BudgetApprovalRequest(id = existing?.id ?: 0L, type = type, categoryId = categoryId, assetCategoryId = assetCategoryId, amount = parsedAmount, purchaseDate = purchaseDate, expectedPurchaseEndAt = expectedPurchaseEndAt, reason = reason.trim(), itemName = "", specification = "", quantity = 1.0, attachments = AttachmentConverter.toJson(attachments), status = BudgetApprovalStatus.PENDING, createdAt = existing?.createdAt ?: now), fieldValues)
                        }
                    },
                    enabled = !isSubmitting,
                    loading = isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                )
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

    if (showDeadlinePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expectedPurchaseEndAt ?: purchaseDate ?: System.currentTimeMillis())
        DatePickerDialog(onDismissRequest = { showDeadlinePicker = false }, confirmButton = { TextButton(onClick = { expectedPurchaseEndAt = state.selectedDateMillis; showDeadlinePicker = false }) { Text(strings.ok) } }, dismissButton = { TextButton(onClick = { showDeadlinePicker = false }) { Text(strings.cancel) } }) { DatePicker(state = state) }
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
                modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCategoryFilter(selectedId: Long?, categories: List<Category>, allLabel: String, onSelected: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = categories.firstOrNull { it.id == selectedId }?.name ?: allLabel, onValueChange = {}, readOnly = true, label = { Text(if (categories.firstOrNull { it.id == selectedId } == null) allLabel else categories.first { it.id == selectedId }.name) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().width(170.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelected(null); expanded = false })
            categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelected(category.id); expanded = false }) }
        }
    }
}

private fun statusText(status: BudgetApprovalStatus, strings: AppStrings): String = when (status) {
    BudgetApprovalStatus.PENDING -> strings.approvalPending
    BudgetApprovalStatus.APPROVED -> strings.approvalApproved
    BudgetApprovalStatus.REJECTED -> strings.approvalRejected
    BudgetApprovalStatus.WITHDRAWN -> strings.approvalWithdrawn
    BudgetApprovalStatus.PURCHASE_IN_PROGRESS -> strings.approvalPurchaseInProgress
    BudgetApprovalStatus.PURCHASE_COMPLETED -> strings.approvalPurchaseCompleted
}

private fun formatApprovalDate(time: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
