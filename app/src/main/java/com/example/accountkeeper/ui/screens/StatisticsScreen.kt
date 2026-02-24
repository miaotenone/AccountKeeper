package com.example.accountkeeper.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

enum class TimeRange { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
enum class PickerType { ANCHOR, CUSTOM_START, CUSTOM_END }
enum class StatType { EXPENSE, INCOME, BALANCE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current

    var selectedRange by remember { mutableStateOf(TimeRange.MONTHLY) }
    var statType by remember { mutableStateOf(StatType.EXPENSE) }
    var anchorDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var activePicker by remember { mutableStateOf<PickerType?>(null) }

    LaunchedEffect(selectedRange) {
        if (selectedRange != TimeRange.CUSTOM) {
            anchorDate = System.currentTimeMillis()
        }
    }

    val (startTime, endTime, displayPeriodStr) = remember(selectedRange, anchorDate, customStartDate, customEndDate) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = anchorDate
        var start = 0L
        var end = 0L
        var periodStr = ""

        when (selectedRange) {
            TimeRange.DAILY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                end = calendar.timeInMillis
                periodStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(start))
            }
            TimeRange.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                end = calendar.timeInMillis
                val weekNum = calendar.get(Calendar.WEEK_OF_YEAR) - 1
                val year = calendar.get(Calendar.YEAR)
                periodStr = "${year} Week $weekNum"
            }
            TimeRange.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                end = calendar.timeInMillis
                periodStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(start))
            }
            TimeRange.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                end = calendar.timeInMillis
                periodStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(start))
            }
            TimeRange.CUSTOM -> {
                start = customStartDate ?: 0L
                end = customEndDate ?: Long.MAX_VALUE
                if (customStartDate != null && customEndDate != null) {
                    val s = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(start))
                    val e = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(end))
                    periodStr = "$s -> $e"
                } else {
                    periodStr = strings.selectRange
                }
            }
        }
        Triple(start, end, periodStr)
    }

    val displayTransactions = remember(allTransactions, startTime, endTime, statType) {
        allTransactions.filter {
            it.date in startTime until endTime &&
            (statType == StatType.BALANCE ||
             it.type == if (statType == StatType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME)
        }
    }

    val totalIncomeBase = displayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpenseBase = displayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val totalAmountBase = if (statType == StatType.BALANCE) {
        totalIncomeBase - totalExpenseBase
    } else {
        displayTransactions.sumOf { it.amount }
    }

    val totalAmount = CurrencyUtils.convertToDisplay(totalAmountBase, currency)
    val pieTotalBase = if (statType == StatType.BALANCE) totalIncomeBase + totalExpenseBase else totalAmountBase
    val pieTotalDisplay = CurrencyUtils.convertToDisplay(pieTotalBase, currency)

    val categoryTotalsBase = if (statType == StatType.BALANCE) {
        listOf(Pair(-1L, totalIncomeBase), Pair(-2L, totalExpenseBase)).filter { it.second > 0 }
    } else {
        displayTransactions.groupBy { it.categoryId ?: 0L }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categoryTotals = categoryTotalsBase.map { it.first to CurrencyUtils.convertToDisplay(it.second, currency) }

    if (activePicker != null) {
        val initialSelected = when (activePicker) {
            PickerType.ANCHOR -> anchorDate
            PickerType.CUSTOM_START -> customStartDate ?: System.currentTimeMillis()
            PickerType.CUSTOM_END -> customEndDate ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelected)
        DatePickerDialog(
            onDismissRequest = { activePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateVal ->
                        when(activePicker) {
                            PickerType.ANCHOR -> anchorDate = dateVal
                            PickerType.CUSTOM_START -> {
                                val c = Calendar.getInstance()
                                c.timeInMillis = dateVal
                                c.set(Calendar.HOUR_OF_DAY, 0)
                                c.set(Calendar.MINUTE, 0)
                                c.set(Calendar.SECOND, 0)
                                customStartDate = c.timeInMillis
                            }
                            PickerType.CUSTOM_END -> {
                                val c = Calendar.getInstance()
                                c.timeInMillis = dateVal
                                c.set(Calendar.HOUR_OF_DAY, 23)
                                c.set(Calendar.MINUTE, 59)
                                c.set(Calendar.SECOND, 59)
                                customEndDate = c.timeInMillis
                            }
                            else -> {}
                        }
                    }
                    activePicker = null
                }) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { activePicker = null }) { Text(strings.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                strings.statistics,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                strings.categoryStatistics,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Premium Time Range Selector
            PremiumTimeRangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = { selectedRange = it },
                strings = strings
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Period Navigation & Type Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedRange != TimeRange.CUSTOM) {
                        Text(
                            text = displayPeriodStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { activePicker = PickerType.ANCHOR },
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Column {
                            val startStr = customStartDate?.let { SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it)) } ?: strings.startDate
                            val endStr = customEndDate?.let { SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it)) } ?: strings.endDate
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = startStr,
                                    modifier = Modifier.clickable { activePicker = PickerType.CUSTOM_START },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(" -> ", modifier = Modifier.padding(horizontal = 4.dp))
                                Text(
                                    text = endStr,
                                    modifier = Modifier.clickable { activePicker = PickerType.CUSTOM_END },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row {
                    PremiumFilterChip(
                        selected = statType == StatType.EXPENSE,
                        onClick = { statType = StatType.EXPENSE },
                        label = strings.expense
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    PremiumFilterChip(
                        selected = statType == StatType.INCOME,
                        onClick = { statType = StatType.INCOME },
                        label = strings.income
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    PremiumFilterChip(
                        selected = statType == StatType.BALANCE,
                        onClick = { statType = StatType.BALANCE },
                        label = strings.balanceOverall
                    )
                }
            }

            // Premium Total Card
            PremiumTotalCard(
                statType = statType,
                totalAmount = totalAmount,
                currency = currency,
                strings = strings,
                isNegative = totalAmountBase < 0
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Premium Pie Chart
            if (categoryTotals.isNotEmpty() && pieTotalBase > 0) {
                PremiumPieChart(
                    categoryTotals = categoryTotals,
                    pieTotalDisplay = pieTotalDisplay,
                    statType = statType,
                    categories = categories,
                    strings = strings
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Premium Category Breakdown
                Text(
                    strings.categoryRanking,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                categoryTotals.forEachIndexed { index, pair ->
                    val (categoryId, amount) = pair
                    val categoryName = if (statType == StatType.BALANCE) {
                        if (categoryId == -1L) strings.income else strings.expense
                    } else {
                        categories.find { it.id == categoryId }?.name ?: strings.other
                    }
                    val percentage = if (pieTotalDisplay > 0) (amount / pieTotalDisplay) * 100 else 0.0
                    val categoryColor = ChartColors[index % ChartColors.size]

                    PremiumCategoryBreakdown(
                        name = categoryName,
                        amount = amount,
                        percentage = percentage,
                        currency = currency,
                        color = categoryColor,
                        isBalanceMode = statType == StatType.BALANCE,
                        isIncome = categoryId == -1L
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.noTransactions,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PremiumTimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    strings: AppStrings
) {
    ScrollableTabRow(
        selectedTabIndex = selectedRange.ordinal,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        divider = {},
        modifier = Modifier.fillMaxWidth()
    ) {
        TimeRange.entries.forEachIndexed { index, range ->
            Tab(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                text = {
                    Text(
                        when(range) {
                            TimeRange.DAILY -> strings.daily
                            TimeRange.WEEKLY -> strings.weekly
                            TimeRange.MONTHLY -> strings.monthly
                            TimeRange.YEARLY -> strings.yearly
                            TimeRange.CUSTOM -> strings.custom
                        },
                        fontWeight = if (selectedRange == range) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PremiumFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun PremiumTotalCard(
    statType: StatType,
    totalAmount: Double,
    currency: String,
    strings: AppStrings,
    isNegative: Boolean
) {
    val gradient = when {
        statType == StatType.EXPENSE -> {
            if (isSystemInDarkTheme()) DarkGradientExpense else LightGradientExpense
        }
        statType == StatType.INCOME -> {
            if (isSystemInDarkTheme()) DarkGradientIncome else LightGradientIncome
        }
        else -> {
            if (isSystemInDarkTheme()) DarkGradientPrimary else LightGradientPrimary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    when (statType) {
                        StatType.EXPENSE -> strings.totalExpense
                        StatType.INCOME -> strings.totalIncome
                        StatType.BALANCE -> "${strings.balanceOverall}结余"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${if (statType == StatType.BALANCE && totalAmount > 0) "+" else ""}$currency${String.format(Locale.US, "%.2f", totalAmount)}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PremiumPieChart(
    categoryTotals: List<Pair<Long, Double>>,
    pieTotalDisplay: Double,
    statType: StatType,
    categories: List<com.example.accountkeeper.data.model.Category>,
    strings: AppStrings
) {
    // 准备饼图数据
    val pieData = categoryTotals.mapIndexed { index, pair ->
        val (categoryId, amount) = pair
        val categoryName = if (statType == StatType.BALANCE) {
            if (categoryId == -1L) strings.income else strings.expense
        } else {
            categories.find { it.id == categoryId }?.name ?: strings.other
        }
        val percentage = if (pieTotalDisplay > 0) (amount / pieTotalDisplay) * 100 else 0.0
        val categoryColor = if (statType == StatType.BALANCE) {
            if (categoryId == -1L) Color(0xFF5BD9CA) else Color(0xFFFF6B6B)
        } else {
            ChartColors[index % ChartColors.size]
        }
        PieSliceData(categoryName, percentage, categoryColor)
    }.take(8) // 只显示比例最大的前八项

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 饼图标题
            Text(
                "开销比例",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 饼状图区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：饼图
                val isDarkTheme = isSystemInDarkTheme()
                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.size(240.dp)
                    ) {
                        val canvasSize = size
                        val centerX = canvasSize.width / 2
                        val centerY = canvasSize.height / 2
                        val radius = minOf(centerX, centerY) - 10.dp.toPx()

                        var startAngle = -90f

                        pieData.forEach { slice ->
                            if (slice.percentage > 0) {
                                val sweepAngle = (slice.percentage.toFloat() / 100f) * 360f

                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    style = androidx.compose.ui.graphics.drawscope.Fill,
                                    topLeft = Offset(centerX - radius, centerY - radius),
                                    size = Size(radius * 2, radius * 2)
                                )

                                // 绘制白色边框分隔
                                drawArc(
                                    color = Color.White,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    style = Stroke(width = 2.dp.toPx()),
                                    topLeft = Offset(centerX - radius, centerY - radius),
                                    size = Size(radius * 2, radius * 2)
                                )

                                startAngle += sweepAngle
                            }
                        }

                        // 绘制中心圆（创建环形效果）
                        drawCircle(
                            color = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
                            radius = radius * 0.5f,
                            center = Offset(centerX, centerY)
                        )
                    }
                }

                // 右侧：图例（确保所有文字都显示）
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.Start
                    ) {
                        pieData.forEach { slice ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(slice.color, CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slice.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", slice.percentage),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PieSliceData(
    val name: String,
    val percentage: Double,
    val color: Color
)

@Composable
fun PremiumCategoryBreakdown(
    name: String,
    amount: Double,
    percentage: Double,
    currency: String,
    color: Color,
    isBalanceMode: Boolean,
    isIncome: Boolean
) {
    val finalColor = if (isBalanceMode) {
        if (isIncome) Color(0xFF5BD9CA) else Color(0xFFFF6B6B)
    } else {
        color
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(finalColor, CircleShape)
                    )
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$currency${String.format(Locale.US, "%.2f", amount)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        String.format(Locale.US, "%.1f%%", percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (percentage / 100).toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = finalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}