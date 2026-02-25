package com.example.accountkeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
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
fun ImportExportScreen(
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: TransactionViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val transactions by viewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val currentCurrency = LocalCurrencySymbol.current
    val strings = LocalAppStrings.current

    var showClearConfirmDialog by remember { mutableStateOf(false) }
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
                        
                        // We need the full transactions list for export, 
                        // relying on the flow state might be partial if it is paged, but currently it's all.
                        transactions.forEach { tx ->
                            val categoryName = categories.find { it.id == tx.categoryId }?.name ?: "Other"
                            val typeString = if (tx.type == TransactionType.INCOME) "Income" else "Expense"
                            val dateString = dateFormat.format(Date(tx.date))
                            // Safely escape CSV note
                            val safeNote = tx.note.replace("\"", "\"\"")
                            writer.write("${tx.id},${dateString},${typeString},${tx.amount},${categoryName},\"${safeNote}\"\n")
                        }
                        writer.flush()
                    }
                    snackbarHostState.showSnackbar("Export successful!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    val performCsvImport: suspend (() -> java.io.InputStream?) -> Unit = { openStream ->
        try {
            var successCount = 0
            val importNewCategoriesMap = mutableMapOf<Pair<String, TransactionType>, Boolean>()
            
            // Safe CSV line parser without regex catastrophic backtracking
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

            // First pass: Find categories
            openStream()?.bufferedReader()?.useLines { lines ->
                val iterator = lines.iterator()
                if (!iterator.hasNext()) return@useLines
                
                // Parse header
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
                        
                        // Auto-create category if missing
                        if (catMatch == null && categoryName.isNotBlank() && categoryName != "Other" && categoryName != "/") {
                            importNewCategoriesMap[categoryName to type] = true
                        }
                    }
                }
            }
                
            // Create required missing categories
            for ((name, type) in importNewCategoriesMap.keys) {
                categoryViewModel.addCategory(com.example.accountkeeper.data.model.Category(name = name, type = type, isDefault = false))
            }
            
            // Wait a bit for Room to process categories
            kotlinx.coroutines.delay(500)
            
            // Now real insertion
            val latestCategories = categoryViewModel.categories.value
            val latestTransactions = viewModel.transactions.value // Get local data for conflict resolution
            
            // Second pass: Insert transactions
            openStream()?.bufferedReader()?.useLines { lines ->
                val iterator = lines.iterator()
                if (!iterator.hasNext()) return@useLines
                iterator.next() // Skip header
                
                while(iterator.hasNext()) {
                    val line = iterator.next()
                    val parts = parseCsvLine(line)
                    if (parts.size >= 5) {
                        val parsedId = if (idIdx >= 0) parts.getOrNull(idIdx)?.toLongOrNull() ?: IdGenerator.generateId() else IdGenerator.generateId()
                        
                        // Conflict handling: App data takes precedence. Skip if ID exists in local DB.
                        if (latestTransactions.any { it.id == parsedId }) {
                            continue
                        }

                        val typeString = parts.getOrNull(typeIdx) ?: ""
                        val amountString = parts.getOrNull(amountIdx)?.replace(Regex("[^\\d.]"), "") ?: "0"
                        val amount = amountString.toDoubleOrNull() ?: 0.0
                        val categoryName = parts.getOrNull(catIdx)?.trim() ?: ""
                        val note = parts.getOrNull(noteIdx) ?: ""
                        val dateStr = parts.getOrNull(dateIdx) ?: ""
                        
                        // Enhanced Date Parsing
                        var dateMillis = System.currentTimeMillis()
                        val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd")
                        for (format in formats) {
                            try {
                                val parsed = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                                if (parsed != null) {
                                    dateMillis = parsed.time
                                    break
                                }
                            } catch (e: Exception) { /* Ignore */ }
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
            snackbarHostState.showSnackbar(if (successCount > 0) "成功融合 $successCount 笔数据！" else "合并完毕：但未识别出任何需要补充的新数据")
        } catch (e: Exception) {
            e.printStackTrace()
            snackbarHostState.showSnackbar("合并解析失败: ${e.localizedMessage}")
        }
    }

    // Launcher for Import CSV (Get Content)
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                performCsvImport { context.contentResolver.openInputStream(uri) }
            }
        }
    }

    // Launcher for Third-party Bill Import
    val importBillLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val lines = FileConverter.readLines(context, uri)
                    if (lines.isNullOrEmpty()) {
                        snackbarHostState.showSnackbar("无法读取文件内容")
                        return@launch
                    }

                    val billType = BillParser.detectBillType(lines)
                    val parsedTransactions = when (billType) {
                        "wechat" -> BillParser.parseWeChatBill(lines)
                        "alipay" -> BillParser.parseAlipayBill(lines)
                        else -> {
                            snackbarHostState.showSnackbar("无法识别的账单格式")
                            return@launch
                        }
                    }

                    if (parsedTransactions.isEmpty()) {
                        snackbarHostState.showSnackbar("未找到可导入的交易记录")
                        return@launch
                    }

                    // 自动创建缺失的分类
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
                        // 跳过已存在的记录
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

                    // 保存账单文件到本地
                    val savedFile = settingsViewModel.backupManager.saveBillFile(uri, billType)
                    refreshBackupTrigger++

                    val billTypeName = if (billType == "wechat") "微信" else "支付宝"
                    snackbarHostState.showSnackbar("${billTypeName}账单导入成功！共导入 $successCount 笔交易")
                } catch (e: Exception) {
                    e.printStackTrace()
                    snackbarHostState.showSnackbar("导入失败: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(strings.settings) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Category Management
            Text(strings.categoryManagement, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.categoryManagementDescription, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = onNavigateToCategorySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.categoryAndTagManagement)
                    }
                }
            }

            // Section 2: CSV Data Management
            Text(strings.manualDataManagement, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.infoLimitation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { importCsvLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.uploadBackup)
                    }

                    OutlinedButton(
                        onClick = {
                            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            val fileName = "AccountKeeper_Export_${dateFormat.format(Date())}.csv"
                            exportCsvLauncher.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.exportAll)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Third-party Bill Import
                    Text(
                        strings.thirdPartyBillImport,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        strings.thirdPartyBillImportDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { importBillLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.importWeChatAlipayBill)
                    }

                    OutlinedButton(
                        onClick = { showBillFileDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.manageImportedBills)
                    }
                }
            }
            
            // Section 3: Local Internal Backup Management
            Text(strings.localBackupVault, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.enableAutoBackup, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(strings.autoBackupDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = appSettings.isAutoBackupEnabled,
                            onCheckedChange = { settingsViewModel.updateAutoBackup(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column {
                            Text(strings.backupRetentionLimit, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(strings.backupThresholdDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.currentBackupStatus, style = MaterialTheme.typography.bodyLarge)
                            val latestAuto = settingsViewModel.backupManager.getLatestAutoBackupDateStr()
                            val latestManual = settingsViewModel.backupManager.getLatestManualBackupDateStr()
                            Column {
                                Text(
                                    text = strings.latestAutoBackup + (latestAuto ?: strings.noAutoBackup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (latestAuto != null) androidx.compose.ui.graphics.Color(0xFF07C160) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.latestManualBackup + (latestManual ?: strings.noManualBackup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (latestManual != null) androidx.compose.ui.graphics.Color(0xFF5BD9CA) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCustomBackupNameDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.createManualBackup)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                settingsViewModel.backupManager.clearAllAutoBackups()
                                refreshBackupTrigger++
                                scope.launch { snackbarHostState.showSnackbar(strings.backupsCleared) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.clearAutoBackups)
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                settingsViewModel.backupManager.clearAllManualBackups()
                                refreshBackupTrigger++
                                scope.launch { snackbarHostState.showSnackbar(strings.manualBackupsCleared) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(strings.clearManualBackups)
                        }
                    }
                    OutlinedButton(
                        onClick = { showManualBackupsDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(strings.backupVault)
                    }
                }
            }
            
            // Section 4: App Settings
            Text(strings.generalSettings, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.darkMode, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = appSettings.isDarkMode, 
                            onCheckedChange = { settingsViewModel.updateTheme(it) }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.language, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = {
                            val newLang = if (appSettings.language == "zh") "en" else "zh"
                            settingsViewModel.updateLanguage(newLang)
                        }) {
                            Text(if (appSettings.language == "zh") "中文" else "English", fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.currencySymbol, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = {
                            val nextSymbol = when (currentCurrency) {
                                "¥" -> "$"
                                "$" -> "€"
                                else -> "¥"
                            }
                            settingsViewModel.updateCurrency(nextSymbol)
                        }) {
                            Text(currentCurrency, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // Section 5: About
            Text(strings.about, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onNavigateToAbout() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AccountKeeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(strings.version, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                )
            ) {
                Text("⚠️ 清空所有交易记录 ⚠️", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("危险警告") },
                text = { Text("清空操作将永久删除包含在当前数据库内的所有记录信息！如果您未妥善备份这些数据将无法找回。您确定要执行此清空操作吗？") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAllTransactions()
                            showClearConfirmDialog = false
                            scope.launch { snackbarHostState.showSnackbar("已成功清空所有账单记录") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("执意清空")
                    }
                },
                dismissButton = {
                    FilledTonalButton(onClick = { showClearConfirmDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showManualBackupsDialog) {
            AlertDialog(
                onDismissRequest = { showManualBackupsDialog = false },
                title = { Text(strings.backupVault) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Auto Backups Section
                        Text(strings.autoBackup, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        val autoBackups = settingsViewModel.backupManager.getAllAutoBackups()
                        if (autoBackups.isEmpty()) {
                            Text(strings.noAutoBackup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                items(autoBackups.size) { index ->
                                    val file = autoBackups[index]
                                    val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val dateStr = displayFormat.format(Date(file.lastModified()))
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = dateStr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = strings.autoBackup,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        scope.launch(Dispatchers.IO) {
                                                            performCsvImport { java.io.FileInputStream(file) }
                                                        }
                                                        showManualBackupsDialog = false
                                                    },
                                                    modifier = Modifier.height(32.dp)
                                                ) { 
                                                    Text(strings.restore, style = MaterialTheme.typography.labelSmall) 
                                                }
                                                TextButton(
                                                    onClick = {
                                                        settingsViewModel.backupManager.deleteBackupFile(file)
                                                        refreshBackupTrigger++
                                                        scope.launch { snackbarHostState.showSnackbar(strings.deleteBackupSuccess) }
                                                    },
                                                    modifier = Modifier.height(32.dp)
                                                ) { 
                                                    Text(strings.delete, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) 
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Manual Backups Section
                        Text(strings.manualBackup, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        val manualBackups = settingsViewModel.backupManager.getAllManualBackups()
                        if (manualBackups.isEmpty()) {
                            Text(strings.noManualBackups, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                items(manualBackups.size) { index ->
                                    val file = manualBackups[index]
                                    val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val dateStr = displayFormat.format(Date(file.lastModified()))
                                    val fileName = file.nameWithoutExtension
                                    // Extract custom name from filename
                                    val customName = if (fileName.startsWith("AK_Manual_")) {
                                        fileName.removePrefix("AK_Manual_")
                                            .substringBeforeLast("_")
                                            .replace("_", " ")
                                    } else {
                                        fileName
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            // First row: Name and Manual tag
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = customName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = strings.manualBackup,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    TextButton(
                                                        onClick = {
                                                            scope.launch(Dispatchers.IO) {
                                                                performCsvImport { java.io.FileInputStream(file) }
                                                            }
                                                            showManualBackupsDialog = false
                                                        },
                                                        modifier = Modifier.height(32.dp)
                                                    ) { 
                                                        Text(strings.restore, style = MaterialTheme.typography.labelSmall) 
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            settingsViewModel.backupManager.deleteBackupFile(file)
                                                            refreshBackupTrigger++
                                                            scope.launch { snackbarHostState.showSnackbar(strings.deleteBackupSuccess) }
                                                        },
                                                        modifier = Modifier.height(32.dp)
                                                    ) { 
                                                        Text(strings.delete, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) 
                                                    }
                                                }
                                            }
                                            // Second row: Date
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showManualBackupsDialog = false }) { Text(strings.close) }
                }
            )
        }

        if (showCustomBackupNameDialog) {
            AlertDialog(
                onDismissRequest = { showCustomBackupNameDialog = false },
                title = { Text(strings.createManualBackup) },
                text = {
                    Column {
                        Text(strings.enterBackupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customBackupName,
                            onValueChange = { customBackupName = it },
                            placeholder = { Text(strings.backupNamePlaceholder) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (customBackupName.isNotBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    val safeCsvSequence = sequence {
                                        yield("ID,Date,Type,Amount,Category,Note")
                                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        for (tx in transactions) {
                                            val categoryName = categories.find { it.id == tx.categoryId }?.name ?: "Other"
                                            val typeString = if (tx.type == TransactionType.INCOME) "Income" else "Expense"
                                            val safeNote = tx.note.replace("\"", "\"\"")
                                            yield("${tx.id},${dateFormat.format(Date(tx.date))},${typeString},${tx.amount},${categoryName},\"${safeNote}\"")
                                        }
                                    }
                                    settingsViewModel.backupManager.writeNewBackup(safeCsvSequence, appSettings.backupRetentionLimit, isAuto = false, customName = customBackupName)
                                    refreshBackupTrigger++
                                    customBackupName = ""
                                    showCustomBackupNameDialog = false
                                    snackbarHostState.showSnackbar(strings.manualBackupSuccess)
                                }
                            }
                        }
                    ) { Text(strings.save) }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomBackupNameDialog = false }) { Text(strings.cancel) }
                }
            )
        }

        // Third-party Bill Files Dialog
        if (showBillFileDialog) {
            AlertDialog(
                onDismissRequest = { showBillFileDialog = false },
                title = { Text("第三方账单文件管理") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val billFiles = settingsViewModel.backupManager.getAllBillFiles()
                        
                        if (billFiles.isEmpty()) {
                            Text(
                                "暂无已导入的账单文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                            ) {
                                items(billFiles.size) { index ->
                                    val file = billFiles[index]
                                    val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val dateStr = displayFormat.format(Date(file.lastModified()))
                                    val billType = settingsViewModel.backupManager.detectBillType(file)
                                    val fileSize = settingsViewModel.backupManager.getBillFileSize(file)
                                    val typeLabel = when (billType) {
                                        "wechat" -> "微信账单"
                                        "alipay" -> "支付宝账单"
                                        else -> "未知账单"
                                    }
                                    val typeColor = when (billType) {
                                        "wechat" -> androidx.compose.ui.graphics.Color(0xFF07C160)
                                        "alipay" -> androidx.compose.ui.graphics.Color(0xFF1677FF)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                            // First row: File name and type
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = file.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "$typeLabel · $fileSize",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = typeColor
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    TextButton(
                                                        onClick = {
                                                            scope.launch(Dispatchers.IO) {
                                                                // Re-import the bill file
                                                                val lines = FileConverter.readLines(context, Uri.fromFile(file))
                                                                if (lines != null) {
                                                                    val parsed = when (billType) {
                                                                        "wechat" -> BillParser.parseWeChatBill(lines)
                                                                        "alipay" -> BillParser.parseAlipayBill(lines)
                                                                        else -> emptyList()
                                                                    }
                                                                    
                                                                    val latestCategories = categoryViewModel.categories.value
                                                                    val latestTransactions = viewModel.transactions.value
                                                                    var reimportCount = 0
                                                                    
                                                                    parsed.forEach { tx ->
                                                                        if (!latestTransactions.any { it.id == tx.id }) {
                                                                            val catMatch = latestCategories.find { 
                                                                                it.name.equals(tx.category, ignoreCase = true) && it.type == tx.type 
                                                                            }
                                                                            val categoryId = catMatch?.id ?: latestCategories.firstOrNull { it.type == tx.type }?.id
                                                                            
                                                                            if (categoryId != null) {
                                                                                viewModel.addTransaction(
                                                                                    Transaction(
                                                                                        id = tx.id,
                                                                                        type = tx.type,
                                                                                        amount = tx.amount,
                                                                                        note = tx.note,
                                                                                        date = tx.date,
                                                                                        categoryId = categoryId
                                                                                    )
                                                                                )
                                                                                reimportCount++
                                                                            }
                                                                        }
                                                                    }
                                                                    
                                                                    if (reimportCount > 0) {
                                                                        snackbarHostState.showSnackbar("成功恢复 $reimportCount 笔交易")
                                                                    } else {
                                                                        snackbarHostState.showSnackbar("没有需要恢复的新交易")
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.height(32.dp)
                                                    ) { 
                                                        Text(strings.restore, style = MaterialTheme.typography.labelSmall) 
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            settingsViewModel.backupManager.deleteBillFile(file)
                                                            refreshBackupTrigger++
                                                            scope.launch { 
                                                                snackbarHostState.showSnackbar("账单文件已删除") 
                                                            }
                                                        },
                                                        modifier = Modifier.height(32.dp)
                                                    ) { 
                                                        Text(strings.delete, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) 
                                                    }
                                                }
                                            }
                                            // Second row: Date
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBillFileDialog = false }) { Text(strings.close) }
                }
            )
        }
    }
}
