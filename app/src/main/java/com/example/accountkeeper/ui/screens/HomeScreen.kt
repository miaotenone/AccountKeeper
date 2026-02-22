package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
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
    
    val totalIncomeBase = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpenseBase = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = CurrencyUtils.convertToDisplay(totalIncomeBase, currency)
    val totalExpense = CurrencyUtils.convertToDisplay(totalExpenseBase, currency)
    val totalBalance = totalIncome - totalExpense

    // Grouping transactions by Date (yyyy-MM-dd)
    val groupedTransactions = transactions.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
    }.toSortedMap(reverseOrder())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = strings.addTransaction)
            }
        }
    ) { paddingValues ->
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(strings.totalAssets, style = MaterialTheme.typography.titleMedium)
                    Text("$currency${String.format(Locale.US, "%.2f", totalBalance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.income, style = MaterialTheme.typography.bodyMedium)
                            Text("$currency${String.format(Locale.US, "%.2f", totalIncome)}", color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(strings.expense, style = MaterialTheme.typography.bodyMedium)
                            Text("$currency${String.format(Locale.US, "%.2f", totalExpense)}", color = MaterialTheme.colorScheme.error)
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
                            onClick = { onNavigateToEditTransaction(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, categoryName: String, currency: String, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    ListItem(
        headlineContent = { Text(categoryName, fontWeight = FontWeight.SemiBold) },
        supportingContent = { 
            Column {
                if (transaction.note.isNotBlank()) {
                    Text(transaction.note, style = MaterialTheme.typography.bodySmall)
                }
                Text(timeFormat.format(Date(transaction.date)), style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = {
            val isIncome = transaction.type == TransactionType.INCOME
            val displayAmount = CurrencyUtils.convertToDisplay(transaction.amount, currency)
            Text(
                text = "${if (isIncome) "+" else "-"}$currency${String.format(Locale.US, "%.2f", displayAmount)}",
                color = if (isIncome) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
