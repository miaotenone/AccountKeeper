package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current
    
    var isShowingMonthly by remember { mutableStateOf(false) }
    
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteTransactionDialog by remember { mutableStateOf(false) }
    
    val currentMonthStart = remember {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val displayTransactions = remember(transactions, isShowingMonthly) {
        if (isShowingMonthly) {
            transactions.filter { it.date >= currentMonthStart }
        } else {
            transactions
        }
    }

    val totalIncomeBase = displayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpenseBase = displayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = CurrencyUtils.convertToDisplay(totalIncomeBase, currency)
    val totalExpense = CurrencyUtils.convertToDisplay(totalExpenseBase, currency)
    val totalBalance = totalIncome - totalExpense

    // Grouping transactions by Date (yyyy-MM-dd)
    val groupedTransactions = displayTransactions.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
    }.toSortedMap(reverseOrder())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("AccountKeeper", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) 
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = strings.addTransaction)
            }
        }
    ) { paddingValues ->
        if (showDeleteTransactionDialog && transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteTransactionDialog = false
                    transactionToDelete = null
                },
                title = { Text("删除账单") },
                text = { Text("确定要删除这条账单吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        transactionToDelete?.let {
                            viewModel.deleteTransaction(it)
                        }
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    }) { Text(strings.ok) }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    }) { Text(strings.cancel) }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFF0050B3) // Deep blue for gradient
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { isShowingMonthly = !isShowingMonthly }
                            .padding(4.dp)
                    ) {
                        Text(if (isShowingMonthly) "本月资产" else strings.totalAssets, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                        Text(" 🔁", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text("$currency${String.format(Locale.US, "%.2f", totalBalance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.income, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                            Text("$currency${String.format(Locale.US, "%.2f", totalIncome)}", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.expense, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                            Text("$currency${String.format(Locale.US, "%.2f", totalExpense)}", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedTransactions.forEach { (dateString, txList) ->
                    stickyHeader {
                        val dayIncomeBase = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                        val dayExpenseBase = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        val dayIncome = CurrencyUtils.convertToDisplay(dayIncomeBase, currency)
                        val dayExpense = CurrencyUtils.convertToDisplay(dayExpenseBase, currency)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateString, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (dayIncome > 0) Text("${strings.income}: $currency${String.format(Locale.US, "%.2f", dayIncome)}", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                if (dayExpense > 0) Text("${strings.expense}: $currency${String.format(Locale.US, "%.2f", dayExpense)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    items(txList, key = { it.id }) { transaction ->
                        val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: strings.other
                        TransactionItem(
                            transaction = transaction,
                            categoryName = categoryName,
                            currency = currency,
                            onClick = { onNavigateToEditTransaction(transaction.id) },
                            onLongClick = {
                                transactionToDelete = transaction
                                showDeleteTransactionDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, categoryName: String, currency: String, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME
    
    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryName.take(1),
                    color = if (isIncome) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        headlineContent = { Text(categoryName, fontWeight = FontWeight.SemiBold) },
        supportingContent = { 
            Column {
                if (transaction.note.isNotBlank()) {
                    Text(transaction.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(timeFormat.format(Date(transaction.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingContent = {
            val displayAmount = CurrencyUtils.convertToDisplay(transaction.amount, currency)
            Text(
                text = "${if (isIncome) "+" else "-"}$currency${String.format(Locale.US, "%.2f", displayAmount)}",
                color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.pointerInput(transaction.id) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick() }
            )
        }
    )
}
