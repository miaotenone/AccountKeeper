package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Income", style = MaterialTheme.typography.titleMedium)
                    Text("¥${String.format(Locale.US, "%.2f", totalIncome)}", style = MaterialTheme.typography.headlineMedium, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Expense", style = MaterialTheme.typography.titleMedium)
                    Text("¥${String.format(Locale.US, "%.2f", totalExpense)}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Transactions", style = MaterialTheme.typography.titleMedium)
                    Text("${transactions.size} records", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
