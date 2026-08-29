package com.example.accountkeeper.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.SortType
import com.example.accountkeeper.ui.viewmodel.TimeRange
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToSearchResult: (String) -> Unit,
    homeVisibilityEventId: Long = 0L,
    homeVisibilityVisible: Boolean = false,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val pagedTransactions = viewModel.pagedTransactions.collectAsLazyPagingItems()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    val filterCategoryId by viewModel.filterCategoryId.collectAsState()
    val filterStartDate by viewModel.filterStartDate.collectAsState()
    val filterEndDate by viewModel.filterEndDate.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    var isBalanceVisible by remember { mutableStateOf(false) }
    var lastConsumedVisibilityEventId by remember { mutableStateOf(0L) }
    LaunchedEffect(homeVisibilityEventId) {
        if (homeVisibilityEventId > lastConsumedVisibilityEventId) {
            isBalanceVisible = homeVisibilityVisible
            lastConsumedVisibilityEventId = homeVisibilityEventId
        }
    }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var selectionMode by remember { mutableStateOf(false) }
    val selectedTransactions = remember { mutableStateOf<Long>(0L) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    
    var swipedOpenTransactionId by remember { mutableStateOf<Long?>(null) }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var hasHadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    val displayIncome = CurrencyUtils.convertToDisplay(monthlyIncome, currency)
    val displayExpense = CurrencyUtils.convertToDisplay(monthlyExpense, currency)
    val displayBalance = displayIncome - displayExpense

    val hasActiveFilter = filterCategoryId != null || filterStartDate != null || filterEndDate != null

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
                            if (isSearchExpanded) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusEvent { focusState ->
                                            if (focusState.isFocused) {
                                                hasHadFocus = true
                                            } else if (hasHadFocus && searchQuery.isBlank()) {
                                                isSearchExpanded = false
                                                hasHadFocus = false
                                            }
                                        },
                                    placeholder = {
                                        Text(
                                            strings.searchHint,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onSearch = {
                                            if (searchQuery.isNotBlank()) {
                                                onNavigateToSearchResult(searchQuery.trim())
                                                isSearchExpanded = false
                                                searchQuery = ""
                                                hasHadFocus = false
                                                keyboardController?.hide()
                                            }
                                        }
                                    )
                                )
                            } else {
                                Column {
                                    Text(
                                        "AccountKeeper",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Manage your finances",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isSearchExpanded) {
                                TextButton(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            onNavigateToSearchResult(searchQuery.trim())
                                            isSearchExpanded = false
                                            searchQuery = ""
                                            hasHadFocus = false
                                        }
                                    },
                                    enabled = searchQuery.isNotBlank()
                                ) {
                                    Text(
                                        strings.search,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                IconButton(onClick = { showFilterSheet = true }) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = strings.filter,
                                        tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { showSortDialog = true }) {
                                    Icon(
                                        Icons.Default.Sort,
                                        contentDescription = strings.sort,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    isSearchExpanded = true
                                    hasHadFocus = false
                                }) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = strings.search,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                    LaunchedEffect(isSearchExpanded) {
                        if (isSearchExpanded) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddTransaction,
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
                        contentDescription = strings.addTransaction,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        val pagedList = remember(pagedTransactions.itemCount) {
            (0 until pagedTransactions.itemCount).mapNotNull { pagedTransactions[it] }
        }

        val sortedPagedList = remember(pagedList, sortType) {
            when (sortType) {
                SortType.TIME_DESC -> pagedList.sortedByDescending { it.date }
                SortType.TIME_ASC -> pagedList.sortedBy { it.date }
                SortType.AMOUNT_DESC -> pagedList.sortedByDescending { it.amount }
                SortType.AMOUNT_ASC -> pagedList.sortedBy { it.amount }
            }
        }

        val groupedPagedTransactions = sortedPagedList.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        if (swipedOpenTransactionId != null) {
                            swipedOpenTransactionId = null
                        }
                        focusManager.clearFocus()
                        if (searchQuery.isBlank()) {
                            isSearchExpanded = false
                        }
                    })
                },
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!selectionMode) {
                item {
                    PremiumBalanceCard(
                        totalBalance = displayBalance,
                        totalIncome = displayIncome,
                        totalExpense = displayExpense,
                        currency = currency,
                        isVisible = isBalanceVisible,
                        onToggleVisibility = { isBalanceVisible = !isBalanceVisible },
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { viewModel.selectedTimeRange.value = it },
                        strings = strings
                    )
                }
            }

            if (hasActiveFilter) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (filterCategoryId != null) {
                            val catName = categories.find { it.id == filterCategoryId }?.name ?: ""
                            AssistChip(
                                onClick = { viewModel.filterCategoryId.value = null },
                                label = { Text(catName) },
                                trailingIcon = {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        if (filterStartDate != null || filterEndDate != null) {
                            AssistChip(
                                onClick = {
                                    viewModel.filterStartDate.value = null
                                    viewModel.filterEndDate.value = null
                                },
                                label = { Text(strings.dateRange) },
                                trailingIcon = {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                        AssistChip(
                            onClick = {
                                viewModel.filterCategoryId.value = null
                                viewModel.filterStartDate.value = null
                                viewModel.filterEndDate.value = null
                            },
                            label = { Text(strings.clearFilter) }
                        )
                    }
                }
            }

            if (isBalanceVisible) {
                groupedPagedTransactions.forEach { (dateString, txList) ->
                    item(key = "header_$dateString") {
                        DateHeader(
                            date = dateString,
                            txList = txList,
                            currency = currency,
                            strings = strings
                        )
                    }

                    items(
                        items = txList,
                        key = { it.id }
                    ) { transaction ->
                        val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: strings.other
                        val isSelected = selectedIds.contains(transaction.id)
                        PremiumTransactionItem(
                            transaction = transaction,
                            categoryName = categoryName,
                            currency = currency,
                            swipedOpenId = swipedOpenTransactionId,
                            onSwipeOpen = { swipedOpenTransactionId = transaction.id },
                            onClick = {
                                if (swipedOpenTransactionId != null && swipedOpenTransactionId != transaction.id) {
                                    swipedOpenTransactionId = null
                                }
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
                                if (swipedOpenTransactionId != null) {
                                    swipedOpenTransactionId = null
                                }
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
                            },
                            isSelected = isSelected,
                            inSelectionMode = selectionMode,
                            swipeDeleteRequiresConfirm = appSettings.swipeDeleteRequiresConfirm
                        )
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "••••",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (showDeleteDialog && transactionToDelete != null) {
            DeleteTransactionDialog(
                onConfirm = {
                    transactionToDelete?.let { viewModel.deleteTransaction(it) }
                    showDeleteDialog = false
                    transactionToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    transactionToDelete = null
                },
                strings = strings
            )
        }

        if (showBatchDeleteDialog && selectedIds.isNotEmpty()) {
            BatchDeleteTransactionDialog(
                count = selectedIds.size,
                onConfirm = {
                    val pagedListForDelete = (0 until pagedTransactions.itemCount).mapNotNull { pagedTransactions[it] }
                    val transactionsToDelete = pagedListForDelete.filter { it.id in selectedIds }
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

        if (showFilterSheet) {
            HomeFilterDialog(
                filterCategoryId = filterCategoryId,
                onCategorySelected = { viewModel.filterCategoryId.value = it },
                filterStartDate = filterStartDate,
                onStartDateSelected = { viewModel.filterStartDate.value = it },
                filterEndDate = filterEndDate,
                onEndDateSelected = { viewModel.filterEndDate.value = it },
                categories = categories,
                onDismiss = { showFilterSheet = false },
                strings = strings
            )
        }

        if (showSortDialog) {
            HomeSortDialog(
                sortType = sortType,
                onSortTypeSelected = { viewModel.sortType.value = it },
                onDismiss = { showSortDialog = false },
                strings = strings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFilterDialog(
    filterCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
    filterStartDate: Long?,
    onStartDateSelected: (Long?) -> Unit,
    filterEndDate: Long?,
    onEndDateSelected: (Long?) -> Unit,
    categories: List<com.example.accountkeeper.data.model.Category>,
    onDismiss: () -> Unit,
    strings: AppStrings
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.filter, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(strings.dateRange, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            filterStartDate?.let {
                                SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it))
                            } ?: strings.startDate,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            filterEndDate?.let {
                                SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it))
                            } ?: strings.endDate,
                            maxLines = 1
                        )
                    }
                }

                Text(strings.categoryFilter, style = MaterialTheme.typography.labelLarge)
                val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = expenseCategories.find { it.id == filterCategoryId }?.name ?: strings.allCategories,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.allCategories) },
                            onClick = {
                                onCategorySelected(null)
                                categoryExpanded = false
                            }
                        )
                        expenseCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onCategorySelected(category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    onStartDateSelected(null)
                    onEndDateSelected(null)
                    onCategorySelected(null)
                    onDismiss()
                }) {
                    Text(strings.clearFilter)
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.ok)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onStartDateSelected(it) }
                    showStartDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onEndDateSelected(it) }
                    showEndDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun HomeSortDialog(
    sortType: SortType,
    onSortTypeSelected: (SortType) -> Unit,
    onDismiss: () -> Unit,
    strings: AppStrings
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.sort, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortType.values().forEach { type ->
                    val label = when (type) {
                        SortType.TIME_DESC -> strings.timeDescending
                        SortType.TIME_ASC -> strings.timeAscending
                        SortType.AMOUNT_DESC -> strings.amountDescending
                        SortType.AMOUNT_ASC -> strings.amountAscending
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortTypeSelected(type)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortType == type,
                            onClick = {
                                onSortTypeSelected(type)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
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
fun PremiumBalanceCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currency: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    strings: AppStrings
) {
    val gradient = if (isSystemInDarkTheme()) {
        Brush.linearGradient(
            colors = DarkGradientPrimary,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = LightGradientPrimary,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = (-70).dp, y = (-70).dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 80.dp, y = 60.dp)
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
            )

    var timeRangeExpanded by remember { mutableStateOf(false) }

    Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { timeRangeExpanded = true }
                    ) {
                        Text(
                            when (selectedTimeRange) {
                                TimeRange.MONTH -> strings.monthly
                                TimeRange.YEAR -> strings.yearly
                                TimeRange.ALL -> strings.all
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        DropdownMenu(
                            expanded = timeRangeExpanded,
                            onDismissRequest = { timeRangeExpanded = false }
                        ) {
                            TimeRange.entries.forEach { range ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (range) {
                                                TimeRange.MONTH -> strings.monthly
                                                TimeRange.YEAR -> strings.yearly
                                                TimeRange.ALL -> strings.all
                                            }
                                        )
                                    },
                                    onClick = {
                                        onTimeRangeSelected(range)
                                        timeRangeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onToggleVisibility() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isVisible) strings.hideAmount else strings.showAmount,
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        strings.monthlyBalance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isVisible) "$currency${String.format(Locale.US, "%.2f", totalBalance)}" else "••••",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BalanceStat(
                                label = strings.income,
                                value = if (isVisible) totalIncome else null,
                                currency = currency,
                                color = Color(0xFF5BD9CA)
                            )
                            VerticalDivider(
                                color = Color.White.copy(alpha = 0.25f),
                                thickness = 1.dp,
                                modifier = Modifier.height(50.dp)
                            )
                            BalanceStat(
                                label = strings.expense,
                                value = if (isVisible) totalExpense else null,
                                currency = currency,
                                color = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceStat(
    label: String,
    value: Double?,
    currency: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value?.let { "$currency${String.format(Locale.US, "%.2f", it)}" } ?: "••••",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DateHeader(
    date: String,
    txList: List<Transaction>,
    currency: String,
    strings: AppStrings
) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumTransactionItem(
    transaction: Transaction,
    categoryName: String,
    currency: String,
    swipedOpenId: Long?,
    onSwipeOpen: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false,
    inSelectionMode: Boolean = false,
    swipeDeleteRequiresConfirm: Boolean = true
) {
    val strings = LocalAppStrings.current
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
                if (inSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

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
        var offsetX by remember { mutableStateOf(0f) }
        val maxOffset = 300f
        
        var isDragging by remember { mutableStateOf(false) }
        var hasTriggeredAction by remember { mutableStateOf(false) }
        
        var showDeleteDialog by remember { mutableStateOf(false) }
        
        var isSwipedOpen by remember { mutableStateOf(false) }
        
        LaunchedEffect(swipedOpenId) {
            if (isSwipedOpen && swipedOpenId != transaction.id) {
                isSwipedOpen = false
                animate(
                    initialValue = offsetX,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) { value, _ ->
                    offsetX = value
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .draggable(
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetX + delta
                        offsetX = newOffset.coerceIn(-maxOffset, 0f)
                        isDragging = true
                    },
                    onDragStopped = {
                        isDragging = false
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        isSwipedOpen = false
                        offsetX = 0f
                        showDeleteDialog = true
                    }
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
            
            LaunchedEffect(isDragging) {
                if (!isDragging && offsetX != 0f) {
                    if (offsetX <= -maxOffset * 0.3f && !hasTriggeredAction) {
                        if (swipeDeleteRequiresConfirm) {
                            showDeleteDialog = true
                            hasTriggeredAction = true
                            animate(
                                initialValue = offsetX,
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) { value, _ ->
                                offsetX = value
                            }
                        } else {
                            isSwipedOpen = true
                            onSwipeOpen()
                            hasTriggeredAction = true
                            animate(
                                initialValue = offsetX,
                                targetValue = -maxOffset * 0.7f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) { value, _ ->
                                offsetX = value
                            }
                        }
                    } else if (!isSwipedOpen) {
                        animate(
                            initialValue = offsetX,
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) { value, _ ->
                            offsetX = value
                        }
                    }
                }
            }
            
            LaunchedEffect(showDeleteDialog) {
                if (showDeleteDialog && isSwipedOpen) {
                    isSwipedOpen = false
                    animate(
                        initialValue = offsetX,
                        targetValue = 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) { value, _ ->
                        offsetX = value
                    }
                }
                if (!showDeleteDialog) {
                    hasTriggeredAction = false
                }
            }
            
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text(strings.deleteConfirmTitle) },
                    text = { Text(strings.deleteConfirmMessage) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDelete()
                            }
                        ) {
                            Text(strings.delete, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text(strings.cancel)
                        }
                    }
                )
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.toInt(), 0) }
                    .scale(scale)
                    .combinedClickable(
                        onClick = {
                            if (isSwipedOpen) {
                                isSwipedOpen = false
                                offsetX = 0f
                            } else {
                                onClick()
                            }
                        },
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

@Composable
fun DeleteTransactionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: AppStrings
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.rotate(45f)
            )
        },
        title = {
            Text(
                strings.deleteTransaction,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(strings.deleteConfirm)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun BatchDeleteTransactionDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: AppStrings
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.rotate(45f)
            )
        },
        title = {
            Text(
                strings.deleteTransaction,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("$count ${strings.deleteConfirm}")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
