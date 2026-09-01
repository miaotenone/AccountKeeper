package com.example.accountkeeper.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Category
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.CurrencyUtils
import com.example.accountkeeper.utils.IdGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long = -1L,
    onNavigateBack: () -> Unit,
    onTransactionSaved: () -> Unit = {},
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    budgetViewModel: com.example.accountkeeper.ui.viewmodel.BudgetViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
     var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }
     var originalExpenseAmount by remember { mutableStateOf<Double?>(null) }
     var originalExpenseMonth by remember { mutableStateOf<String?>(null) }
     var originalExpenseCategoryId by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf(emptyList<Attachment>()) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val categories by categoryViewModel.categories.collectAsState()
    val currentType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
    val filteredCategories = categories.filter { it.type == currentType }.distinctBy { it.name.lowercase() }

    val scope = rememberCoroutineScope()
    val isEditMode = transactionId != -1L
    val currency = LocalCurrencySymbol.current
    val budgetMonth = remember(transactionDate) { com.example.accountkeeper.ui.viewmodel.BudgetViewModel.monthKey(transactionDate) }
    val budgetRange = remember(budgetMonth) { com.example.accountkeeper.ui.viewmodel.BudgetViewModel.monthRange(budgetMonth) }
    val categoryExpenseFlow = remember(selectedCategoryId, budgetRange) { selectedCategoryId?.let { budgetViewModel.expenseFor(it, budgetRange.first, budgetRange.second) } } ?: kotlinx.coroutines.flow.flowOf(0.0)
    val monthlyBudgetFlow = remember(budgetMonth) { budgetViewModel.budgetFor(budgetMonth, null) }
    val monthlySpentFlow = remember(budgetMonth) { budgetViewModel.expenseForMonth(budgetMonth) }
    val categoryBudgetFlow = remember(budgetMonth, selectedCategoryId) { budgetViewModel.budgetFor(budgetMonth, selectedCategoryId) }
    val monthlyBudget by monthlyBudgetFlow.collectAsState()
    val monthlySpent by monthlySpentFlow.collectAsState()
    val categoryBudget by categoryBudgetFlow.collectAsState()
    val categorySpent by categoryExpenseFlow.collectAsState(initial = 0.0)
    val enteredAmount = amountText.toDoubleOrNull()?.let { CurrencyUtils.convertToBase(it, currency) } ?: 0.0
    val projectedMonthlySpent = com.example.accountkeeper.utils.projectedExpenseForEdit(
        currentMonthExpense = monthlySpent,
        oldAmount = originalExpenseAmount,
        oldMonth = originalExpenseMonth,
        newAmount = if (isExpense) enteredAmount else 0.0,
        newMonth = budgetMonth
    )
    val projectedCategorySpent = com.example.accountkeeper.utils.projectedCategoryExpenseForEdit(
        currentCategoryExpense = categorySpent,
        oldAmount = originalExpenseAmount,
        oldCategoryId = originalExpenseCategoryId,
        oldMonth = originalExpenseMonth,
        newAmount = if (isExpense) enteredAmount else 0.0,
        newCategoryId = if (isExpense) selectedCategoryId else null,
        newMonth = budgetMonth
    )

    LaunchedEffect(transactionId) {
        if (isEditMode) {
            val existingTx = viewModel.getTransactionById(transactionId)
            existingTx?.let { tx ->
                amountText = CurrencyUtils.convertToDisplay(tx.amount, currency).toString()
                note = tx.note
                attachments = AttachmentConverter.fromJson(tx.attachments)
                isExpense = tx.type == TransactionType.EXPENSE
                if (tx.type == TransactionType.EXPENSE) {
                    originalExpenseAmount = tx.amount
                    originalExpenseMonth = com.example.accountkeeper.ui.viewmodel.BudgetViewModel.monthKey(tx.date)
                    originalExpenseCategoryId = tx.categoryId
                }
                selectedCategoryId = tx.categoryId
                transactionDate = tx.date
            }
        }
    }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { copyAttachmentToInternalStorage(context, it)?.let { attachment -> attachments = attachments + attachment } }
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

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            if (isEditMode) strings.editTransaction else strings.addTransaction,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Type Selector with Premium Design
            PremiumTypeSelector(
                isExpense = isExpense,
                onExpenseSelected = {
                    isExpense = true
                    selectedCategoryId = null
                },
                onIncomeSelected = {
                    isExpense = false
                    selectedCategoryId = null
                },
                strings = strings
            )

            // Date Selection with Premium Design
            PremiumDateSelector(
                date = transactionDate,
                onClick = { showDatePicker = true },
                strings = strings,
                modifier = Modifier.fillMaxWidth()
            )

            // Amount Input with Premium Design
            PremiumAmountInput(
                amountText = amountText,
                onAmountChange = { amountText = it },
                currency = currency,
                strings = strings,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection with Premium Grid
            Text(
                strings.category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            PremiumCategoryGrid(
                categories = filteredCategories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Budget hint for expense transactions
            if (isExpense) {
                TransactionBudgetHint(
                    strings = strings,
                    currency = currency,
                    monthlyBudget = monthlyBudget,
                    monthlySpent = projectedMonthlySpent,
                    categoryBudget = categoryBudget,
                    categorySpent = projectedCategorySpent
                )
            }
            PremiumNoteInput(
                note = note,
                onNoteChange = { note = it },
                strings = strings,
                modifier = Modifier.fillMaxWidth()
            )

            AttachmentSection(
                title = strings.attachments,
                attachments = attachments,
                onAddAttachment = { attachmentPicker.launch(arrayOf("*/*")) },
                onRemoveAttachment = { removed -> attachments = attachments.filterNot { it.id == removed.id } },
                onPreviewAttachment = { previewAttachment = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button with Premium Design
            PremiumSaveButton(
                onClick = {
                    val displayAmount = amountText.toDoubleOrNull() ?: 0.0
                    val amount = CurrencyUtils.convertToBase(displayAmount, currency)
                    if (amount > 0) {
                        val transaction = Transaction(
                            id = if (isEditMode) transactionId else IdGenerator.generateId(),
                            type = currentType,
                            amount = amount,
                            note = note,
                            date = transactionDate,
                            categoryId = selectedCategoryId,
                            attachments = AttachmentConverter.toJson(attachments)
                        )
                        scope.launch {
                            if (viewModel.saveTransaction(transaction, isEditMode)) onTransactionSaved()
                        }
                    }
                },
                strings = strings,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    previewAttachment?.let { attachment ->
        AttachmentPreviewDialog(attachment = attachment, onDismiss = { previewAttachment = null })
    }
}

@Composable
fun PremiumTypeSelector(
    isExpense: Boolean,
    onExpenseSelected: () -> Unit,
    onIncomeSelected: () -> Unit,
    strings: AppStrings
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (isExpense) {
                                Modifier.background(
                                    if (isSystemInDarkTheme()) {
                                        Brush.horizontalGradient(DarkGradientExpense)
                                    } else {
                                        Brush.horizontalGradient(LightGradientExpense)
                                    }
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onExpenseSelected() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.expense,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (!isExpense) {
                                Modifier.background(
                                    if (isSystemInDarkTheme()) {
                                        Brush.horizontalGradient(DarkGradientIncome)
                                    } else {
                                        Brush.horizontalGradient(LightGradientIncome)
                                    }
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onIncomeSelected() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        strings.income,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }    }
}

@Composable
fun PremiumAmountInput(
    amountText: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 4.dp
        ),
        border = if (isFocused) {
            BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                strings.amount,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currency,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("0.00") },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
    }
}

@Composable
fun PremiumDateSelector(
    date: Long,
    onClick: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    strings.date,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    dateFormatter.format(Date(date)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    SimpleDateFormat("dd", Locale.getDefault()).format(Date(date)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PremiumCategoryGrid(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.height(300.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = selectedCategoryId == category.id
            val categoryColor = CategoryColors[(category.id % CategoryColors.size).toInt()]

            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.95f else if (isSelected) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "scale"
            )

            Card(
                onClick = { onCategorySelected(category.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(scale),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = categoryColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 8.dp else 2.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumNoteInput(
    note: String,
    onNoteChange: (String) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            placeholder = { Text("Add a note...") },
            label = { Text(strings.note) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun PremiumSaveButton(
    onClick: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .scale(scale),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Text(
            strings.save,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
