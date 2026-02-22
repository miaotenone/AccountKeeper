package com.example.accountkeeper.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

enum class TimeRange { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }

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

    // Offset for navigating previous/next periods (0 means current)
    var timeOffset by remember { mutableIntStateOf(0) }
    
    // Custom date range state
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Reset offset when changing range
    LaunchedEffect(selectedRange) {
        if (selectedRange != TimeRange.CUSTOM) {
            timeOffset = 0
        }
    }

    val (startTime, endTime, displayPeriodStr) = remember(selectedRange, timeOffset, customStartDate, customEndDate) {
        val calendar = Calendar.getInstance()
        var start = 0L
        var end = 0L
        var periodStr = ""

        when (selectedRange) {
            TimeRange.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, timeOffset)
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
                calendar.add(Calendar.WEEK_OF_YEAR, timeOffset)
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
                calendar.add(Calendar.MONTH, timeOffset)
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
                calendar.add(Calendar.YEAR, timeOffset)
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
                    val s = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(start))
                    val e = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(end))
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

    val totalAmount = displayTransactions.sumOf { it.amount }

    // Aggregate by category
    val categoryTotals = displayTransactions.groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    customStartDate = dateRangePickerState.selectedStartDateMillis
                    // Default to end of the selected end day if picked
                    customEndDate = dateRangePickerState.selectedEndDateMillis?.let {
                        val c = Calendar.getInstance()
                        c.timeInMillis = it
                        c.set(Calendar.HOUR_OF_DAY, 23)
                        c.set(Calendar.MINUTE, 59)
                        c.set(Calendar.SECOND, 59)
                        c.timeInMillis
                    }
                    if (customStartDate != null) {
                        selectedRange = TimeRange.CUSTOM
                    }
                    showDateRangePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(strings.statistics) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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
                                showDateRangePicker = true
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
                        IconButton(onClick = { timeOffset-- }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                        }
                    }
                    Text(
                        text = displayPeriodStr,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    if (selectedRange != TimeRange.CUSTOM) {
                        IconButton(onClick = { timeOffset++ }, enabled = timeOffset < 0) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                        }
                    } else {
                        IconButton(onClick = { showDateRangePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Range")
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categoryTotals.size) { index ->
                        val (categoryId, amount) = categoryTotals[index]
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
