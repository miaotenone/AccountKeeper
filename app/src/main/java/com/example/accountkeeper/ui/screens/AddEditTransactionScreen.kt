package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = isExpense,
                    onClick = { isExpense = true },
                    label = { Text("Expense") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                FilterChip(
                    selected = !isExpense,
                    onClick = { isExpense = false },
                    label = { Text("Income") },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.addTransaction(
                            Transaction(
                                type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                                amount = amount,
                                note = note,
                                date = System.currentTimeMillis(),
                                categoryId = null
                            )
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}
