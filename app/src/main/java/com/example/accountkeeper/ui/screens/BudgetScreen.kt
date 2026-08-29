package com.example.accountkeeper.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.data.model.BudgetPeriodType
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.DarkGradientPrimary
import com.example.accountkeeper.ui.theme.LightGradientPrimary
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.BudgetViewModel
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateToCategoryTransactions: (Long, String, Long, Long) -> Unit = { _, _, _, _ -> },
    onNavigateToBudgetApproval: () -> Unit = {},
    viewModel: BudgetViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val uncategorizedExpense by viewModel.uncategorizedExpense.collectAsState()
    val spentCategoryIds by viewModel.expenseCategoryIds.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val strings = LocalAppStrings.current
    val currency = LocalCurrencySymbol.current
    val monthKey by viewModel.monthKey.collectAsState()
    val selectedPeriodType by viewModel.selectedPeriodType.collectAsState()
    val range = remember(monthKey, selectedPeriodType) { BudgetViewModel.periodRange(monthKey, selectedPeriodType) }
    val periodLabel = remember(monthKey, selectedPeriodType) { BudgetViewModel.periodLabel(monthKey, selectedPeriodType) }
    val totalBudget = budgets.firstOrNull { it.categoryId == null }
    val expenseCategories = remember(categories) { categories.filter { it.type == TransactionType.EXPENSE } }
    val budgetedCategoryIds = remember(budgets) { budgets.mapNotNull { it.categoryId }.toSet() }
    var includeUnbudgetedSpend by remember { mutableStateOf(true) }
    val visibleCategoryIds = if (includeUnbudgetedSpend) budgetedCategoryIds + spentCategoryIds else budgetedCategoryIds
    val visibleCategories = expenseCategories.filter { it.id in visibleCategoryIds }
    val categoryBudgetTotal = budgets.filter { it.categoryId != null }.sumOf { it.amount }
    val categoryExpenses by viewModel.categoryExpenses.collectAsState()
    var editing by remember { mutableStateOf<Budget?>(null) }
    var showCreateTarget by remember { mutableStateOf(false) }

    val gradient = if (isSystemInDarkTheme()) {
        Brush.linearGradient(colors = DarkGradientPrimary, start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))
    } else {
        Brush.linearGradient(colors = LightGradientPrimary, start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            strings.budget,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            strings.budgetsDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    TextButton(onClick = onNavigateToBudgetApproval) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(strings.approvalCenter)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTarget = true },
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
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Period type selector
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedPeriodType.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BudgetPeriodType.entries.forEach { period ->
                        Tab(
                            selected = selectedPeriodType == period,
                            onClick = { viewModel.setPeriodType(period) },
                            text = {
                                Text(
                                    when (period) {
                                        BudgetPeriodType.MONTHLY -> strings.monthly
                                        BudgetPeriodType.SEMI_ANNUAL -> strings.semiAnnual
                                        BudgetPeriodType.ANNUAL -> strings.yearly
                                    },
                                    fontWeight = if (selectedPeriodType == period) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Period navigation
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::previousPeriod) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = strings.back)
                    }
                    Text(periodLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = viewModel::nextPeriod) {
                        Icon(Icons.Default.ChevronRight, contentDescription = strings.navigate)
                    }
                }
            }

            // Total budget summary card with gradient
            item {
                BudgetSummaryCard(
                    title = when (selectedPeriodType) {
                        BudgetPeriodType.MONTHLY -> "${strings.monthly} ${strings.expense}"
                        BudgetPeriodType.SEMI_ANNUAL -> "${strings.semiAnnual} ${strings.expense}"
                        BudgetPeriodType.ANNUAL -> "${strings.yearly} ${strings.expense}"
                    },
                    budget = totalBudget?.amount,
                    spent = monthlyExpense,
                    currency = currency,
                    gradient = gradient,
                    onClick = { editing = totalBudget ?: Budget(monthKey = monthKey, amount = 0.0) }
                )
            }

            if (totalBudget != null && categoryBudgetTotal > totalBudget.amount) {
                item {
                    Text(
                        "${strings.category} ${strings.expense} > ${strings.total} ${strings.budget}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Category expense header
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${strings.category} ${strings.expense}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FilterChip(
                        selected = includeUnbudgetedSpend,
                        onClick = { includeUnbudgetedSpend = !includeUnbudgetedSpend },
                        label = { Text(if (includeUnbudgetedSpend) strings.showUnbudgetedSpend else strings.showBudgetedOnly) }
                    )
                }
            }

            if (visibleCategories.isEmpty()) {
                item { Text(strings.budgetNotSet, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            items(visibleCategories, key = { it.id }) { category ->
                val budget = budgets.firstOrNull { it.categoryId == category.id }
                val spent = categoryExpenses[category.id] ?: 0.0
                CategoryBudgetCard(
                    categoryName = category.name,
                    budget = budget?.amount,
                    spent = spent,
                    currency = currency,
                    onClick = { onNavigateToCategoryTransactions(category.id, category.name, range.first, range.second) },
                    onEdit = { editing = budget ?: Budget(monthKey = monthKey, categoryId = category.id, amount = 0.0) }
                )
            }

            if (uncategorizedExpense > 0) {
                item {
                    BudgetSummaryCard(
                        title = "${strings.category}: ${strings.other}",
                        budget = null,
                        spent = uncategorizedExpense,
                        currency = currency,
                        gradient = null,
                        onClick = {}
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    editing?.let { budget ->
        BudgetEditorDialog(
            budget = budget,
            onDismiss = { editing = null },
            onSave = { amount ->
                viewModel.save(budget.copy(amount = amount))
                editing = null
            },
            onDelete = if (budget.id != 0L) ({
                viewModel.delete(budget)
                editing = null
            }) else null
        )
    }
    if (showCreateTarget) {
        BudgetTargetDialog(
            categories = expenseCategories,
            onSelectTotal = {
                editing = totalBudget ?: Budget(monthKey = monthKey, amount = 0.0)
                showCreateTarget = false
            },
            onSelectCategory = { categoryId ->
                editing = budgets.firstOrNull { it.categoryId == categoryId }
                    ?: Budget(monthKey = monthKey, categoryId = categoryId, amount = 0.0)
                showCreateTarget = false
            },
            onDismiss = { showCreateTarget = false }
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    title: String,
    budget: Double?,
    spent: Double,
    currency: String,
    gradient: Brush?,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val limit = budget ?: 0.0
    val over = budget != null && spent > limit
    val ratio = when {
        budget == null -> 0f
        limit > 0.0 -> (spent / limit).toFloat().coerceIn(0f, 1f)
        spent > 0.0 -> 1f
        else -> 0f
    }
    val remaining = limit - spent

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (gradient != null) Modifier.background(gradient) else Modifier)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (gradient != null) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (budget == null) strings.budgetNotSet else "$currency${formatAmount(limit)}",
                        color = if (gradient != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        over -> MaterialTheme.colorScheme.error
                        gradient != null -> Color(0xFF5BD9CA)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = if (gradient != null) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "${strings.budgetSpent} $currency${formatAmount(spent)}",
                    color = if (gradient != null) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (budget != null) {
                    Text(
                        "${strings.budgetRemaining} $currency${formatAmount(remaining)}" +
                            if (over) "  ${strings.budgetExceeded} $currency${formatAmount(-remaining)}" else "",
                        color = when {
                            over -> MaterialTheme.colorScheme.error
                            gradient != null -> Color.White.copy(alpha = 0.75f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    categoryName: String,
    budget: Double?,
    spent: Double,
    currency: String,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val strings = LocalAppStrings.current
    val limit = budget ?: 0.0
    val over = budget != null && spent > limit
    val ratio = when {
        budget == null -> 0f
        limit > 0.0 -> (spent / limit).toFloat().coerceIn(0f, 1f)
        spent > 0.0 -> 1f
        else -> 0f
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onEdit) {
                    Text(if (budget == null) strings.add else strings.change)
                }
            }
            if (budget != null) {
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = when {
                        over -> MaterialTheme.colorScheme.error
                        ratio > 0.8f -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$currency${formatAmount(spent)} / $currency${formatAmount(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val pct = if (limit > 0) (spent / limit * 100).toInt().coerceAtMost(999) else 0
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(strings.budgetNotSet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatAmount(value: Double): String = String.format(Locale.US, "%.2f", value)

@Composable
private fun BudgetTargetDialog(
    categories: List<com.example.accountkeeper.data.model.Category>,
    onSelectTotal: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.budget) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                item {
                    TextButton(onClick = onSelectTotal, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.totalExpense)
                    }
                }
                items(categories, key = { it.id }) { category ->
                    TextButton(onClick = { onSelectCategory(category.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(category.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun BudgetEditorDialog(
    budget: Budget,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: (() -> Unit)?
) {
    val strings = LocalAppStrings.current
    var text by remember(budget.id, budget.amount) { mutableStateOf(budget.amount.toString()) }
    var showZeroWarning by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget.categoryId == null) strings.totalExpense else strings.category) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    validationError = false
                },
                label = { Text(strings.amount) },
                isError = validationError,
                supportingText = { if (validationError) Text(strings.budgetInvalidAmount) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                val value = text.toDoubleOrNull()?.takeIf { it >= 0.0 }
                when {
                    value == null -> validationError = true
                    value == 0.0 -> showZeroWarning = true
                    else -> onSave(value)
                }
            }) { Text(strings.save) }
        },
        dismissButton = {
            Row {
                onDelete?.let { delete ->
                    TextButton(onClick = delete) { Text(strings.delete, color = Color.Red) }
                }
                TextButton(onClick = onDismiss) { Text(strings.cancel) }
            }
        }
    )
    if (showZeroWarning) {
        AlertDialog(
            onDismissRequest = { showZeroWarning = false },
            title = { Text(strings.budget) },
            text = { Text(strings.budgetZeroWarning) },
            confirmButton = {
                Button(onClick = {
                    showZeroWarning = false
                    onSave(0.0)
                }) { Text(strings.ok) }
            },
            dismissButton = { TextButton(onClick = { showZeroWarning = false }) { Text(strings.cancel) } }
        )
    }
}
