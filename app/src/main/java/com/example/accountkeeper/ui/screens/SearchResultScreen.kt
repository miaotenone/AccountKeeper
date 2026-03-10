package com.example.accountkeeper.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

enum class FilterType {
    ALL, INCOME, EXPENSE
}

enum class SortType {
    TIME_DESC, TIME_ASC, AMOUNT_DESC, AMOUNT_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    query: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.categories.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current

    // 分页搜索结果
    val pagedTransactions = viewModel.searchTransactionsPaged(query).collectAsLazyPagingItems()
    
    // 将分页数据转换为列表用于客户端筛选和排序（使用 derivedStateOf 确保响应式更新）
    val rawTransactions by remember {
        derivedStateOf {
            (0 until pagedTransactions.itemCount).mapNotNull { pagedTransactions[it] }
        }
    }

    // Filter and sort state
    var filterType by remember { mutableStateOf(FilterType.ALL) }
    var sortType by remember { mutableStateOf(SortType.TIME_DESC) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    
    // Time range filter
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Selection state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // Apply filters and sort on loaded data
    val transactions = remember(rawTransactions, filterType, sortType, startDate, endDate) {
        var filtered = rawTransactions

        // Apply type filter
        filtered = when (filterType) {
            FilterType.ALL -> filtered
            FilterType.INCOME -> filtered.filter { it.type == TransactionType.INCOME }
            FilterType.EXPENSE -> filtered.filter { it.type == TransactionType.EXPENSE }
        }

        // Apply time range filter
        if (startDate != null) {
            filtered = filtered.filter { it.date >= startDate!! }
        }
        if (endDate != null) {
            // Add one day to include the end date
            val endOfDay = endDate!! + 24 * 60 * 60 * 1000 - 1
            filtered = filtered.filter { it.date <= endOfDay }
        }

        // Apply sort
        when (sortType) {
            SortType.TIME_DESC -> filtered.sortedByDescending { it.date }
            SortType.TIME_ASC -> filtered.sortedBy { it.date }
            SortType.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortType.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    // Group transactions by date
    val groupedTransactions = transactions.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
    }.toSortedMap(reverseOrder())

    // Calculate totals
    val totalIncome = CurrencyUtils.convertToDisplay(
        transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        currency
    )
    val totalExpense = CurrencyUtils.convertToDisplay(
        transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        currency
    )
    val transactionCount = transactions.size

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                "${selectedIds.size} ${strings.selected}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                selectionMode = false
                                selectedIds.clear()
                            }) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = strings.cancel,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showBatchDeleteDialog = true },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = strings.delete,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.rotate(45f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (selectedIds.size == 1) {
                                        onNavigateToEditTransaction(selectedIds.first())
                                        selectionMode = false
                                        selectedIds.clear()
                                    }
                                },
                                enabled = selectedIds.size == 1
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = strings.editTransaction,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "${strings.searchResults}: $query",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "$transactionCount ${strings.transactions}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.back
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = strings.filter
                                )
                            }
                            IconButton(onClick = { showSortDialog = true }) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = strings.sortByTime
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active filters display
            if (filterType != FilterType.ALL || startDate != null || endDate != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            
                            // Type filter chip
                            if (filterType != FilterType.ALL) {
                                FilterChip(
                                    selected = true,
                                    onClick = { filterType = FilterType.ALL },
                                    label = { 
                                        Text(
                                            when (filterType) {
                                                FilterType.INCOME -> strings.income
                                                FilterType.EXPENSE -> strings.expense
                                                else -> strings.all
                                            }
                                        ) 
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Clear",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .rotate(45f)
                                        )
                                    }
                                )
                            }
                            
                            // Time range filter chip
                            if (startDate != null || endDate != null) {
                                FilterChip(
                                    selected = true,
                                    onClick = { 
                                        startDate = null
                                        endDate = null
                                    },
                                    label = { 
                                        Text(
                                            buildString {
                                                val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                                                if (startDate != null && endDate != null) {
                                                    append(dateFormat.format(Date(startDate!!)))
                                                    append(" - ")
                                                    append(dateFormat.format(Date(endDate!!)))
                                                } else if (startDate != null) {
                                                    append("From ")
                                                    append(dateFormat.format(Date(startDate!!)))
                                                } else {
                                                    append("Until ")
                                                    append(dateFormat.format(Date(endDate!!)))
                                                }
                                            }
                                        ) 
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Clear",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .rotate(45f)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (transactions.isEmpty()) {
                // Empty state
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                strings.noSearchResults,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "\"$query\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                // Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        val gradient = if (isSystemInDarkTheme()) {
                            Brush.linearGradient(DarkGradientPrimary)
                        } else {
                            Brush.linearGradient(LightGradientPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(gradient)
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (totalIncome > 0) {
                                    SummaryItemSearch(
                                        label = strings.income,
                                        value = "$currency${String.format(Locale.US, "%.2f", totalIncome)}",
                                        color = Color(0xFF5BD9CA)
                                    )
                                }
                                if (totalExpense > 0) {
                                    if (totalIncome > 0) {
                                        VerticalDivider(
                                            color = Color.White.copy(alpha = 0.25f),
                                            thickness = 1.dp,
                                            modifier = Modifier.height(50.dp)
                                        )
                                    }
                                    SummaryItemSearch(
                                        label = strings.expense,
                                        value = "-$currency${String.format(Locale.US, "%.2f", totalExpense)}",
                                        color = Color(0xFFFF6B6B)
                                    )
                                }
                            }
                        }
                    }
                }

                // Transaction List - grouped by date
                groupedTransactions.forEach { (dateString, txList) ->
                    // Date Header
                    item(key = "header_$dateString") {
                        DateHeaderCompactSearch(
                            date = dateString,
                            txList = txList,
                            currency = currency
                        )
                    }

                    // Transaction Items
                    items(
                        items = txList,
                        key = { it.id }
                    ) { transaction ->
                        val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: strings.other
                        val isSelected = selectedIds.contains(transaction.id)
                        SearchTransactionItem(
                            transaction = transaction,
                            categoryName = categoryName,
                            currency = currency,
                            isSelected = isSelected,
                            inSelectionMode = selectionMode,
                            strings = strings,
                            onClick = {
                                if (selectionMode) {
                                    if (isSelected) {
                                        selectedIds.remove(transaction.id)
                                    } else {
                                        selectedIds.add(transaction.id)
                                    }
                                } else {
                                    onNavigateToEditTransaction(transaction.id)
                                }
                            },
                            onLongClick = {
                                if (selectionMode) {
                                    if (isSelected) {
                                        selectedIds.remove(transaction.id)
                                    } else {
                                        selectedIds.add(transaction.id)
                                    }
                                } else {
                                    selectionMode = true
                                    selectedIds.add(transaction.id)
                                }
                            },
                            onDelete = {
                                viewModel.deleteTransaction(transaction)
                            }
                        )
                    }
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Filter dialog
        if (showFilterDialog) {
            FilterDialog(
                filterType = filterType,
                startDate = startDate,
                endDate = endDate,
                strings = strings,
                onFilterTypeChange = { filterType = it },
                onStartClick = { showStartDatePicker = true },
                onEndClick = { showEndDatePicker = true },
                onClearTimeRange = {
                    startDate = null
                    endDate = null
                },
                onDismiss = { showFilterDialog = false }
            )
        }

        // Sort dialog
        if (showSortDialog) {
            SortDialog(
                sortType = sortType,
                strings = strings,
                onSortTypeChange = { sortType = it },
                onDismiss = { showSortDialog = false }
            )
        }

        // Date pickers
        if (showStartDatePicker) {
            DatePickerDialog(
                onDateSelected = { date ->
                    startDate = date
                    showStartDatePicker = false
                },
                onDismiss = { showStartDatePicker = false },
                title = strings.startDate
            )
        }

        if (showEndDatePicker) {
            DatePickerDialog(
                onDateSelected = { date ->
                    endDate = date
                    showEndDatePicker = false
                },
                onDismiss = { showEndDatePicker = false },
                title = strings.endDate
            )
        }

        // Batch delete dialog
        if (showBatchDeleteDialog && selectedIds.isNotEmpty()) {
            BatchDeleteTransactionDialog(
                count = selectedIds.size,
                onConfirm = {
                    val transactionsToDelete = transactions.filter { it.id in selectedIds }
                    viewModel.deleteTransactions(transactionsToDelete)
                    selectionMode = false
                    selectedIds.clear()
                    showBatchDeleteDialog = false
                },
                onDismiss = {
                    showBatchDeleteDialog = false
                },
                strings = strings
            )
        }
    }
}

@Composable
fun SummaryItemSearch(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DateHeaderCompactSearch(date: String, txList: List<com.example.accountkeeper.data.model.Transaction>, currency: String) {
    val dayIncome = CurrencyUtils.convertToDisplay(
        txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        currency
    )
    val dayExpense = CurrencyUtils.convertToDisplay(
        txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        currency
    )

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dayIncome > 0) {
                    Text(
                        "+$currency${String.format(Locale.US, "%.2f", dayIncome)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00B5A4)
                    )
                }
                if (dayExpense > 0) {
                    Text(
                        "-$currency${String.format(Locale.US, "%.2f", dayExpense)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE63946)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    filterType: FilterType,
    startDate: Long?,
    endDate: Long?,
    strings: AppStrings,
    onFilterTypeChange: (FilterType) -> Unit,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onClearTimeRange: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.filter, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type filter
                Text(
                    strings.category,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == FilterType.ALL,
                        onClick = { onFilterTypeChange(FilterType.ALL) },
                        label = { Text(strings.all) }
                    )
                    FilterChip(
                        selected = filterType == FilterType.INCOME,
                        onClick = { onFilterTypeChange(FilterType.INCOME) },
                        label = { Text(strings.income) }
                    )
                    FilterChip(
                        selected = filterType == FilterType.EXPENSE,
                        onClick = { onFilterTypeChange(FilterType.EXPENSE) },
                        label = { Text(strings.expense) }
                    )
                }

                HorizontalDivider()

                // Time range filter
                Text(
                    strings.timeRange,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            startDate?.let { dateFormat.format(Date(it)) } ?: strings.startDate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onEndClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            endDate?.let { dateFormat.format(Date(it)) } ?: strings.endDate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (startDate != null || endDate != null) {
                    TextButton(onClick = onClearTimeRange) {
                        Text(strings.cancel)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.ok)
            }
        }
    )
}

@Composable
fun SortDialog(
    sortType: SortType,
    strings: AppStrings,
    onSortTypeChange: (SortType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(strings.sortSettings, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortType == SortType.TIME_DESC,
                        onClick = { onSortTypeChange(SortType.TIME_DESC) },
                        label = { 
                            Text(
                                "${strings.sortByTime} ↓",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = sortType == SortType.TIME_ASC,
                        onClick = { onSortTypeChange(SortType.TIME_ASC) },
                        label = { 
                            Text(
                                "${strings.sortByTime} ↑",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortType == SortType.AMOUNT_DESC,
                        onClick = { onSortTypeChange(SortType.AMOUNT_DESC) },
                        label = { 
                            Text(
                                "${strings.sortByAmount} ↓",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = sortType == SortType.AMOUNT_ASC,
                        onClick = { onSortTypeChange(SortType.AMOUNT_ASC) },
                        label = { 
                            Text(
                                "${strings.sortByAmount} ↑",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.ok)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchTransactionItem(
    transaction: com.example.accountkeeper.data.model.Transaction,
    categoryName: String,
    currency: String,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backgroundColor"
    )

    if (inSelectionMode) {
        // Selection mode - show checkbox
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { onLongClick() }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = if (isSelected) {
                BorderStroke(2.dp, borderColor)
            } else {
                null
            },
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                hoveredElevation = 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Category icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isIncome) {
                                if (isSystemInDarkTheme()) {
                                    Brush.verticalGradient(DarkGradientIncome)
                                } else {
                                    Brush.verticalGradient(LightGradientIncome)
                                }
                            } else {
                                if (isSystemInDarkTheme()) {
                                    Brush.verticalGradient(DarkGradientExpense)
                                } else {
                                    Brush.verticalGradient(LightGradientExpense)
                                }
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        categoryName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Type indicator
                    Text(
                        if (isIncome) "💰 ${strings.income}" else "💸 ${strings.expense}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0xFF00B5A4) else Color(0xFFE63946)
                    )
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            transaction.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        timeFormat.format(Date(transaction.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val displayAmount = CurrencyUtils.convertToDisplay(transaction.amount, currency)
                Text(
                    text = "${if (isIncome) "+" else "-"}$currency${String.format(Locale.US, "%.2f", displayAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF00B5A4) else Color(0xFFE63946)
                )
            }
        }
    } else {
        // Normal mode - support swipe to delete
        var offsetX by remember { mutableStateOf(0f) }
        val maxOffset = 300f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .draggable(
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetX + delta
                        offsetX = newOffset.coerceIn(-maxOffset, 0f)
                    }
                )
        ) {
            // Red delete background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onDelete() }
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.rotate(45f)
                )
            }

            // Transaction card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.toInt(), 0) }
                    .scale(scale)
                    .combinedClickable(
                        onClick = { onClick() },
                        onLongClick = { onLongClick() }
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                    hoveredElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (isIncome) {
                                    if (isSystemInDarkTheme()) {
                                        Brush.verticalGradient(DarkGradientIncome)
                                    } else {
                                        Brush.verticalGradient(LightGradientIncome)
                                    }
                                } else {
                                    if (isSystemInDarkTheme()) {
                                        Brush.verticalGradient(DarkGradientExpense)
                                    } else {
                                        Brush.verticalGradient(LightGradientExpense)
                                    }
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            categoryName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Type indicator
                        Text(
                            if (isIncome) "💰 ${strings.income}" else "💸 ${strings.expense}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) Color(0xFF00B5A4) else Color(0xFFE63946)
                        )
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (transaction.note.isNotBlank()) {
                            Text(
                                transaction.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            timeFormat.format(Date(transaction.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val displayAmount = CurrencyUtils.convertToDisplay(transaction.amount, currency)
                    Text(
                        text = "${if (isIncome) "+" else "-"}$currency${String.format(Locale.US, "%.2f", displayAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) Color(0xFF00B5A4) else Color(0xFFE63946)
                    )
                }
            }
        }
    }
}
