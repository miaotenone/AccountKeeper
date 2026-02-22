package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long = -1L,
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf<String?>(null) }

    val categories by categoryViewModel.categories.collectAsState()
    // currentType is computed manually since we rely on boolean 'isExpense'
    val currentType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
    val filteredCategories = categories.filter { it.type == currentType }.distinctBy { it.name.lowercase() }

    val scope = rememberCoroutineScope()
    val isEditMode = transactionId != -1L
    val currency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current

    LaunchedEffect(transactionId) {
        if (isEditMode) {
            val existingTx = viewModel.getTransactionById(transactionId)
            existingTx?.let { tx ->
                amountText = CurrencyUtils.convertToDisplay(tx.amount, currency).toString()
                note = tx.note
                isExpense = tx.type == TransactionType.EXPENSE
                selectedCategoryId = tx.categoryId
                transactionDate = tx.date
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transactionDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        transactionDate = it
                    }
                    showDatePicker = false
                }) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCustomCategoryDialog = false
                customCategoryName = ""
                categoryError = null
            },
            title = { Text(strings.newCategory) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = { 
                            customCategoryName = it
                            categoryError = null
                        },
                        label = { Text(strings.name) },
                        singleLine = true,
                        isError = categoryError != null,
                        supportingText = { categoryError?.let { Text(it) } }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = customCategoryName.trim()
                    if (name.isEmpty()) {
                        categoryError = strings.nameEmptyError
                    } else if (filteredCategories.any { it.name.equals(name, ignoreCase = true) }) {
                        categoryError = strings.nameExistsError
                    } else {
                        categoryViewModel.addCategory(Category(name = name, type = currentType, isDefault = false))
                        showCustomCategoryDialog = false
                        customCategoryName = ""
                    }
                }) { Text(strings.add) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) strings.editTransaction else strings.addTransaction) },
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
            // Type Selector
            Row(modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = isExpense,
                    onClick = { 
                        isExpense = true
                        selectedCategoryId = null 
                    },
                    label = { Text(strings.expense) },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                FilterChip(
                    selected = !isExpense,
                    onClick = { 
                        isExpense = false
                        selectedCategoryId = null 
                    },
                    label = { Text(strings.income) },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("${strings.amount} ($currency)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Date Selection
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            OutlinedTextField(
                value = dateFormatter.format(Date(transactionDate)),
                onValueChange = {},
                label = { Text(strings.date) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(strings.change)
                    }
                }
            )

            // Category Selection (Grid)
            Text(strings.category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories, key = { it.id }) { category ->
                    val isSelected = selectedCategoryId == category.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { selectedCategoryId = category.id }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = category.name)
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { showCustomCategoryDialog = true }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add Custom")
                        }
                    }
                }
            }

            // Note Input
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(strings.note) },
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = {
                    val displayAmount = amountText.toDoubleOrNull() ?: 0.0
                    val amount = CurrencyUtils.convertToBase(displayAmount, currency)
                    if (amount > 0) {
                        val transaction = Transaction(
                            id = if (isEditMode) transactionId else 0,
                            type = currentType,
                            amount = amount,
                            note = note,
                            date = transactionDate,
                            categoryId = selectedCategoryId
                        )
                        if (isEditMode) {
                            viewModel.updateTransaction(transaction)
                        } else {
                            viewModel.addTransaction(transaction)
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(strings.save)
            }
        }
    }
}
