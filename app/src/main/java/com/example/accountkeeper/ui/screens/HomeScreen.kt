package com.example.accountkeeper.ui.screens

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
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalBalance = totalIncome - totalExpense

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                    Text("Total Assets", style = MaterialTheme.typography.titleMedium)
                    Text("¥${String.format(Locale.US, "%.2f", totalBalance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Income", style = MaterialTheme.typography.bodyMedium)
                            Text("¥${String.format(Locale.US, "%.2f", totalIncome)}", color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Expense", style = MaterialTheme.typography.bodyMedium)
                            Text("¥${String.format(Locale.US, "%.2f", totalExpense)}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onNavigateToEditTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    
    ListItem(
        headlineContent = { Text(transaction.note.ifEmpty { "No Note" }) },
        supportingContent = { Text(dateFormat.format(Date(transaction.date))) },
        trailingContent = {
            val isIncome = transaction.type == TransactionType.INCOME
            Text(
                text = "${if (isIncome) "+" else "-"}¥${String.format(Locale.US, "%.2f", transaction.amount)}",
                color = if (isIncome) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
