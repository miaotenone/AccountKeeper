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
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.categories.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current

    // 分页数据
    val pagedTransactions = viewModel.getByCategoryAndTimePaged(categoryId, startTime, endTime)
        .collectAsLazyPagingItems()
    
    // 将分页数据转换为列表（使用 derivedStateOf 确保响应式更新）
    val categoryTransactions by remember {
        derivedStateOf {
            (0 until pagedTransactions.itemCount).mapNotNull { pagedTransactions[it] }
        }
    }

    // Group transactions by date
    val groupedTransactions = categoryTransactions.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
    }.toSortedMap(reverseOrder())

    // Calculate totals
    val totalIncome = CurrencyUtils.convertToDisplay(
        categoryTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        currency
    )
    val totalExpense = CurrencyUtils.convertToDisplay(
        categoryTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        currency
    )
    val transactionCount = categoryTransactions.size

    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            categoryName,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                SummaryItem(
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
                                SummaryItem(
                                    label = strings.expense,
                                    value = "-$currency${String.format(Locale.US, "%.2f", totalExpense)}",
                                    color = Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }
                }
            }

            // Transaction List
            if (groupedTransactions.isEmpty()) {
                item {
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
            } else {
                groupedTransactions.forEach { (dateString, txList) ->
                    // Date Header
                    item(key = "header_$dateString") {
                        DateHeaderCompact(
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
                        CategoryTransactionItem(
                            transaction = transaction,
                            currency = currency
                        )
                    }
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
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
    txList: List<com.example.accountkeeper.data.model.Transaction>,
    currency: String
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
        shape = RoundedCornerShape(16.dp),
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
