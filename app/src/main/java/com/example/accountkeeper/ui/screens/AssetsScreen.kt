package com.example.accountkeeper.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.AssetViewModel
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.screens.SortType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    onNavigateToAddAsset: () -> Unit,
    onNavigateToEditAsset: (Long) -> Unit,
    viewModel: AssetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val assets by viewModel.assets.collectAsState()
    val allCategories by categoryViewModel.categories.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current
    
    // 只显示资产类型的分类
    val assetCategories = remember(allCategories) {
        allCategories.filter { it.type == TransactionType.ASSET }
    }
    val categories = assetCategories

    val netAssets by viewModel.netAssets.collectAsState()
    val totalAssets by viewModel.totalAssets.collectAsState()
    val totalLiabilities by viewModel.totalLiabilities.collectAsState()
    val transactionBalance by viewModel.transactionBalance.collectAsState()
    val positiveAssetAmount by viewModel.positiveAssetAmount.collectAsState()
    val negativeAssetAmount by viewModel.negativeAssetAmount.collectAsState()

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    
    // Track which card is swiped open (for closing when clicking elsewhere)
    var swipedOpenAssetId by remember { mutableStateOf<Long?>(null) }

    // Search state
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var hasHadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Filter and sort state
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var filterStartDate by remember { mutableStateOf<Long?>(null) }
    var filterEndDate by remember { mutableStateOf<Long?>(null) }
    var filterCategoryId by remember { mutableStateOf<Long?>(null) }
    var sortType by remember { mutableStateOf(SortType.TIME_DESC) }

    // Filter and sort assets
    val displayAssets = remember(assets, searchQuery, categories, filterStartDate, filterEndDate, filterCategoryId, sortType) {
        var filtered = assets
        
        // Apply search filter
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            filtered = filtered.filter { asset ->
                val categoryName = categories.find { it.id == asset.categoryId }?.name ?: ""
                val statusName = when (asset.status) {
                    AssetStatus.NONE -> "未选择 none"
                    AssetStatus.OWNED -> "确定拥有 owned"
                    AssetStatus.NOT_OWNED -> "确定没有 not owned"
                    AssetStatus.IN_PROGRESS -> "进行中 in progress"
                    AssetStatus.TEMPORARILY_WITH_ME -> "暂时在自己手里 temporarily with me"
                    AssetStatus.TEMPORARILY_WITH_OTHERS -> "暂时在别人手里 temporarily with others"
                }
                asset.note.lowercase().contains(query) ||
                asset.targetPerson.lowercase().contains(query) ||
                asset.targetAccount.lowercase().contains(query) ||
                categoryName.lowercase().contains(query) ||
                statusName.lowercase().contains(query)
            }
        }
        
        // Apply time range filter
        if (filterStartDate != null) {
            filtered = filtered.filter { it.date >= filterStartDate!! }
        }
        if (filterEndDate != null) {
            // filterEndDate 已经在 DatePicker 中设置为当天 23:59:59.999
            filtered = filtered.filter { it.date <= filterEndDate!! }
        }
        
        // Apply category filter
        if (filterCategoryId != null) {
            filtered = filtered.filter { it.categoryId == filterCategoryId }
        }
        
        // Apply sort
        when (sortType) {
            SortType.TIME_DESC -> filtered.sortedByDescending { it.date }
            SortType.TIME_ASC -> filtered.sortedBy { it.date }
            SortType.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
            SortType.AMOUNT_ASC -> filtered.sortedBy { it.amount }
        }
    }

    // 是否有筛选条件激活
    val hasActiveFilter = filterStartDate != null || filterEndDate != null || filterCategoryId != null
    
    // 是否有分类筛选（用于简化卡片显示）
    val hasCategoryFilter = filterCategoryId != null
    
    // 获取筛选的分类信息
    val filteredCategory = remember(filterCategoryId, categories) {
        filterCategoryId?.let { id -> categories.find { it.id == id } }
    }

    // 计算筛选后的统计数据
    val filteredStats = remember(displayAssets, categories) {
        val positiveCategoryIds = categories.filter { it.isPositiveAsset }.map { it.id }.toSet()
        val negativeCategoryIds = categories.filter { !it.isPositiveAsset }.map { it.id }.toSet()
        
        // 筛选后的正资产金额
        val filteredPositiveAmount = displayAssets.filter { it.categoryId in positiveCategoryIds }.sumOf { asset ->
            when {
                asset.status == AssetStatus.OWNED -> asset.amount
                asset.isCompleted -> asset.amount
                else -> 0.0
            }
        }
        
        // 筛选后的负资产金额
        val filteredNegativeAmount = displayAssets.filter { it.categoryId in negativeCategoryIds }.sumOf { asset ->
            when {
                asset.status == AssetStatus.OWNED -> asset.amount
                asset.status == AssetStatus.IN_PROGRESS && !asset.isCompleted -> asset.amount
                else -> 0.0
            }
        }
        
        // 筛选后的总负债
        val filteredTotalLiabilities = displayAssets.filter { 
            it.categoryId in negativeCategoryIds && 
            it.status == AssetStatus.IN_PROGRESS && 
            !it.isCompleted 
        }.sumOf { it.amount }
        
        // 筛选后的资产总额（不含交易结余，因为交易结余无法按资产分类筛选）
        val filteredTotalAssets = filteredPositiveAmount + filteredNegativeAmount
        
        // 筛选后的净资产
        val filteredNetAssets = filteredTotalAssets - filteredNegativeAmount
        
        Triple(filteredPositiveAmount, filteredNegativeAmount, filteredTotalLiabilities)
    }

    // 根据筛选条件决定显示的数据
    val displayPositiveAssetAmount = if (hasActiveFilter) filteredStats.first else positiveAssetAmount
    val displayNegativeAssetAmount = if (hasActiveFilter) filteredStats.second else negativeAssetAmount
    val displayTotalLiabilities = if (hasActiveFilter) filteredStats.third else totalLiabilities
    // 交易结余无法按资产筛选，始终显示全局值
    val displayTransactionBalance = transactionBalance
    // 总资产 = 正资产 + 负资产 + 交易结余（无筛选时直接使用 ViewModel 计算的值）
    val displayTotalAssets = if (hasActiveFilter) {
        displayPositiveAssetAmount + displayNegativeAssetAmount  // 筛选时不计入交易结余
    } else {
        totalAssets  // 直接使用 ViewModel 已正确计算的总资产
    }
    // 净资产 = 正资产 + 综合（交易结余） - 负债（进行中）
    // 无筛选时直接使用 ViewModel 计算的值
    val displayNetAssets = if (hasActiveFilter) {
        displayPositiveAssetAmount - displayTotalLiabilities  // 筛选时不计入交易结余
    } else {
        netAssets  // 直接使用 ViewModel 已正确计算的净资产
    }
    
    // 分类筛选时的详细状态统计
    data class CategoryStats(
        val totalAmount: Double,        // 总金额：该分类所有资产
        val inProgressAmount: Double,   // 进行中状态金额
        val statusAmount: Double        // 正资产：OWNED金额；负资产：NOT_OWNED金额
    )
    
    val categoryFilteredStats = remember(displayAssets, filteredCategory) {
        if (filteredCategory != null) {
            val categoryAssets = displayAssets.filter { it.categoryId == filteredCategory.id }
            
            // 总金额：该分类所有资产金额全部加起来
            val totalAmount = categoryAssets.sumOf { it.amount }
            
            // 进行中：所有 IN_PROGRESS 状态的金额
            val inProgressAmount = categoryAssets.filter { it.status == AssetStatus.IN_PROGRESS }.sumOf { it.amount }
            
            // 根据分类类型计算状态金额
            val statusAmount = if (filteredCategory.isPositiveAsset) {
                // 正资产：OWNED状态金额（确认拥有）
                categoryAssets.filter { it.status == AssetStatus.OWNED }.sumOf { it.amount }
            } else {
                // 负资产：NOT_OWNED状态金额（确认没有）
                categoryAssets.filter { it.status == AssetStatus.NOT_OWNED }.sumOf { it.amount }
            }
            
            CategoryStats(totalAmount, inProgressAmount, statusAmount)
        } else {
            CategoryStats(0.0, 0.0, 0.0)
        }
    }

    // Group assets by date
    val groupedAssets = displayAssets.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
    }.toSortedMap(reverseOrder())

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
                                        onNavigateToEditAsset(selectedIds.first())
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
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                        }
                                    )
                                )
                            } else {
                                Column {
                                    Text(
                                        strings.assets,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        strings.assetsDescription,
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
                                        searchQuery = ""
                                        isSearchExpanded = false
                                        hasHadFocus = false
                                    }
                                ) {
                                    Text(strings.cancel, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                IconButton(onClick = { showFilterDialog = true }) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = strings.filter,
                                        tint = if (filterStartDate != null || filterEndDate != null || filterCategoryId != null) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
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
                    onClick = onNavigateToAddAsset,
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
                        contentDescription = strings.addAsset,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        // Reset swiped open card
                        if (swipedOpenAssetId != null) {
                            swipedOpenAssetId = null
                        }
                        focusManager.clearFocus()
                        if (searchQuery.isBlank()) {
                            isSearchExpanded = false
                        }
                    })
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 统计卡片 - 根据筛选条件显示不同内容
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

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(gradient)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 根据是否有分类筛选显示不同内容
                    if (hasCategoryFilter && filteredCategory != null) {
                        // 分类筛选模式：显示分类名称、总金额和状态统计
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                filteredCategory.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (filteredCategory.isPositiveAsset) "(${strings.positiveAsset})" else "(${strings.negativeAsset})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$currency${String.format(Locale.US, "%.2f", categoryFilteredStats.totalAmount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 显示两个状态的统计
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 进行中状态金额
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    strings.inProgress,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", categoryFilteredStats.inProgressAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC107)
                                )
                            }
                            // 根据分类类型显示不同状态
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (filteredCategory.isPositiveAsset) strings.owned else strings.notOwned,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", categoryFilteredStats.statusAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                        }
                    } else {
                        // 无分类筛选：显示完整统计
                        // 净资产（主要显示）
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                strings.netAssets,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            // 如果有筛选条件，显示标识
                            if (hasActiveFilter) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "(${strings.filter})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$currency${String.format(Locale.US, "%.2f", displayNetAssets)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 第一行：总资产、交易结余（筛选时不显示交易结余）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    strings.totalAssets,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", displayTotalAssets)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5BD9CA)
                                )
                            }
                            // 只有在没有筛选条件时才显示交易结余
                            if (!hasActiveFilter) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        strings.balanceOverall,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        "$currency${String.format(Locale.US, "%.2f", displayTransactionBalance)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7DD3FC)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 第二行：正资产、总负债
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    strings.positiveAsset,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", displayPositiveAssetAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    strings.totalLiabilities,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    "$currency${String.format(Locale.US, "%.2f", displayTotalLiabilities)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }
                }
            }

            // Asset list
            if (displayAssets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.noAssets,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                groupedAssets.forEach { (dateString, assetList) ->
                    // Date Header
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            dateString,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Asset Items
                    assetList.forEach { asset ->
                        val category = categories.find { it.id == asset.categoryId }
                        val categoryName = category?.name ?: strings.other
                        val isPositiveCategory = category?.isPositiveAsset ?: true
                        val isSelected = selectedIds.contains(asset.id)
                        
                        AssetItem(
                            asset = asset,
                            categoryName = categoryName,
                            isPositiveCategory = isPositiveCategory,
                            currency = currency,
                            isSelected = isSelected,
                            inSelectionMode = selectionMode,
                            strings = strings,
                            swipeDeleteRequiresConfirm = appSettings.swipeDeleteRequiresConfirm,
                            swipedOpenId = swipedOpenAssetId,
                            onSwipeOpen = { swipedOpenAssetId = asset.id },
                            onClick = {
                                // Close any swiped open card first
                                if (swipedOpenAssetId != null && swipedOpenAssetId != asset.id) {
                                    swipedOpenAssetId = null
                                }
                                if (selectionMode) {
                                    if (isSelected) {
                                        selectedIds.remove(asset.id)
                                    } else {
                                        selectedIds.add(asset.id)
                                    }
                                } else {
                                    onNavigateToEditAsset(asset.id)
                                }
                            },
                            onLongClick = {
                                // Close any swiped open card first
                                if (swipedOpenAssetId != null) {
                                    swipedOpenAssetId = null
                                }
                                if (selectionMode) {
                                    if (isSelected) {
                                        selectedIds.remove(asset.id)
                                    } else {
                                        selectedIds.add(asset.id)
                                    }
                                } else {
                                    selectionMode = true
                                    selectedIds.add(asset.id)
                                }
                            },
                            onDelete = { viewModel.deleteAsset(asset) },
                            onToggleStatus = { viewModel.toggleAssetStatus(asset, isPositiveCategory) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Batch delete dialog
        if (showBatchDeleteDialog && selectedIds.isNotEmpty()) {
            BatchDeleteAssetDialog(
                count = selectedIds.size,
                onConfirm = {
                    val assetsToDelete = displayAssets.filter { it.id in selectedIds }
                    viewModel.deleteAssets(assetsToDelete)
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
        
        // Filter dialog
        if (showFilterDialog) {
            var showStartDatePicker by remember { mutableStateOf(false) }
            var showEndDatePicker by remember { mutableStateOf(false) }
            
            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = filterStartDate ?: System.currentTimeMillis()
                )
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { dateVal ->
                                // 转换为本地时区时间，设置为当天 00:00:00
                                val c = java.util.Calendar.getInstance()
                                c.timeInMillis = dateVal
                                c.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                c.set(java.util.Calendar.MINUTE, 0)
                                c.set(java.util.Calendar.SECOND, 0)
                                c.set(java.util.Calendar.MILLISECOND, 0)
                                filterStartDate = c.timeInMillis
                            }
                            showStartDatePicker = false
                        }) {
                            Text(strings.ok)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) {
                            Text(strings.cancel)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            if (showEndDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = filterEndDate ?: System.currentTimeMillis()
                )
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { dateVal ->
                                // 转换为本地时区时间，设置为当天 23:59:59
                                val c = java.util.Calendar.getInstance()
                                c.timeInMillis = dateVal
                                c.set(java.util.Calendar.HOUR_OF_DAY, 23)
                                c.set(java.util.Calendar.MINUTE, 59)
                                c.set(java.util.Calendar.SECOND, 59)
                                c.set(java.util.Calendar.MILLISECOND, 999)
                                filterEndDate = c.timeInMillis
                            }
                            showEndDatePicker = false
                        }) {
                            Text(strings.ok)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) {
                            Text(strings.cancel)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text(strings.filter, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Time range
                        Text(strings.timeRange, style = MaterialTheme.typography.labelLarge)
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
                        
                        // Category filter - 只显示资产类型的分类
                        Text(strings.categoryFilter, style = MaterialTheme.typography.labelLarge)
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = assetCategories.find { it.id == filterCategoryId }?.name ?: strings.allCategories,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.allCategories) },
                                    onClick = {
                                        filterCategoryId = null
                                        expanded = false
                                    }
                                )
                                assetCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(category.name)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    if (category.isPositiveAsset) "(${strings.positiveAsset})" else "(${strings.negativeAsset})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (category.isPositiveAsset) 
                                                        Color(0xFF4ADE80) 
                                                    else 
                                                        Color(0xFFFF6B6B)
                                                )
                                            }
                                        },
                                        onClick = {
                                            filterCategoryId = category.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row {
                        // 清除筛选按钮
                        TextButton(onClick = {
                            filterStartDate = null
                            filterEndDate = null
                            filterCategoryId = null
                            showFilterDialog = false
                        }) {
                            Text(strings.clearFilter)
                        }
                        TextButton(onClick = { showFilterDialog = false }) {
                            Text(strings.ok)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
        
        // Sort dialog
        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
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
                                        sortType = type
                                        showSortDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = sortType == type,
                                    onClick = {
                                        sortType = type
                                        showSortDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text(strings.ok)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetItem(
    asset: Asset,
    categoryName: String,
    isPositiveCategory: Boolean,
    currency: String,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    strings: AppStrings,
    swipeDeleteRequiresConfirm: Boolean,
    swipedOpenId: Long?,
    onSwipeOpen: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    // Determine card color based on status
    val statusColor = when (asset.status) {
        AssetStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        AssetStatus.OWNED -> Color(0xFF4CAF50)  // Green - confirmed owned
        AssetStatus.NOT_OWNED -> Color(0xFF9E9E9E)  // Gray - confirmed not owned
        AssetStatus.IN_PROGRESS -> Color(0xFFFFC107)  // Yellow - in progress
        AssetStatus.TEMPORARILY_WITH_ME -> Color(0xFFFFC107)  // Legacy - yellow
        AssetStatus.TEMPORARILY_WITH_OTHERS -> Color(0xFFFFC107)  // Legacy - yellow
    }
    
    val cardColor = statusColor

    var offsetX by remember { mutableStateOf(0f) }
    val maxOffset = 300f
    
    // Track if user is dragging
    var isDragging by remember { mutableStateOf(false) }
    var hasTriggeredAction by remember { mutableStateOf(false) }

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "backgroundColor"
    )

    // Track if card is swiped open (showing delete button)
    var isSwipedOpen by remember { mutableStateOf(false) }

    // Reset when another card is swiped open or when clicking elsewhere (swipedOpenId becomes null)
    LaunchedEffect(swipedOpenId) {
        if (isSwipedOpen && swipedOpenId != asset.id) {
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
            } else if (offsetX >= maxOffset * 0.3f && !hasTriggeredAction) {
                // Right swipe triggered status toggle
                onToggleStatus()
                hasTriggeredAction = true
                // Animate back to 0
                animate(
                    initialValue = offsetX,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) { value, _ ->
                    offsetX = value
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

    // Reset swiped state when dialog is shown or delete is triggered
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
    }

    if (inSelectionMode) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { onLongClick() }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                
                // Category icon (clickable for status toggle)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(cardColor, CircleShape)
                        .clickable { onToggleStatus() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        categoryName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // 正/负资产标签
                        Text(
                            if (isPositiveCategory) "[${strings.positiveAsset}]" else "[${strings.negativeAsset}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPositiveCategory) Color(0xFF4ADE80) else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (asset.targetPerson.isNotBlank()) {
                        Text(
                            "${strings.targetPerson}: ${asset.targetPerson}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (asset.targetAccount.isNotBlank()) {
                        Text(
                            "${strings.targetAccount}: ${asset.targetAccount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        timeFormat.format(Date(asset.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status label and amount
                Column(horizontalAlignment = Alignment.End) {
                    // Status label
                    val statusLabel = when (asset.status) {
                        AssetStatus.NONE -> null
                        AssetStatus.OWNED -> strings.owned
                        AssetStatus.NOT_OWNED -> strings.notOwned
                        AssetStatus.IN_PROGRESS -> strings.inProgress
                        AssetStatus.TEMPORARILY_WITH_ME -> strings.temporarilyWithMe
                        AssetStatus.TEMPORARILY_WITH_OTHERS -> strings.temporarilyWithOthers
                    }
                    if (statusLabel != null) {
                        Text(
                            statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = cardColor
                        )
                    }
                    Text(
                        "$currency${String.format(Locale.US, "%.2f", asset.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cardColor
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .draggable(
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetX + delta
                        offsetX = newOffset.coerceIn(-maxOffset, maxOffset)
                        hasTriggeredAction = false
                    },
                    onDragStarted = {
                        isDragging = true
                        hasTriggeredAction = false
                    },
                    onDragStopped = {
                        isDragging = false
                    }
                )
        ) {
            // Left side: Status change background (green)
            if (offsetX > 0) {
                // Determine action text based on current status and category type
                val actionText = when (asset.status) {
                    AssetStatus.IN_PROGRESS -> {
                        if (isPositiveCategory) strings.markAsOwned else strings.markAsNotOwned
                    }
                    else -> strings.markInProgress
                }
                
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0xFF4CAF50), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            actionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )
                    }
                }
            }
            
            // Right side: Delete background (red)
            if (offsetX < 0) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(20.dp))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.rotate(45f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.delete,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
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
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon (clickable for status toggle)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(cardColor, CircleShape)
                            .clickable { onToggleStatus() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            categoryName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // 正/负资产标签
                            Text(
                                if (isPositiveCategory) "[${strings.positiveAsset}]" else "[${strings.negativeAsset}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPositiveCategory) Color(0xFF4ADE80) else Color(0xFFFF6B6B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Show note if not blank
                        if (asset.note.isNotBlank()) {
                            Text(
                                asset.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (asset.targetPerson.isNotBlank()) {
                            Text(
                                "${strings.targetPerson}: ${asset.targetPerson}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (asset.targetAccount.isNotBlank()) {
                            Text(
                                "${strings.targetAccount}: ${asset.targetAccount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            timeFormat.format(Date(asset.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status label and amount
                    Column(horizontalAlignment = Alignment.End) {
                        // Status label
                        val statusLabel = when (asset.status) {
                            AssetStatus.NONE -> null
                            AssetStatus.OWNED -> strings.owned
                            AssetStatus.NOT_OWNED -> strings.notOwned
                            AssetStatus.IN_PROGRESS -> strings.inProgress
                            AssetStatus.TEMPORARILY_WITH_ME -> strings.temporarilyWithMe
                            AssetStatus.TEMPORARILY_WITH_OTHERS -> strings.temporarilyWithOthers
                        }
                        if (statusLabel != null) {
                            Text(
                                statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = cardColor
                            )
                        }
                        Text(
                            "$currency${String.format(Locale.US, "%.2f", asset.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cardColor
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.rotate(45f)
                )
            },
            title = {
                Text(strings.deleteAsset, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(strings.confirmDelete)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun BatchDeleteAssetDialog(
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
            Text(strings.deleteAsset, fontWeight = FontWeight.Bold)
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
        }
    )
}