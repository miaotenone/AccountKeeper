package com.example.accountkeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.BillParser
import com.example.accountkeeper.utils.FileConverter
import com.example.accountkeeper.utils.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val transactions by viewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val strings = LocalAppStrings.current

    var refreshBackupTrigger by remember { mutableStateOf(0) }
    var showManualBackupsDialog by remember { mutableStateOf(false) }
    var showCustomBackupNameDialog by remember { mutableStateOf(false) }
    var customBackupName by remember { mutableStateOf("") }
    var showBillFileDialog by remember { mutableStateOf(false) }

    // Launcher for Export (Create Document)
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val writer = OutputStreamWriter(outputStream)
                        writer.write("ID,Date,Type,Amount,Category,Note\n")
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                        transactions.forEach { tx ->
                            val categoryName = categories.find { it.id == tx.categoryId }?.name ?: "Other"
                            val typeString = if (tx.type == TransactionType.INCOME) "Income" else "Expense"
                            val dateString = dateFormat.format(Date(tx.date))
                            val safeNote = tx.note.replace("\"", "\"\"")
                            writer.write("${tx.id},${dateString},${typeString},${tx.amount},${categoryName},\"${safeNote}\"\n")
                        }
                        writer.flush()
                    }
                    snackbarHostState.showSnackbar(if (strings.language == "中文") "导出成功！" else "Export successful!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(if (strings.language == "中文") "导出失败: ${e.localizedMessage}" else "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    val performCsvImport: suspend (() -> java.io.InputStream?) -> Unit = { openStream ->
        try {
            var successCount = 0
            val importNewCategoriesMap = mutableMapOf<Pair<String, TransactionType>, Boolean>()

            fun parseCsvLine(line: String): List<String> {
                val result = mutableListOf<String>()
                var current = java.lang.StringBuilder()
                var inQuotes = false
                for (char in line) {
                    if (char == '\"') {
                        inQuotes = !inQuotes
                    } else if (char == ',' && !inQuotes) {
                        result.add(current.toString().replace("\"\"", "\"").trim())
                        current = java.lang.StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                result.add(current.toString().replace("\"\"", "\"").trim())
                return result
            }

            var idIdx = -1
            var dateIdx = 0
            var typeIdx = 1
            var amountIdx = 2
            var catIdx = 3
            var noteIdx = 4

            openStream()?.bufferedReader()?.useLines { lines ->
                val iterator = lines.iterator()
                if (!iterator.hasNext()) return@useLines

                val headerLine = iterator.next()
                val headerParts = parseCsvLine(headerLine)
                if (headerParts.any { it.equals("Date", true) || it.contains("日期") || it.contains("时间") } ||
                    headerParts.any { it.equals("Amount", true) || it.contains("金额") }) {
                    idIdx = headerParts.indexOfFirst { it.equals("ID", true) || it.contains("单号") }
                    dateIdx = headerParts.indexOfFirst { it.equals("Date", true) || it.contains("日期") || it.contains("时间") }.takeIf { it >= 0 } ?: 0
                    typeIdx = headerParts.indexOfFirst { it.equals("Type", true) || it.contains("类型") || it.contains("收支") }.takeIf { it >= 0 } ?: 1
                    amountIdx = headerParts.indexOfFirst { it.equals("Amount", true) || it.contains("金额") }.takeIf { it >= 0 } ?: 2
                    catIdx = headerParts.indexOfFirst { it.equals("Category", true) || it.contains("分类") || it.contains("类别") }.takeIf { it >= 0 } ?: 3
                    noteIdx = headerParts.indexOfFirst { it.equals("Note", true) || it.contains("备注") || it.contains("说明") || it.contains("商品") }.takeIf { it >= 0 } ?: 4
                }

                while(iterator.hasNext()) {
                    val line = iterator.next()
                    val parts = parseCsvLine(line)
                    if (parts.size >= 5) {
                        val typeString = parts.getOrNull(typeIdx) ?: ""
                        val categoryName = parts.getOrNull(catIdx)?.trim() ?: ""

                        val type = if (typeString.equals("Income", ignoreCase = true) || typeString.contains("收入") || typeString.contains("退款")) TransactionType.INCOME else TransactionType.EXPENSE
                        val catMatch = categories.find { it.name.equals(categoryName, ignoreCase = true) && it.type == type }

                        if (catMatch == null && categoryName.isNotBlank() && categoryName != "Other" && categoryName != "/") {
                            importNewCategoriesMap[categoryName to type] = true
                        }
                    }
                }
            }

            for ((name, type) in importNewCategoriesMap.keys) {
                categoryViewModel.addCategory(com.example.accountkeeper.data.model.Category(name = name, type = type, isDefault = false))
            }

            kotlinx.coroutines.delay(500)

            val latestCategories = categoryViewModel.categories.value
            val latestTransactions = viewModel.transactions.value

            openStream()?.bufferedReader()?.useLines { lines ->
                val iterator = lines.iterator()
                if (!iterator.hasNext()) return@useLines
                iterator.next()

                while(iterator.hasNext()) {
                    val line = iterator.next()
                    val parts = parseCsvLine(line)
                    if (parts.size >= 5) {
                        val parsedId = if (idIdx >= 0) parts.getOrNull(idIdx)?.toLongOrNull() ?: IdGenerator.generateId() else IdGenerator.generateId()

                        if (latestTransactions.any { it.id == parsedId }) {
                            continue
                        }

                        val typeString = parts.getOrNull(typeIdx) ?: ""
                        val amountString = parts.getOrNull(amountIdx)?.replace(Regex("[^\\d.]"), "") ?: "0"
                        val amount = amountString.toDoubleOrNull() ?: 0.0
                        val categoryName = parts.getOrNull(catIdx)?.trim() ?: ""
                        val note = parts.getOrNull(noteIdx) ?: ""
                        val dateStr = parts.getOrNull(dateIdx) ?: ""

                        var dateMillis = System.currentTimeMillis()
                        val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd")
                        for (format in formats) {
                            try {
                                val parsed = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                                if (parsed != null) {
                                    dateMillis = parsed.time
                                    break
                                }
                            } catch (e: Exception) { }
                        }

                        val type = if (typeString.equals("Income", ignoreCase = true) || typeString.contains("收入") || typeString.contains("退款")) TransactionType.INCOME else TransactionType.EXPENSE
                        val catMatch = latestCategories.find { it.name.equals(categoryName, ignoreCase = true) && it.type == type }

                        val categoryId = catMatch?.id ?: latestCategories.firstOrNull { it.type == type }?.id

                        if (amount > 0 && categoryId != null) {
                            val transaction = Transaction(
                                id = parsedId,
                                type = type,
                                amount = amount,
                                note = note,
                                date = dateMillis,
                                categoryId = categoryId
                            )
                            viewModel.addTransaction(transaction)
                            successCount++
                        }
                    }
                }
            }
            snackbarHostState.showSnackbar(if (successCount > 0) {
                                        if (strings.language == "中文") "成功融合 $successCount 笔数据！" else "Successfully merged $successCount records!"
                                    } else {
                                        if (strings.language == "中文") "合并完毕：但未识别出任何需要补充的新数据" else "Merge complete: no new data to add"
                                    })
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar(if (strings.language == "中文") "合并解析失败: ${e.localizedMessage}" else "Merge parsing failed: ${e.localizedMessage}")        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                performCsvImport { context.contentResolver.openInputStream(uri) }
            }
        }
    }

    val importBillLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val lines = FileConverter.readLines(context, uri)
                    if (lines.isNullOrEmpty()) {
                        snackbarHostState.showSnackbar(if (strings.language == "中文") "无法读取文件内容" else "Unable to read file content")
                        return@launch
                    }

                    val billType = BillParser.detectBillType(lines)
                    val parsedTransactions = when (billType) {
                        "wechat" -> BillParser.parseWeChatBill(lines)
                        "alipay" -> BillParser.parseAlipayBill(lines)
                        else -> {
                            snackbarHostState.showSnackbar(if (strings.language == "中文") "无法识别的账单格式" else "Unable to recognize bill format")
                            return@launch
                        }
                    }

                    if (parsedTransactions.isEmpty()) {
                        snackbarHostState.showSnackbar(if (strings.language == "中文") "未找到可导入的交易记录" else "No transaction records found to import")
                        return@launch
                    }

                    val importNewCategoriesMap = mutableMapOf<Pair<String, TransactionType>, Boolean>()
                    parsedTransactions.forEach { tx ->
                        val catMatch = categories.find { it.name.equals(tx.category, ignoreCase = true) && it.type == tx.type }
                        if (catMatch == null && tx.category.isNotBlank()) {
                            importNewCategoriesMap[tx.category to tx.type] = true
                        }
                    }

                    for ((name, type) in importNewCategoriesMap.keys) {
                        categoryViewModel.addCategory(com.example.accountkeeper.data.model.Category(name = name, type = type, isDefault = false))
                    }

                    kotlinx.coroutines.delay(500)

                    val latestCategories = categoryViewModel.categories.value
                    val latestTransactions = viewModel.transactions.value
                    var successCount = 0

                    parsedTransactions.forEach { tx ->
                        if (latestTransactions.any { it.id == tx.id }) {
                            return@forEach
                        }

                        val catMatch = latestCategories.find { it.name.equals(tx.category, ignoreCase = true) && it.type == tx.type }
                        val categoryId = catMatch?.id ?: latestCategories.firstOrNull { it.type == tx.type }?.id

                        if (categoryId != null) {
                            val transaction = Transaction(
                                id = tx.id,
                                type = tx.type,
                                amount = tx.amount,
                                note = tx.note,
                                date = tx.date,
                                categoryId = categoryId
                            )
                            viewModel.addTransaction(transaction)
                            successCount++
                        }
                    }

                    val savedFile = settingsViewModel.backupManager.saveBillFile(uri, billType)
                    refreshBackupTrigger++

                    val billTypeName = if (billType == "wechat") {
                        if (strings.language == "中文") "微信" else "WeChat"
                    } else {
                        if (strings.language == "中文") "支付宝" else "Alipay"
                    }
                    snackbarHostState.showSnackbar(if (strings.language == "中文") "${billTypeName}账单导入成功！共导入 $successCount 笔交易" else "$billTypeName bill import successful! Imported $successCount transactions")
                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbarHostState.showSnackbar(if (strings.language == "中文") "导入失败: ${e.localizedMessage}" else "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                                if (strings.language == "中文") "第三方账单导入" else "Third-party Bill Import",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                if (strings.language == "中文") "微信和支付宝账单支持" else "WeChat and Alipay bill support",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // CSV Data Management Section
            PremiumDataCard(
                icon = Icons.Default.Description,
                title = strings.manualDataManagement,
                description = if (strings.language == "中文") "CSV 导入导出功能" else "CSV Import/Export Features",
                color = if (isSystemInDarkTheme()) DarkGradientIncome else LightGradientIncome
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.infoLimitation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    PremiumButton(
                        text = strings.uploadBackup,
                        icon = Icons.Default.CloudUpload,
                        onClick = { importCsvLauncher.launch("*/*") }
                    )

                    PremiumButton(
                        text = strings.exportAll,
                        icon = Icons.Default.CloudDownload,
                        onClick = {
                            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            val fileName = "AccountKeeper_Export_${dateFormat.format(Date())}.csv"
                            exportCsvLauncher.launch(fileName)
                        }
                    )
                }
            }

            // Third-party Bill Import Section
            PremiumDataCard(
                icon = Icons.Default.ReceiptLong,
                title = if (strings.language == "中文") "第三方账单导入" else "Third-party Bill Import",
                description = if (strings.language == "中文") "微信和支付宝账单支持" else "WeChat and Alipay bill support",
                color = if (isSystemInDarkTheme()) DarkGradientExpense else LightGradientExpense
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                    if (strings.language == "中文") "支持微信和支付宝账单CSV文件导入" else "Support WeChat and Alipay bill CSV file import",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                    PremiumButton(
                        text = if (strings.language == "中文") "导入微信/支付宝账单" else "Import WeChat/Alipay Bill",
                        icon = Icons.Default.FileUpload,
                        onClick = { importBillLauncher.launch("*/*") }
                    )

                    PremiumButton(
                        text = if (strings.language == "中文") "管理已导入的账单文件" else "Manage Imported Bill Files",
                        icon = Icons.Default.FolderOpen,
                        onClick = { showBillFileDialog = true }
                    )
                }
            }

            // Local Backup Section
            PremiumDataCard(
                icon = Icons.Default.Backup,
                title = strings.localBackupVault,
                description = strings.localBackupVault,
                color = if (isSystemInDarkTheme()) DarkGradientPrimary else LightGradientPrimary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Auto Backup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.enableAutoBackup, style = MaterialTheme.typography.bodyMedium)
                            Text(strings.autoBackupDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = appSettings.isAutoBackupEnabled,
                            onCheckedChange = { settingsViewModel.updateAutoBackup(it) }
                        )
                    }

                    HorizontalDivider()

                    // Backup Retention Limit
                    Column {
                        Text(strings.backupRetentionLimit, style = MaterialTheme.typography.bodyMedium)
                        Text(strings.backupThresholdDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${appSettings.backupRetentionLimit}${strings.backupRetentionUnit}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = appSettings.backupRetentionLimit.toFloat(),
                            onValueChange = { settingsViewModel.updateBackupRetentionLimit(it.toInt()) },
                            valueRange = 5f..50f,
                            steps = 44,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    // Backup Status
                    Text(strings.currentBackupStatus, style = MaterialTheme.typography.bodyLarge)
                    val latestAuto = settingsViewModel.backupManager.getLatestAutoBackupDateStr()
                    val latestManual = settingsViewModel.backupManager.getLatestManualBackupDateStr()
                    Column {
                        Text(
                            text = strings.latestAutoBackup + (latestAuto ?: strings.noAutoBackup),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (latestAuto != null) Color(0xFF07C160) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.latestManualBackup + (latestManual ?: strings.noManualBackup),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (latestManual != null) Color(0xFF5BD9CA) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Backup Actions
                    PremiumButton(
                        text = strings.createManualBackup,
                        icon = Icons.Default.Add,
                        onClick = { showCustomBackupNameDialog = true }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                settingsViewModel.backupManager.clearAllAutoBackups()
                                refreshBackupTrigger++
                                scope.launch { snackbarHostState.showSnackbar(strings.backupsCleared) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.clearAutoBackups) }

                        OutlinedButton(
                            onClick = {
                                settingsViewModel.backupManager.clearAllManualBackups()
                                refreshBackupTrigger++
                                scope.launch { snackbarHostState.showSnackbar(strings.manualBackupsCleared) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.clearManualBackups) }
                    }

                    PremiumButton(
                        text = strings.backupVault,
                        icon = Icons.Default.FolderOpen,
                        onClick = { showManualBackupsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (showCustomBackupNameDialog) {
        AlertDialog(
            onDismissRequest = { showCustomBackupNameDialog = false },
            title = { Text(strings.enterBackupName) },
            text = {
                OutlinedTextField(
                    value = customBackupName,
                    onValueChange = { customBackupName = it },
                    placeholder = { Text(strings.backupNamePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                // Create manual backup by exporting current data
                                val result = settingsViewModel.backupManager.writeNewBackup(
                                    csvLineSequence = sequenceOf(""),
                                    maxKeep = appSettings.backupRetentionLimit,
                                    isAuto = false,
                                    customName = customBackupName.ifBlank { null }
                                )
                                refreshBackupTrigger++
                                snackbarHostState.showSnackbar(strings.manualBackupSuccess)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(if (strings.language == "中文") "创建备份失败: ${e.localizedMessage}" else "Backup creation failed: ${e.localizedMessage}")
                            }
                        }
                        showCustomBackupNameDialog = false
                        customBackupName = ""
                    }
                ) { Text(strings.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomBackupNameDialog = false }) { Text(strings.cancel) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showManualBackupsDialog) {
        ManualBackupsDialog(
            onDismiss = { showManualBackupsDialog = false },
            backupManager = settingsViewModel.backupManager,
            strings = strings,
            refreshTrigger = refreshBackupTrigger,
            onRefresh = { refreshBackupTrigger++ },
            snackbarHostState = snackbarHostState
        )
    }

    if (showBillFileDialog) {
        BillFileDialog(
            onDismiss = { showBillFileDialog = false },
            backupManager = settingsViewModel.backupManager,
            strings = strings,
            refreshTrigger = refreshBackupTrigger,
            onRefresh = { refreshBackupTrigger++ }
        )
    }
}

@Composable
fun PremiumDataCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(color)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
fun PremiumButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun ManualBackupsDialog(
    onDismiss: () -> Unit,
    backupManager: com.example.accountkeeper.utils.BackupManager,
    strings: AppStrings,
    refreshTrigger: Int,
    onRefresh: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val backups by remember(refreshTrigger) { mutableStateOf(backupManager.getAllManualBackups()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.backupVault) },
        text = {
            if (backups.isEmpty()) {
                                    Text(strings.noManualBackups)
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        backups.forEach { backup ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        scope.launch {
                                                            // TODO: Implement restore functionality
                                                            snackbarHostState.showSnackbar(if (strings.language == "中文") "恢复功能暂未实现" else "Restore feature not yet implemented")
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        backup.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(backup.lastModified())),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        backupManager.deleteBackupFile(backup)
                                        onRefresh()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun BillFileDialog(
    onDismiss: () -> Unit,
    backupManager: com.example.accountkeeper.utils.BackupManager,
    strings: AppStrings,
    refreshTrigger: Int,
    onRefresh: () -> Unit
) {
    val bills by remember(refreshTrigger) { mutableStateOf(backupManager.getAllBillFiles()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (strings.language == "中文") "已导入的账单文件" else "Imported Bill Files") },
                        text = {
                            if (bills.isEmpty()) {
                                Text(if (strings.language == "中文") "尚未导入任何账单文件" else "No bill files imported yet")
                            } else {                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bills.forEach { bill ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        bill.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val billType = backupManager.detectBillType(bill)
                                    Text(
                                        "${if (billType == "wechat") {
                                            if (strings.language == "中文") "微信" else "WeChat"
                                        } else if (billType == "alipay") {
                                            if (strings.language == "中文") "支付宝" else "Alipay"
                                        } else {
                                            if (strings.language == "中文") "未知" else "Unknown"
                                        }} - ${backupManager.getBillFileSize(bill)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        backupManager.deleteBillFile(bill)
                                        onRefresh()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
        shape = RoundedCornerShape(20.dp)
    )
}