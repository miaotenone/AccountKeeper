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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    // 用于列表显示（分页数据）
    val pagedTransactions = viewModel.pagedTransactions.collectAsLazyPagingItems()
    // 用于统计显示（ViewModel 缓存）
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalBalanceValue by viewModel.totalBalance.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyBalanceValue by viewModel.monthlyBalance.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current

    var isShowingMonthly by remember { mutableStateOf(false) }
    var isBalanceCardExpanded by remember { mutableStateOf(true) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    var selectionMode by remember { mutableStateOf(false) }
    val selectedTransactions = remember { mutableStateOf<Long>(0L) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    
    // Track which card is swiped open (for closing when clicking elsewhere)
    var swipedOpenTransactionId by remember { mutableStateOf<Long?>(null) }

    // Search state
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var hasHadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val lazyListState = rememberLazyListState()

    // 使用 ViewModel 缓存的统计数据，根据显示模式选择
    val displayIncome = CurrencyUtils.convertToDisplay(
        if (isShowingMonthly) monthlyIncome else totalIncome,
        currency
    )
    val displayExpense = CurrencyUtils.convertToDisplay(
        if (isShowingMonthly) monthlyExpense else totalExpense,
        currency
    )
    val displayBalance = displayIncome - displayExpense

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
                                                // Only close if we had focus before and query is blank
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
        // 将分页数据转换为列表用于分组显示
        val pagedList = remember(pagedTransactions.itemCount) {
            (0 until pagedTransactions.itemCount).mapNotNull { pagedTransactions[it] }
        }

        // 按日期分组
        val groupedPagedTransactions = pagedList.groupBy {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        // Reset swiped open card
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 余额卡片
            if (!selectionMode) {
                item {
                    PremiumBalanceCard(
                        totalBalance = displayBalance,
                        totalIncome = displayIncome,
                        totalExpense = displayExpense,
                        currency = currency,
                        isShowingMonthly = isShowingMonthly,
                        isExpanded = isBalanceCardExpanded,
                        onTogglePeriod = { isShowingMonthly = !isShowingMonthly },
                        onToggleExpand = { isBalanceCardExpanded = !isBalanceCardExpanded },
                        strings = strings
                    )
                }
            }

            // 分页数据显示 - 按日期分组
            groupedPagedTransactions.forEach { (dateString, txList) ->
                // 日期头部
                item(key = "header_$dateString") {
                    DateHeader(
                        date = dateString,
                        txList = txList,
                        currency = currency,
                        strings = strings
                    )
                }

                // 交易列表
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
                            // Close any swiped open card first
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
                            // Close any swiped open card first
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

            // 底部间距
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
fun PremiumBalanceCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currency: String,
    isShowingMonthly: Boolean,
    isExpanded: Boolean,
    onTogglePeriod: () -> Unit,
    onToggleExpand: () -> Unit,
    strings: AppStrings
) {
    val density = LocalDensity.current
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

    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) Dp.Infinity else Dp.Infinity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardHeight"
    )

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
            // Decorative circles
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onTogglePeriod() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (isShowingMonthly) strings.thisMonth else strings.totalAssets,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Icon(
                            if (isShowingMonthly) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle period",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle expand",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total Balance - always visible
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        strings.totalBalance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currency${String.format(Locale.US, "%.2f", totalBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
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
                        // Income and Expense row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BalanceStat(
                                label = strings.income,
                                value = totalIncome,
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
                                value = totalExpense,
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
    value: Double,
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
            "$currency${String.format(Locale.US, "%.2f", value)}",
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
        
        // Track if user is dragging
        var isDragging by remember { mutableStateOf(false) }
        var hasTriggeredAction by remember { mutableStateOf(false) }
        
        // Delete confirmation dialog state
        var showDeleteDialog by remember { mutableStateOf(false) }
        
        // Track if card is swiped open (showing delete button)
        var isSwipedOpen by remember { mutableStateOf(false) }
        
        // Reset when another card is swiped open or when clicking elsewhere (swipedOpenId becomes null)
        LaunchedEffect(swipedOpenId) {
            if (isSwipedOpen && swipedOpenId != transaction.id) {
                // Either another card was swiped open, or swipedOpenId was set to null (click elsewhere)
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
            // Red delete background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.error,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        // Reset card position first
                        isSwipedOpen = false
                        offsetX = 0f
                        // Show confirmation dialog
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
            
            // Animate offset back to 0 when not dragging
            LaunchedEffect(isDragging) {
                if (!isDragging && offsetX != 0f) {
                    // Check if we should trigger action based on settings
                    if (offsetX <= -maxOffset * 0.3f && !hasTriggeredAction) {
                        // Left swipe triggered delete
                        if (swipeDeleteRequiresConfirm) {
                            // If setting is ON, show confirmation dialog directly and reset
                            showDeleteDialog = true
                            hasTriggeredAction = true
                            // Animate back to 0
                            animate(
                                initialValue = offsetX,
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) { value, _ ->
                                offsetX = value
                            }
                        } else {
                            // If setting is OFF, keep card swiped open showing delete button
                            isSwipedOpen = true
                            onSwipeOpen() // Notify parent that this card is swiped open
                            hasTriggeredAction = true
                            // Snap to threshold position to show delete button (70%)
                            animate(
                                initialValue = offsetX,
                                targetValue = -maxOffset * 0.7f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) { value, _ ->
                                offsetX = value
                            }
                        }
                    } else if (!isSwipedOpen) {
                        // Not triggered action and not swiped open, animate back to 0
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
            
            // Reset swiped state when dialog is shown
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
                // Reset hasTriggeredAction when dialog is closed
                if (!showDeleteDialog) {
                    hasTriggeredAction = false
                }
            }
            
            // Delete confirmation dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("确认删除") },
                    text = { Text("确定要删除这条交易记录吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                onDelete()
                            }
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
            
            // Transaction card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.toInt(), 0) }
                    .scale(scale)
                    .combinedClickable(
                        onClick = {
                            if (isSwipedOpen) {
                                // If card is swiped open, close it instead of triggering onClick
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