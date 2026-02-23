package com.example.accountkeeper.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.TransactionType
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
    var isExpenseView by remember { mutableStateOf(true) }

    // Offset for navigating previous/next periods (0 means current) is removed
    // We strictly use selected date to anchor our period
    var anchorDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Custom date range state
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    
    // Which picker is currently active
    var activePicker by remember { mutableStateOf<PickerType?>(null) }

    // Reset anchor date when changing range to bring us to the present
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
                val weekNum = calendar.get(Calendar.WEEK_OF_YEAR) - 1 // week before addition
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
                    val s = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(start))
                    val e = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(end))
                    periodStr = "$s -> $e"
                } else {
                    periodStr = strings.selectRange
                }
            }
        }
        Triple(start, end, periodStr)
    }

    val displayTransactions = remember(allTransactions, startTime, endTime, isExpenseView) {
        allTransactions.filter { 
            it.date in startTime until endTime &&
            it.type == if (isExpenseView) TransactionType.EXPENSE else TransactionType.INCOME 
        }
    }

    // Currency Conversion
    val totalAmountBase = displayTransactions.sumOf { it.amount }
    val totalAmount = CurrencyUtils.convertToDisplay(totalAmountBase, currency)

    // Aggregate by category
    val categoryTotalsBase = displayTransactions.groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
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
        topBar = { TopAppBar(title = { Text(strings.statistics) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Range Selector
            ScrollableTabRow(
                selectedTabIndex = selectedRange.ordinal,
                edgePadding = 8.dp
            ) {
                TimeRange.entries.forEachIndexed { index, range ->
                    Tab(
                        selected = selectedRange == range,
                        onClick = { 
                            selectedRange = range
                            if (range == TimeRange.CUSTOM) {
                                // Default selection for custom range avoids immediately popping dialog if we don't want to
                                // Setting to CUSTOM range keeps the distinct UI
                            }
                        },
                        text = { 
                            val label = when(range) {
                                TimeRange.DAILY -> strings.daily
                                TimeRange.WEEKLY -> strings.weekly
                                TimeRange.MONTHLY -> strings.monthly
                                TimeRange.YEARLY -> strings.yearly
                                TimeRange.CUSTOM -> strings.custom
                            }
                            Text(label) 
                        }
                    )
                }
            }

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
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { activePicker = PickerType.ANCHOR }
                        )
                    } else {
                        Column {
                            val startStr = customStartDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "Start Date"
                            val endStr = customEndDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: "End Date"
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = startStr,
                                    modifier = Modifier.clickable { activePicker = PickerType.CUSTOM_START },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(" -> ", modifier = Modifier.padding(horizontal = 4.dp))
                                Text(
                                    text = endStr,
                                    modifier = Modifier.clickable { activePicker = PickerType.CUSTOM_END },
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row {
                    FilterChip(
                        selected = isExpenseView,
                        onClick = { isExpenseView = true },
                        label = { Text(strings.expense) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    FilterChip(
                        selected = !isExpenseView,
                        onClick = { isExpenseView = false },
                        label = { Text(strings.income) }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (isExpenseView) strings.totalExpense else strings.totalIncome, style = MaterialTheme.typography.titleMedium)
                    Text("$currency${String.format(Locale.US, "%.2f", totalAmount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Pie Chart implementation with Labels
            if (categoryTotals.isNotEmpty() && totalAmount > 0) {
                val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFFF8A65))
                
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        var startAngle = -90f
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2
                        
                        // Use native canvas to draw text
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 32f
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }

                        categoryTotals.forEachIndexed { index, pair ->
                            val sweepAngle = (pair.second / totalAmount).toFloat() * 360f
                            val color = colors[index % colors.size]
                            
                            // Draw the arc section
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 50f)
                            )
                            
                            // Calculate position for text (middle of the arc, pushed outwards)
                            val midAngle = startAngle + sweepAngle / 2
                            val midAngleRad = Math.toRadians(midAngle.toDouble())
                            // Extend text positioning outside the stroke
                            val textRadius = radius + 60f 
                            
                            val textX = (center.x + textRadius * cos(midAngleRad)).toFloat()
                            val textY = (center.y + textRadius * sin(midAngleRad)).toFloat()

                            // Only draw label if it occupies a decent slice (>5%) to prevent clutter
                            if (sweepAngle > 18f) {
                                val catName = categories.find { it.id == pair.first }?.name ?: strings.other
                                drawContext.canvas.nativeCanvas.drawText(
                                    catName,
                                    textX,
                                    textY,
                                    textPaint
                                )
                            }
                            
                            startAngle += sweepAngle
                        }
                    }
                }

                // Category Breakdowns
                Text(strings.categoryRanking, style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.fillMaxWidth()) {
                    categoryTotals.forEachIndexed { index, pair ->
                        val (categoryId, amount) = pair
                        val categoryName = categories.find { it.id == categoryId }?.name ?: strings.other
                        val percentage = if (totalAmount > 0) (amount / totalAmount) * 100 else 0.0
                        
                        ListItem(
                            headlineContent = { Text(categoryName) },
                            supportingContent = { 
                                LinearProgressIndicator(
                                    progress = { (amount / totalAmount).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    color = colors[index % colors.size]
                                )
                            },
                            trailingContent = { 
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$currency${String.format(Locale.US, "%.2f", amount)}", fontWeight = FontWeight.Bold)
                                    Text(String.format(Locale.US, "%.1f%%", percentage), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), contentAlignment = Alignment.Center) {
                    Text(strings.noTransactions, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
