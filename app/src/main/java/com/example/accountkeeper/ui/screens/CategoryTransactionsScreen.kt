package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.components.AppSectionCard
import com.example.accountkeeper.ui.components.AppEmptyState
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    categoryId: Long,
    categoryName: String,
    startTime: Long,
    endTime: Long,
    transactionType: TransactionType = TransactionType.EXPENSE,
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current
    val pagedTransactions = remember(categoryId, startTime, endTime, transactionType) {
        viewModel.getByCategoryAndTimePaged(categoryId, transactionType, startTime, endTime)
    }.collectAsLazyPagingItems()
    val transactionCount by remember(categoryId, startTime, endTime, transactionType) {
        viewModel.countByCategoryAndTime(categoryId, transactionType, startTime, endTime)
    }.collectAsState(initial = 0)
    val totalAmount by remember(categoryId, startTime, endTime, transactionType) {
        viewModel.sumByCategoryAndTime(categoryId, transactionType, startTime, endTime)
    }.collectAsState(initial = 0.0)
    val dailySummary by remember(categoryId, startTime, endTime) {
        viewModel.getDailyCategorySummary(categoryId, startTime, endTime)
    }.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val lazyListState = rememberLazyListState()
    val isInitialLoading = pagedTransactions.loadState.refresh is androidx.paging.LoadState.Loading
    val showEmpty = !isInitialLoading && transactionCount == 0
    val totalDisplay = CurrencyUtils.convertToDisplay(totalAmount, currency)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(" ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val gradient = if (isSystemInDarkTheme()) Brush.linearGradient(DarkGradientPrimary) else Brush.linearGradient(LightGradientPrimary)
                AppSectionCard(modifier = Modifier.fillMaxWidth(), emphasis = true, gradient = gradient) {
                    SummaryItem(
                        label = if (transactionType == TransactionType.INCOME) strings.income else strings.expense,
                        value = "$currency${String.format(Locale.getDefault(), "%.2f", totalDisplay)}",
                        color = if (transactionType == TransactionType.INCOME) Color(0xFF5BD9CA) else Color(0xFFFF6B6B)
                    )
                    Text(String.format(Locale.getDefault(), "%d", transactionCount), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (showEmpty) {

                item { AppEmptyState(strings.noTransactions, Modifier.fillMaxWidth()) }
            } else {
                items(count = pagedTransactions.itemCount, key = { index -> pagedTransactions[index]?.id ?: "placeholder-" }) { index ->
                    val transaction = pagedTransactions[index] ?: return@items
                    val currentDate = dateFormat.format(Date(transaction.date))
                    val previousDate = if (index > 0) pagedTransactions[index - 1]?.let { dateFormat.format(Date(it.date)) } else null
                    if (currentDate != previousDate) {
                        val summary = dailySummary.firstOrNull { it.date == currentDate }
                        DateHeaderCompact(currentDate, summary?.income ?: 0.0, summary?.expense ?: 0.0, currency)
                    }
                    CategoryTransactionItem(transaction = transaction, currency = currency)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
@Composable
fun SummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
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
fun DateHeaderCompact(
    date: String,
    dayIncome: Double,
    dayExpense: Double,
    currency: String
) {
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dayIncome > 0) {
                    Text(
                        "+$currency${String.format(Locale.US, "%.2f", dayIncome)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00B5A4)
                    )
                }
                if (dayExpense > 0) {
                    Text(
                        "-$currency${String.format(Locale.US, "%.2f", dayExpense)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE63946)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryTransactionItem(
    transaction: com.example.accountkeeper.data.model.Transaction,
    currency: String
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                    if (isIncome) "+" else "-",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                if (transaction.note.isNotBlank()) {
                    Text(
                        transaction.note,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
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

            // Amount
            val displayAmount = CurrencyUtils.convertToDisplay(transaction.amount, currency)
            Text(
                text = "${if (isIncome) "+" else "-"}$currency${String.format(Locale.US, "%.2f", displayAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF00B5A4) else Color(0xFFE63946)
            )
        }
    }
}

