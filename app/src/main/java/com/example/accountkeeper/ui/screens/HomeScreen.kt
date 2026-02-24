package com.example.accountkeeper.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var isBalanceCardExpanded by remember { mutableStateOf(true) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentMonthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val displayTransactions = remember(transactions, isShowingMonthly) {
        if (isShowingMonthly) {
            transactions.filter { it.date >= currentMonthStart }
        } else {
            transactions
        }
    }

    val totalIncome = CurrencyUtils.convertToDisplay(
        displayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        currency
    )
    val totalExpense = CurrencyUtils.convertToDisplay(
        displayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        currency
    )
    val totalBalance = totalIncome - totalExpense

    val groupedTransactions = displayTransactions.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
    }.toSortedMap(reverseOrder())

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "AccountKeeper",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Manage your finances",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
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
                    contentDescription = strings.addTransaction,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        if (showDeleteDialog && transactionToDelete != null) {
            DeleteTransactionDialog(
                onConfirm = {
                    transactionToDelete?.let { viewModel.deleteTransaction(it) }
                    showDeleteDialog = false
                    transactionToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    transactionToDelete = null
                },
                strings = strings
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium Balance Card with Glass Effect
            PremiumBalanceCard(
                totalBalance = totalBalance,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                currency = currency,
                isShowingMonthly = isShowingMonthly,
                isExpanded = isBalanceCardExpanded,
                onTogglePeriod = { isShowingMonthly = !isShowingMonthly },
                onToggleExpand = { isBalanceCardExpanded = !isBalanceCardExpanded },
                strings = strings
            )

            groupedTransactions.forEach { (dateString, txList) ->
                DateHeader(
                    date = dateString,
                    txList = txList,
                    currency = currency,
                    strings = strings
                )

                txList.forEach { transaction ->
                    val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: strings.other
                    PremiumTransactionItem(
                        transaction = transaction,
                        categoryName = categoryName,
                        currency = currency,
                        onClick = { onNavigateToEditTransaction(transaction.id) },
                        onLongClick = {
                            transactionToDelete = transaction
                            showDeleteDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun PremiumBalanceCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    currency: String,
    isShowingMonthly: Boolean,
    isExpanded: Boolean,
    onTogglePeriod: () -> Unit,
    onToggleExpand: () -> Unit,
    strings: AppStrings
) {
    val density = LocalDensity.current
    val gradient = if (isSystemInDarkTheme()) {
        Brush.linearGradient(
            colors = DarkGradientPrimary,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = LightGradientPrimary,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val cardHeight by animateDpAsState(
        targetValue = if (isExpanded) Dp.Infinity else Dp.Infinity,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardHeight"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = (-70).dp, y = (-70).dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 80.dp, y = 60.dp)
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onTogglePeriod() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (isShowingMonthly) strings.thisMonth else strings.totalAssets,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                        Icon(
                            if (isShowingMonthly) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle period",
                            tint = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle expand",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total Balance - always visible
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        strings.totalBalance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$currency${String.format(Locale.US, "%.2f", totalBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Income and Expense row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BalanceStat(
                                label = strings.income,
                                value = totalIncome,
                                currency = currency,
                                color = Color(0xFF5BD9CA)
                            )
                            VerticalDivider(
                                color = Color.White.copy(alpha = 0.25f),
                                thickness = 1.dp,
                                modifier = Modifier.height(50.dp)
                            )
                            BalanceStat(
                                label = strings.expense,
                                value = totalExpense,
                                currency = currency,
                                color = Color(0xFFFF6B6B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceStat(
    label: String,
    value: Double,
    currency: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "$currency${String.format(Locale.US, "%.2f", value)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DateHeader(
    date: String,
    txList: List<Transaction>,
    currency: String,
    strings: AppStrings
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dayIncome > 0) {
                    Text(
                        "+$currency${String.format(Locale.US, "%.2f", dayIncome)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00B5A4)
                    )
                }
                if (dayExpense > 0) {
                    Text(
                        "-$currency${String.format(Locale.US, "%.2f", dayExpense)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE63946)
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumTransactionItem(
    transaction: Transaction,
    categoryName: String,
    currency: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Card(
        onClick = { onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient
            Box(
                modifier = Modifier
                    .size(52.dp)
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
                    categoryName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            val displayAmount = CurrencyUtils.convertToDisplay(transaction.amount, currency)
            Text(
                text = "${if (isIncome) "+" else "-"}$currency${String.format(Locale.US, "%.2f", displayAmount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF00B5A4) else Color(0xFFE63946)
            )
        }
    }
}

@Composable
fun DeleteTransactionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    strings: AppStrings
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.rotate(45f)
            )
        },
        title = {
            Text(
                strings.deleteTransaction,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(strings.deleteConfirm)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}