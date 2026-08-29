package com.example.accountkeeper.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.BudgetApprovalRequest
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.BudgetPeriodType
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.BudgetViewModel
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetApprovalScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrencySymbol.current
    val requests by viewModel.approvalRequests.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val expenseCategories = remember(categories) { categories.filter { it.type == TransactionType.EXPENSE } }
    var isApprover by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var editingRequest by remember { mutableStateOf<BudgetApprovalRequest?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var reviewingRequest by remember { mutableStateOf<BudgetApprovalRequest?>(null) }

    val filtered = if (showHistory) {
        requests.filter { it.status != BudgetApprovalStatus.PENDING }
    } else {
        if (isApprover) requests.filter { it.status == BudgetApprovalStatus.PENDING }
        else requests.filter { it.status == BudgetApprovalStatus.PENDING || it.status == BudgetApprovalStatus.WITHDRAWN }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.approvalCenter, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            if (!isApprover && !showHistory) {
                FloatingActionButton(
                    onClick = {
                        editingRequest = null
                        showForm = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 16.dp,
                        pressedElevation = 24.dp
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = strings.add,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = !isApprover, onClick = { isApprover = false }, label = { Text(strings.approvalApplicant) })
                FilterChip(selected = isApprover, onClick = { isApprover = true }, label = { Text(strings.approvalApprover) })
            }
            ScrollableTabRow(
                selectedTabIndex = if (showHistory) 1 else 0,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                TabItem(strings.approvalTodo, !showHistory) { showHistory = false }
                TabItem(strings.approvalHistory, showHistory) { showHistory = true }
            }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.approvalNoRequests, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered.size) { index ->
                        val request = filtered[index]
                        ApprovalRequestCard(
                            request = request,
                            categoryName = expenseCategories.firstOrNull { it.id == request.categoryId }?.name,
                            currency = currency,
                            strings = strings,
                            isApprover = isApprover,
                            showHistory = showHistory,
                            onReview = { reviewingRequest = request },
                            onWithdraw = { viewModel.withdrawApproval(request.id) },
                            onEdit = {
                                editingRequest = request
                                showForm = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        BudgetApprovalFormDialog(
            existing = editingRequest,
            categories = expenseCategories,
            viewModel = viewModel,
            currency = currency,
            onDismiss = { showForm = false; editingRequest = null },
            onSubmit = { request ->
                viewModel.submitApproval(request, editingRequest?.id)
                showForm = false
                editingRequest = null
            }
        )
    }

    reviewingRequest?.let { request ->
        BudgetApprovalReviewDialog(
            request = request,
            categoryName = expenseCategories.firstOrNull { it.id == request.categoryId }?.name,
            currency = currency,
            strings = strings,
            canDecide = isApprover && request.status == BudgetApprovalStatus.PENDING,
            onDismiss = { reviewingRequest = null },
            onApprove = { note -> viewModel.approveApproval(request.id, note); reviewingRequest = null },
            onReject = { note -> viewModel.rejectApproval(request.id, note); reviewingRequest = null }
        )
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Tab(selected = selected, onClick = onClick, text = { Text(label) })
}

@Composable
private fun ApprovalRequestCard(
    request: BudgetApprovalRequest,
    categoryName: String?,
    currency: String,
    strings: com.example.accountkeeper.ui.theme.AppStrings,
    isApprover: Boolean,
    showHistory: Boolean,
    onReview: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit
) {
    val statusText = when (request.status) {
        BudgetApprovalStatus.PENDING -> strings.approvalPending
        BudgetApprovalStatus.APPROVED -> strings.approvalApproved
        BudgetApprovalStatus.REJECTED -> strings.approvalRejected
        BudgetApprovalStatus.WITHDRAWN -> strings.approvalWithdrawn
    }
    val statusColor = when (request.status) {
        BudgetApprovalStatus.PENDING -> MaterialTheme.colorScheme.primary
        BudgetApprovalStatus.APPROVED -> Color(0xFF2E7D32)
        BudgetApprovalStatus.REJECTED -> MaterialTheme.colorScheme.error
        BudgetApprovalStatus.WITHDRAWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val typeText = if (request.type == BudgetApprovalType.PURCHASE_BUDGET) strings.approvalPurchaseBudget else strings.approvalBudgetAdjustment
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(typeText, fontWeight = FontWeight.Bold)
                Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
            Text("${request.monthKey} · ${request.periodType}")
            Text("${categoryName ?: strings.total}: $currency${String.format(Locale.US, "%.2f", request.amount)}")
            if (request.purchaseDate != null) {
                Text("${strings.approvalPurchaseDate}: ${formatDate(request.purchaseDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (request.reason.isNotBlank()) Text(request.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (request.decisionNote.isNotBlank()) Text("${strings.approvalDecisionNote}: ${request.decisionNote}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isApprover && request.status == BudgetApprovalStatus.PENDING) {
                    Button(onClick = onReview) { Icon(Icons.Default.Check, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(strings.approvalApprove) }
                } else if (!isApprover && request.status == BudgetApprovalStatus.PENDING && !showHistory) {
                    TextButton(onClick = onWithdraw) { Icon(Icons.Default.Close, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(strings.approvalWithdraw) }
                } else if (!isApprover && request.status == BudgetApprovalStatus.WITHDRAWN && !showHistory) {
                    TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(4.dp)); Text(strings.approvalResubmit) }
                } else if (showHistory) {
                    TextButton(onClick = onReview) { Text(strings.approvalHistory) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetApprovalFormDialog(
    existing: BudgetApprovalRequest?,
    categories: List<Category>,
    viewModel: BudgetViewModel,
    currency: String,
    onDismiss: () -> Unit,
    onSubmit: (BudgetApprovalRequest) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: BudgetApprovalType.PURCHASE_BUDGET) }
    var monthKey by remember(existing?.id) { mutableStateOf(existing?.monthKey ?: BudgetViewModel.monthKey(System.currentTimeMillis())) }
    var periodType by remember(existing?.id) { mutableStateOf(existing?.periodType ?: BudgetPeriodType.MONTHLY.name) }
    var categoryId by remember(existing?.id) { mutableStateOf(existing?.categoryId) }
    var amountText by remember(existing?.id) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var purchaseDate by remember(existing?.id) { mutableStateOf(existing?.purchaseDate ?: System.currentTimeMillis()) }
    var reason by remember(existing?.id) { mutableStateOf(existing?.reason ?: "") }
    var attachments by remember(existing?.id) { mutableStateOf(existing?.let { AttachmentConverter.fromJson(it.attachments) } ?: emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedPeriod = runCatching { BudgetPeriodType.valueOf(periodType) }.getOrDefault(BudgetPeriodType.MONTHLY)
    val selectedRange = remember(monthKey, selectedPeriod) { BudgetViewModel.periodRange(monthKey, selectedPeriod) }
    val budgetFlow = remember(monthKey, selectedPeriod, categoryId) { viewModel.budgetFor(monthKey, selectedPeriod, categoryId) }
    val budget by budgetFlow.collectAsState()
    val spentFlow = remember(monthKey, selectedPeriod, categoryId, selectedRange) {
        categoryId?.let { viewModel.expenseFor(it, selectedRange.first, selectedRange.second) }
            ?: viewModel.expenseForPeriod(monthKey, selectedPeriod)
    }
    val spent by spentFlow.collectAsState()
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { copyApprovalAttachment(context, it)?.let { attachment -> attachments = attachments + attachment } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) strings.approvalNewRequest else strings.approvalResubmit) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == BudgetApprovalType.PURCHASE_BUDGET, onClick = { type = BudgetApprovalType.PURCHASE_BUDGET }, label = { Text(strings.approvalPurchaseBudget) })
                        FilterChip(selected = type == BudgetApprovalType.BUDGET_ADJUSTMENT, onClick = { type = BudgetApprovalType.BUDGET_ADJUSTMENT }, label = { Text(strings.approvalBudgetAdjustment) })
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { monthKey = shiftMonth(monthKey, -periodStep(selectedPeriod)) }) { Icon(Icons.Default.ChevronLeft, contentDescription = strings.back) }
                        Text(BudgetViewModel.periodLabel(monthKey, selectedPeriod), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { monthKey = shiftMonth(monthKey, periodStep(selectedPeriod)) }) { Icon(Icons.Default.ChevronRight, contentDescription = strings.navigate) }
                    }
                }
                item {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BudgetPeriodType.entries.forEach { period ->
                            FilterChip(selected = selectedPeriod == period, onClick = { periodType = period.name }, label = { Text(periodLabel(period, strings)) })
                        }
                    }
                }
                item {
                    Text(strings.category, fontWeight = FontWeight.SemiBold)
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightIn(max = 180.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (type == BudgetApprovalType.BUDGET_ADJUSTMENT) {
                            item {
                                FilterChip(selected = categoryId == null, onClick = { categoryId = null }, label = { Text(strings.total) })
                            }
                        }
                        items(categories) { category ->
                            FilterChip(selected = categoryId == category.id, onClick = { categoryId = category.id }, label = { Text(category.name) })
                        }
                    }
                }
                item {
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it; error = null }, label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Text("${strings.approvalCurrentBudget}: ${budget?.amount?.let { "$currency${String.format(Locale.US, "%.2f", it)}" } ?: strings.budgetNotSet}")
                    Text("${strings.approvalRemaining}: $currency${String.format(Locale.US, "%.2f", (budget?.amount ?: 0.0) - spent)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (type == BudgetApprovalType.PURCHASE_BUDGET) {
                    item {
                        OutlinedTextField(value = formatDate(purchaseDate), onValueChange = {}, readOnly = true, label = { Text(strings.approvalPurchaseDate) }, trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarToday, contentDescription = strings.approvalPurchaseDate) } }, modifier = Modifier.fillMaxWidth())
                    }
                }
                item { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text(strings.approvalReason) }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.attachments, fontWeight = FontWeight.SemiBold)
                    }
                    attachments.forEach { attachment ->
                        ApprovalAttachmentItem(attachment = attachment, onRemove = { attachments = attachments.filter { it.id != attachment.id } })
                    }
                }
                error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                error = when {
                    amount == null || amount <= 0.0 -> strings.approvalInvalidAmount
                    type == BudgetApprovalType.PURCHASE_BUDGET && categoryId == null -> strings.approvalMissingCategory
                    else -> null
                }
                if (error == null && amount != null) {
                    onSubmit(
                        BudgetApprovalRequest(
                            id = existing?.id ?: 0,
                            type = type,
                            monthKey = monthKey,
                            periodType = periodType,
                            categoryId = categoryId,
                            amount = amount,
                            purchaseDate = if (type == BudgetApprovalType.PURCHASE_BUDGET) purchaseDate else null,
                            reason = reason.trim(),
                            attachments = AttachmentConverter.toJson(attachments),
                            status = BudgetApprovalStatus.PENDING,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            }) { Text(strings.approvalSubmit) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = purchaseDate)
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { purchaseDate = state.selectedDateMillis ?: purchaseDate; showDatePicker = false }) { Text(strings.ok) } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) } }) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun BudgetApprovalReviewDialog(
    request: BudgetApprovalRequest,
    categoryName: String?,
    currency: String,
    strings: com.example.accountkeeper.ui.theme.AppStrings,
    canDecide: Boolean,
    onDismiss: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var note by remember(request.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (request.type == BudgetApprovalType.PURCHASE_BUDGET) strings.approvalPurchaseBudget else strings.approvalBudgetAdjustment) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${request.monthKey} · ${request.periodType}")
                Text("${categoryName ?: strings.total}: $currency${String.format(Locale.US, "%.2f", request.amount)}")
                if (request.purchaseDate != null) Text("${strings.approvalPurchaseDate}: ${formatDate(request.purchaseDate)}")
                if (request.reason.isNotBlank()) Text(request.reason)
                if (canDecide) OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(strings.approvalDecisionNote) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                else if (request.decisionNote.isNotBlank()) Text("${strings.approvalDecisionNote}: ${request.decisionNote}")
            }
        },
        confirmButton = {
            if (canDecide) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onReject(note) }) { Text(strings.approvalReject, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { onApprove(note) }) { Text(strings.approvalApprove) }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun ApprovalAttachmentItem(attachment: Attachment, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(attachment.fileName, modifier = Modifier.weight(1f), maxLines = 1)
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = LocalAppStrings.current.removeAttachment, tint = MaterialTheme.colorScheme.error) }
    }
}

private fun periodLabel(period: BudgetPeriodType, strings: com.example.accountkeeper.ui.theme.AppStrings): String = when (period) {
    BudgetPeriodType.MONTHLY -> strings.monthly
    BudgetPeriodType.SEMI_ANNUAL -> strings.semiAnnual
    BudgetPeriodType.ANNUAL -> strings.yearly
}

private fun periodStep(period: BudgetPeriodType): Int = when (period) {
    BudgetPeriodType.MONTHLY -> 1
    BudgetPeriodType.SEMI_ANNUAL -> 6
    BudgetPeriodType.ANNUAL -> 12
}

private fun shiftMonth(key: String, amount: Int): String {
    val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(key) ?: Date()
    return Calendar.getInstance().apply { time = parsed; add(Calendar.MONTH, amount) }.let { SimpleDateFormat("yyyy-MM", Locale.US).format(it.time) }
}

private fun formatDate(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))

private fun copyApprovalAttachment(context: Context, uri: Uri): Attachment? {
    return try {
        val resolver = context.contentResolver
        val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        } ?: "file_${System.currentTimeMillis()}"
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val directory = File(context.filesDir, "attachments").apply { mkdirs() }
        val target = File(directory, "${System.currentTimeMillis()}_$fileName")
        resolver.openInputStream(uri)?.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } } ?: return null
        Attachment(
            id = System.currentTimeMillis().toString(),
            fileName = fileName,
            filePath = target.absolutePath,
            fileType = Attachment.getTypeFromMimeType(mimeType),
            fileSize = target.length(),
            mimeType = mimeType
        )
    } catch (_: Exception) {
        null
    }
}
