package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import java.util.Calendar
import java.util.Locale

enum class TimeRange { DAILY, MONTHLY, YEARLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val allTransactions by viewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    
    var selectedRange by remember { mutableStateOf(TimeRange.MONTHLY) }
    var isExpenseView by remember { mutableStateOf(true) }

    // Filter transactions based on selected time range
    val filteredTransactions = remember(allTransactions, selectedRange) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        when (selectedRange) {
            TimeRange.DAILY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                allTransactions.filter { it.date >= startOfDay }
            }
            TimeRange.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                allTransactions.filter { it.date >= startOfMonth }
            }
            TimeRange.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfYear = calendar.timeInMillis
                allTransactions.filter { it.date >= startOfYear }
            }
        }
    }

    val displayTransactions = filteredTransactions.filter { 
        it.type == if (isExpenseView) TransactionType.EXPENSE else TransactionType.INCOME 
    }

    val totalAmount = displayTransactions.sumOf { it.amount }

    // Aggregate by category
    val categoryTotals = displayTransactions.groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Statistics") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time Range Selector
            TabRow(selectedTabIndex = selectedRange.ordinal) {
                Tab(
                    selected = selectedRange == TimeRange.DAILY,
                    onClick = { selectedRange = TimeRange.DAILY },
                    text = { Text("Daily") }
                )
                Tab(
                    selected = selectedRange == TimeRange.MONTHLY,
                    onClick = { selectedRange = TimeRange.MONTHLY },
                    text = { Text("Monthly") }
                )
                Tab(
                    selected = selectedRange == TimeRange.YEARLY,
                    onClick = { selectedRange = TimeRange.YEARLY },
                    text = { Text("Yearly") }
                )
            }

            // Type Toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(
                    selected = isExpenseView,
                    onClick = { isExpenseView = true },
                    label = { Text("Expense") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = !isExpenseView,
                    onClick = { isExpenseView = false },
                    label = { Text("Income") }
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total ${if (isExpenseView) "Expense" else "Income"}", style = MaterialTheme.typography.titleMedium)
                    Text("¥${String.format(Locale.US, "%.2f", totalAmount)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Pie Chart implementation
            if (categoryTotals.isNotEmpty() && totalAmount > 0) {
                val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8))
                
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        var startAngle = -90f
                        categoryTotals.forEachIndexed { index, pair ->
                            val sweepAngle = (pair.second / totalAmount).toFloat() * 360f
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 40f)
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                // Category Breakdowns
                Text("Category Ranking", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categoryTotals.size) { index ->
                        val (categoryId, amount) = categoryTotals[index]
                        val categoryName = categories.find { it.id == categoryId }?.name ?: "Other"
                        val percentage = if (totalAmount > 0) (amount / totalAmount) * 100 else 0.0
                        
                        ListItem(
                            headlineContent = { Text(categoryName) },
                            supportingContent = { 
                                LinearProgressIndicator(
                                    progress = { (amount / totalAmount).toFloat() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    color = colors[index % colors.size]
                                )
                            },
                            trailingContent = { 
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("¥${String.format(Locale.US, "%.2f", amount)}", fontWeight = FontWeight.Bold)
                                    Text(String.format(Locale.US, "%.1f%%", percentage), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No transactions found for this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
