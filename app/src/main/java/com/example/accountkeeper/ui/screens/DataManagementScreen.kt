package com.example.accountkeeper.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.AssetViewModel
import com.example.accountkeeper.ui.viewmodel.FinancialArchiveViewModel
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import com.example.accountkeeper.utils.BillParser
import com.example.accountkeeper.utils.FileConverter
import com.example.accountkeeper.utils.IdGenerator
import com.example.accountkeeper.utils.ZipImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    assetViewModel: AssetViewModel = hiltViewModel(),
    financialArchiveViewModel: FinancialArchiveViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val transactions by viewModel.transactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val assets by assetViewModel.assets.collectAsState()
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val strings = LocalAppStrings.current

    var refreshBackupTrigger by remember { mutableStateOf(0) }
    var showManualBackupsDialog by remember { mutableStateOf(false) }
    var showCustomBackupNameDialog by remember { mutableStateOf(false) }
    var customBackupName by remember { mutableStateOf("") }
    var showBillFileDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearTransactionsDialog by remember { mutableStateOf(false) }
    var showClearAssetsDialog by remember { mutableStateOf(false) }
    var showDisableAutoBackupDialog by remember { mutableStateOf(false) }

    // Launcher for Export (Create Document) - ZIP format
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            financialArchiveViewModel.export(uri) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (success) strings.archiveExportSuccess else strings.archiveExportFailed
                    )
                }
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            financialArchiveViewModel.import(uri) { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        }

    }

    // 微信账单导入
    val importWeChatBillLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val lines = FileConverter.readLines(context, uri)
                    if (lines.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(
                                if (strings.language == "界面语言") "无法读取文件内容" else "Unable to read file content"
                            )
                        }
                        return@launch
                    }

                    val parseResult = BillParser.parseWeChatBill(lines)
                    val parsedTransactions = parseResult.transactions

                    if (parsedTransactions.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(
                                if (strings.language == "界面语言") "未找到可导入的交易记录" else "No transaction records found to import"
                            )
                        }
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
                                categoryId = categoryId,
                                source = tx.source
                            )
                            viewModel.addTransaction(transaction)
                            successCount++
                        }
                    }

                    val billTypeName = if (strings.language == "界面语言") "微信" else "WeChat"
                    val excludedInfo = if (parseResult.excludedCount > 0) {
                        if (strings.language == "界面语言") "（已排除 ${parseResult.excludedCount} 笔退款交易）"
                        else " (${parseResult.excludedCount} refund transactions excluded)"
                    } else ""
                    val savedFile = settingsViewModel.backupManager.saveBillFile(uri, "wechat")
                    if (savedFile != null) financialArchiveViewModel.recordBillFile(savedFile, ownerType = "WECHAT")
                    refreshBackupTrigger++
                    val duplicateInfo = if (savedFile == null && successCount > 0) {
                        if (strings.language == "界面语言") "（文件已存在，未重复保存）" else " (File already exists, not saved again)"
                    } else ""
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (strings.language == "界面语言") "${billTypeName}账单导入成功！共导入 $successCount 笔交易$excludedInfo$duplicateInfo"
                            else "$billTypeName bill import successful! Imported $successCount transactions$excludedInfo$duplicateInfo"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (strings.language == "界面语言") "导入失败: ${e.localizedMessage}" else "Import failed: ${e.localizedMessage}"
                        )
                    }
                }
            }
        }
    }

    // 支付宝账单导入
    val importAlipayBillLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val lines = FileConverter.readLines(context, uri)
                    if (lines.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(
                                if (strings.language == "界面语言") "无法读取文件内容" else "Unable to read file content"
                            )
                        }
                        return@launch
                    }

                    val parseResult = BillParser.parseAlipayBill(lines)
                    val parsedTransactions = parseResult.transactions

                    if (parsedTransactions.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            snackbarHostState.showSnackbar(
                                if (strings.language == "界面语言") "未找到可导入的交易记录" else "No transaction records found to import"
                            )
                        }
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
                                categoryId = categoryId,
                                source = tx.source
                            )
                            viewModel.addTransaction(transaction)
                            successCount++
                        }
                    }

                    val billTypeName = if (strings.language == "界面语言") "支付宝" else "Alipay"
                    val excludedInfo = if (parseResult.excludedCount > 0) {
                        if (strings.language == "界面语言") "（已排除 ${parseResult.excludedCount} 笔退款交易）"
                        else " (${parseResult.excludedCount} refund transactions excluded)"
                    } else ""
                    val savedFile = settingsViewModel.backupManager.saveBillFile(uri, "alipay")
                    if (savedFile != null) financialArchiveViewModel.recordBillFile(savedFile, ownerType = "ALIPAY")
                    refreshBackupTrigger++
                    val duplicateInfo = if (savedFile == null && successCount > 0) {
                        if (strings.language == "界面语言") "（文件已存在，未重复保存）" else " (File already exists, not saved again)"
                    } else ""
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (strings.language == "界面语言") "${billTypeName}账单导入成功！共导入 $successCount 笔交易$excludedInfo$duplicateInfo"
                            else "$billTypeName bill import successful! Imported $successCount transactions$excludedInfo$duplicateInfo"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (strings.language == "界面语言") "导入失败: ${e.localizedMessage}" else "Import failed: ${e.localizedMessage}"
                        )
                    }
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
                                                strings.dataManagement,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                strings.dataManagementDescription,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Third-party Bill Import Section
            PremiumDataCard(
                icon = Icons.Default.ReceiptLong,
                title = strings.thirdPartyBillImport,
                description = strings.thirdPartyBillImportDescription,
                color = if (isSystemInDarkTheme()) DarkGradientExpense else LightGradientExpense
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        strings.thirdPartyBillImportDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 微信账单导入按钮
                    PremiumButton(
                        text = strings.importWeChatBill,
                        icon = Icons.Default.Chat,
                        onClick = { importWeChatBillLauncher.launch("*/*") }
                    )

                    // 支付宝账单导入按钮
                    PremiumButton(
                        text = strings.importAlipayBill,
                        icon = Icons.Default.Payment,
                        onClick = { importAlipayBillLauncher.launch("*/*") }
                    )

                    PremiumButton(
                        text = strings.manageImportedBills,
                        icon = Icons.Default.FolderOpen,
                        onClick = { showBillFileDialog = true }
                    )
                }
            }

            // Complete Data Archive Section
            PremiumDataCard(
                icon = Icons.Default.FolderZip,
                title = strings.financialArchive,
                description = strings.financialArchiveDescription,
                color = if (isSystemInDarkTheme()) DarkGradientPrimary else LightGradientPrimary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        strings.financialArchiveDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PremiumButton(
                        text = strings.exportAll,
                        icon = Icons.Default.Upload,
                        onClick = { exportZipLauncher.launch("accountkeeper-full-archive.zip") }
                    )
                    PremiumButton(
                        text = strings.uploadBackup,
                        icon = Icons.Default.Download,
                        onClick = { importZipLauncher.launch("application/zip") }
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
                            onCheckedChange = { enabled ->
                                if (!enabled && settingsViewModel.backupManager.hasBackupChain()) {
                                    // 关闭自动备份且有备份链，显示确认对话框
                                    showDisableAutoBackupDialog = true
                                } else {
                                    settingsViewModel.updateAutoBackup(enabled)
                                }
                            }
                        )
                    }

                    HorizontalDivider()

                    // Scheduled Backup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (strings.language == "界面语言") "定时备份" else "Scheduled Backup",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                if (strings.language == "界面语言") "按照设定的时间间隔自动备份" else "Auto backup at scheduled intervals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = appSettings.isScheduledBackupEnabled,
                            onCheckedChange = { settingsViewModel.updateScheduledBackup(it) }
                        )
                    }

                    // Scheduled Backup Interval (only show when enabled)
                    if (appSettings.isScheduledBackupEnabled) {
                        var expandedInterval by remember { mutableStateOf(false) }
                        val intervalOptions = (1..30).toList()
                        val intervalLabels = if (strings.language == "界面语言") {
                            (1..30).associateWith { days -> "每${days}天" }
                        } else {
                            (1..30).associateWith { days -> "Every $days days" }
                        }

                        Column {
                            Text(
                                if (strings.language == "界面语言") "备份间隔" else "Backup Interval",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = expandedInterval,
                                onExpandedChange = { expandedInterval = it }
                            ) {
                                OutlinedTextField(
                                    value = intervalLabels[appSettings.scheduledBackupInterval] ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterval) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedInterval,
                                    onDismissRequest = { expandedInterval = false }
                                ) {
                                    intervalOptions.forEach { days ->
                                        DropdownMenuItem(
                                            text = { Text(intervalLabels[days] ?: "") },
                                            onClick = {
                                                settingsViewModel.updateScheduledBackupInterval(days)
                                                expandedInterval = false
                                            }
                                        )
                                    }
                                }
                            }
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

                    HorizontalDivider()

                    // Backup Status
                    Text(strings.currentBackupStatus, style = MaterialTheme.typography.bodyLarge)

                    // 增量备份链状态
                    val (baseBackupTime, deltaSteps) = settingsViewModel.backupManager.getBackupChainInfo()
                    val isChinese = strings.language == "界面语言"

                    Column {
                        if (baseBackupTime != null) {
                            Text(
                                text = (if (isChinese) "基准备份: " else "Base Backup: ") + baseBackupTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF07C160)
                            )
                            Text(
                                text = (if (isChinese) "备份步骤: " else "Backup Steps: ") +
                                       (if (isChinese) "${deltaSteps.size} 步" else "${deltaSteps.size} steps"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (deltaSteps.isNotEmpty()) Color(0xFF5BD9CA) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val latestManual = settingsViewModel.backupManager.getLatestZipManualBackupDateStr()
                            Text(
                                text = (if (isChinese) "增量备份: " else "Delta Backup: ") +
                                       (if (isChinese) "未开启" else "Not enabled"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = strings.latestManualBackup + (latestManual ?: strings.noManualBackup),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (latestManual != null) Color(0xFF5BD9CA) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                settingsViewModel.backupManager.clearAllDeltaBackups()
                                refreshBackupTrigger++
                                scope.launch { snackbarHostState.showSnackbar(strings.backupsCleared) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.clearAutoBackups) }

                        OutlinedButton(
                            onClick = {
                                settingsViewModel.backupManager.clearAllZipManualBackups()
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

            // Clear All Data Section
            PremiumDataCard(
                icon = Icons.Default.DeleteForever,
                title = strings.clearAllData,
                description = strings.clearAllDataDescription,
                color = if (isSystemInDarkTheme()) DarkGradientExpense else LightGradientExpense
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.clearAllDataWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    // 删除交易记录按钮
                    OutlinedButton(
                        onClick = { showClearTransactionsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearTransactions)
                    }

                    // 删除资产记录按钮
                    OutlinedButton(
                        onClick = { showClearAssetsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearAssets)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 清除所有数据按钮
                    Button(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearAllData)
                    }
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
                                val result = financialArchiveViewModel.createManualBackup(customBackupName.ifBlank { null })
                                refreshBackupTrigger++
                                withContext(Dispatchers.Main) {
                                    if (result != null) {
                                        snackbarHostState.showSnackbar(strings.manualBackupSuccess)
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            if (strings.language == "界面语言") "创建备份失败"
                                            else "Backup creation failed"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(
                                        if (strings.language == "界面语言") "创建备份失败: ${e.localizedMessage}"
                                        else "Backup creation failed: ${e.localizedMessage}"
                                    )
                                }
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
            snackbarHostState = snackbarHostState,
            categories = categories,
            categoryViewModel = categoryViewModel,
            transactionViewModel = viewModel,
            assetViewModel = assetViewModel,
        )
    }

    if (showBillFileDialog) {
        BillFileDialog(
            onDismiss = { showBillFileDialog = false },
            backupManager = settingsViewModel.backupManager,
            strings = strings,
            refreshTrigger = refreshBackupTrigger,
            onRefresh = { refreshBackupTrigger++ },
            context = context,
            categories = categories,
            categoryViewModel = categoryViewModel,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onDelete = { file ->
                scope.launch(Dispatchers.IO) {
                    val deleted = financialArchiveViewModel.deleteBillFile(file)
                    withContext(Dispatchers.Main) {
                        if (deleted) refreshBackupTrigger++
                        snackbarHostState.showSnackbar(
                            if (deleted) "账单文件已删除" else "账单文件删除失败"
                        )
                    }
                }
            }
        )
    }

    // Clear All Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = {
                Text(
                    strings.clearAllDataConfirmTitle,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(strings.clearAllDataWillDelete)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${strings.clearAllDataTransactions} (${transactions.size})")
                    Text("• ${strings.clearAllDataAssets} (${assets.size})")
                    Text("• ${strings.clearAllDataAttachments}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        strings.clearAllDataCannotUndo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.deleteAllTransactions()
                            assetViewModel.deleteAllAssets()
                            showClearDataDialog = false
                            snackbarHostState.showSnackbar(strings.clearAllDataSuccess)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.clearAllDataConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Clear Transactions Confirmation Dialog
    if (showClearTransactionsDialog) {
        AlertDialog(
            onDismissRequest = { showClearTransactionsDialog = false },
            title = {
                Text(
                    strings.clearTransactionsConfirmTitle,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text("${strings.clearAllDataTransactions}: ${transactions.size}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        strings.clearTransactionsConfirm,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.deleteAllTransactions()
                            showClearTransactionsDialog = false
                            snackbarHostState.showSnackbar(strings.clearTransactionsSuccess)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.clearAllDataConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTransactionsDialog = false }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Clear Assets Confirmation Dialog
    if (showClearAssetsDialog) {
        AlertDialog(
            onDismissRequest = { showClearAssetsDialog = false },
            title = {
                Text(
                    strings.clearAssetsConfirmTitle,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text("${strings.clearAllDataAssets}: ${assets.size}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        strings.clearAssetsConfirm,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            assetViewModel.deleteAllAssets()
                            showClearAssetsDialog = false
                            snackbarHostState.showSnackbar(strings.clearAssetsSuccess)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.clearAllDataConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAssetsDialog = false }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Disable Auto Backup Confirmation Dialog
    if (showDisableAutoBackupDialog) {
        val isChinese = strings.language == "界面语言"
        AlertDialog(
            onDismissRequest = { showDisableAutoBackupDialog = false },
            title = {
                Text(
                    if (isChinese) "关闭自动备份" else "Disable Auto Backup"
                )
            },
            text = {
                Column {
                    Text(
                        if (isChinese) "检测到存在增量备份数据。关闭自动备份后，您希望如何处理这些备份？"
                        else "Existing incremental backup data detected. How would you like to handle these backups?"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isChinese) "• 保留备份：备份数据将被保留，可随时重新开启自动备份"
                        else "• Keep: Backup data will be preserved, can re-enable anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isChinese) "• 删除备份：清除所有增量备份数据"
                        else "• Delete: Clear all incremental backup data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            settingsViewModel.updateAutoBackup(false)
                            showDisableAutoBackupDialog = false
                        }
                    ) {
                        Text(if (isChinese) "保留备份" else "Keep Backup")
                    }
                    Button(
                        onClick = {
                            settingsViewModel.backupManager.clearAllDeltaBackups()
                            settingsViewModel.updateAutoBackup(false)
                            showDisableAutoBackupDialog = false
                            refreshBackupTrigger++
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isChinese) "已关闭自动备份并清除备份数据" else "Auto backup disabled and backup data cleared"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (isChinese) "删除备份" else "Delete Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAutoBackupDialog = false }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(20.dp)
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
    snackbarHostState: SnackbarHostState,
    categories: List<com.example.accountkeeper.data.model.Category>,
    categoryViewModel: CategoryViewModel,
    transactionViewModel: TransactionViewModel,
    assetViewModel: AssetViewModel,
    financialArchiveViewModel: FinancialArchiveViewModel = hiltViewModel()
) {
    val manualBackups by remember(refreshTrigger) { mutableStateOf(backupManager.getAllZipManualBackups()) }
    val scope = rememberCoroutineScope()
    val isChinese = strings.language == "界面语言"

    // 增量备份链信息
    val (baseBackupTime, deltaSteps) = remember(refreshTrigger) { backupManager.getBackupChainInfo() }
    val hasDeltaBackup = baseBackupTime != null

    var showRestoreConfirmDialog by remember { mutableStateOf<java.io.File?>(null) }
    var showDeltaRestoreDialog by remember { mutableStateOf<Int?>(null) } // 步骤号，-1表示最新

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.backupVault) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 增量备份链部分
                if (hasDeltaBackup) {
                    Text(
                        text = if (isChinese) "增量备份链" else "Incremental Backup Chain",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF07C160)
                    )

                    // 基准备份信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
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
                                    if (isChinese) "基准备份" else "Base Backup",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    baseBackupTime ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { showDeltaRestoreDialog = 0 }) {
                                Text(if (isChinese) "还原到此" else "Restore")
                            }
                        }
                    }

                    // 步骤列表
                    if (deltaSteps.isNotEmpty()) {
                        Text(
                            text = if (isChinese) "备份步骤 (${deltaSteps.size})" else "Backup Steps (${deltaSteps.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        deltaSteps.forEachIndexed { index, step ->
                            DeltaStepItem(
                                stepNumber = step.stepNumber,
                                timestamp = step.timestamp,
                                changeCount = step.changeCount,
                                isChinese = isChinese,
                                onRestore = { showDeltaRestoreDialog = step.stepNumber }
                            )
                        }

                        // 还原到最新
                        OutlinedButton(
                            onClick = { showDeltaRestoreDialog = -1 },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isChinese) "还原到最新状态" else "Restore to Latest")
                        }
                    }

                    if (manualBackups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 手动备份分组
                if (manualBackups.isNotEmpty()) {
                    Text(
                        text = if (isChinese) "手动备份 (${manualBackups.size})" else "Manual Backups (${manualBackups.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5BD9CA)
                    )
                    manualBackups.forEach { backup ->
                        BackupFileItem(
                            backup = backup,
                            isChinese = isChinese,
                            onRestore = { showRestoreConfirmDialog = backup },
                            onDelete = {
                                backupManager.deleteBackupFile(backup)
                                onRefresh()
                            }
                        )
                    }
                }

                // 如果没有备份
                if (!hasDeltaBackup && manualBackups.isEmpty()) {
                    Text(strings.noManualBackups)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
        shape = RoundedCornerShape(20.dp)
    )

    // 增量备份还原确认对话框
    showDeltaRestoreDialog?.let { targetStep ->
        val stepLabel = when (targetStep) {
            0 -> if (isChinese) "基准备份" else "Base Backup"
            -1 -> if (isChinese) "最新状态" else "Latest"
            else -> if (isChinese) "第 $targetStep 步" else "Step $targetStep"
        }

        AlertDialog(
            onDismissRequest = { showDeltaRestoreDialog = null },
            title = {
                Text(
                    if (isChinese) "还原到 $stepLabel" else "Restore to $stepLabel",
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(if (isChinese) "确定要还原到 $stepLabel 吗？" else "Are you sure you want to restore to $stepLabel?")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isChinese) "注意：此操作将清除现有数据并恢复到选定状态。"
                        else "Note: This will clear existing data and restore to the selected state.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                withContext(Dispatchers.Main) {
                                    financialArchiveViewModel.restoreAutoBackupStep(targetStep) { message: String ->
                                        scope.launch {
                                            showDeltaRestoreDialog = null
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(
                                        if (isChinese) "恢复失败: ${e.localizedMessage}"
                                        else "Restore failed: ${e.localizedMessage}"
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isChinese) "还原" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeltaRestoreDialog = null }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // 手动备份恢复确认对话框
    showRestoreConfirmDialog?.let { backupFile ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = null },
            title = {
                Text(
                    if (isChinese) "确认恢复" else "Confirm Restore",
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    Text(if (isChinese) "确定要从以下备份文件恢复数据吗？" else "Are you sure you want to restore from this backup?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        backupFile.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (isChinese) "注意：恢复操作将先创建内部保护快照，然后用备份内容替换当前业务数据。"
                        else "Note: restore first creates an internal protection snapshot, then replaces current business data with the backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                withContext(Dispatchers.Main) {
                                    financialArchiveViewModel.restoreManualBackup(backupFile) { message: String ->
                                        scope.launch {
                                            showRestoreConfirmDialog = null
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(
                                        if (isChinese) "恢复失败: ${e.localizedMessage}"
                                        else "Restore failed: ${e.localizedMessage}"
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text(if (isChinese) "恢复" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = null }) {
                    Text(strings.cancel)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DeltaStepItem(
    stepNumber: Int,
    timestamp: Long,
    changeCount: Int,
    isChinese: Boolean,
    onRestore: () -> Unit
) {
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
                    if (isChinese) "步骤 $stepNumber" else "Step $stepNumber",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isChinese) "变更: $changeCount 项" else "Changes: $changeCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(onClick = onRestore) {
                Text(if (isChinese) "还原" else "Restore")
            }
        }
    }
}

@Composable
private fun BackupFileItem(
    backup: java.io.File,
    isChinese: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
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
                    backup.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(backup.lastModified())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                // 恢复按钮
                IconButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = if (isChinese) "恢复" else "Restore",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                // 删除按钮
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = if (isChinese) "删除" else "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun BillFilePreviewDialog(
    bill: java.io.File,
    billType: String,
    onDismiss: () -> Unit
) {
    var parsedTransactions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bill) {
        try {
            val lines = if (bill.extension.equals("xlsx", ignoreCase = true)) {
                val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(bill.inputStream())
                val lineList = mutableListOf<String>()
                val sheet = workbook.getSheetAt(0)
                for (row in sheet) {
                    val cells = mutableListOf<String>()
                    for (cell in row) {
                        val cellValue = when (cell.cellType) {
                            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue ?: ""
                            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                val num = cell.numericCellValue
                                if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                            }
                            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                                try { cell.stringCellValue ?: cell.numericCellValue.toString() } catch (e: Exception) { "" }
                            }
                            else -> ""
                        }
                        cells.add(cellValue.trim())
                    }
                    if (cells.any { it.isNotBlank() }) {
                        lineList.add(cells.joinToString(","))
                    }
                }
                workbook.close()
                lineList
            } else {
                val bytes = bill.readBytes()
                val content = try { String(bytes, charset("GBK")) } catch (e: Exception) { String(bytes, Charsets.UTF_8) }
                content.lines().filter { it.isNotBlank() }
            }

            val parseResult = when (billType) {
                "wechat" -> BillParser.parseWeChatBill(lines)
                "alipay" -> BillParser.parseAlipayBill(lines)
                else -> null
            }

            parsedTransactions = parseResult?.transactions?.map { tx ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                val typeStr = if (tx.type == com.example.accountkeeper.data.model.TransactionType.INCOME) "+" else "-"
                val note = tx.note.ifBlank { tx.category }
                "$typeStr${String.format(java.util.Locale.US, "%.2f", tx.amount)}" to "$dateStr $note"
            } ?: emptyList()
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                    parsedTransactions.isEmpty() -> Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(parsedTransactions) { (amount, detail) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(detail, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(
                                        amount,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (amount.startsWith("+")) Color(0xFF00B5A4) else Color(0xFFE63946)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun BillFileDialog(
    onDismiss: () -> Unit,
    backupManager: com.example.accountkeeper.utils.BackupManager,
    strings: AppStrings,
    refreshTrigger: Int,
    onRefresh: () -> Unit,
    context: Context,
    categories: List<com.example.accountkeeper.data.model.Category>,
    categoryViewModel: CategoryViewModel,
    viewModel: TransactionViewModel,
    snackbarHostState: SnackbarHostState,
    onDelete: (java.io.File) -> Unit
) {
    val bills by remember(refreshTrigger) { mutableStateOf(backupManager.getAllBillFiles()) }
    val scope = rememberCoroutineScope()
    var previewBill by remember { mutableStateOf<Pair<java.io.File, String>?>(null) }

    val isChinese = strings.language == "界面语言"

    // 分类账单文件
    val wechatBills = bills.filter { backupManager.detectBillType(it) == "wechat" }
    val alipayBills = bills.filter { backupManager.detectBillType(it) == "alipay" }
    val otherBills = bills.filter { backupManager.detectBillType(it) !in listOf("wechat", "alipay") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isChinese) "已导入的账单文件" else "Imported Bill Files") },
        text = {
            if (bills.isEmpty()) {
                Text(if (isChinese) "尚未导入任何账单文件" else "No bill files imported yet")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 微信账单分组
                    if (wechatBills.isNotEmpty()) {
                        Text(
                            text = if (isChinese) "微信 (${wechatBills.size})" else "WeChat (${wechatBills.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        wechatBills.forEach { bill ->
                            BillFileItem(
                                bill = bill,
                                billType = "wechat",
                                backupManager = backupManager,
                                isChinese = isChinese,
                                scope = scope,
                                categories = categories,
                                categoryViewModel = categoryViewModel,
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                onRefresh = onRefresh,
                                strings = strings,
                                onPreview = { previewBill = it to "wechat" },
                                onDelete = onDelete,
                            )
                        }
                    }

                    // 支付宝账单分组
                    if (alipayBills.isNotEmpty()) {
                        if (wechatBills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = if (isChinese) "支付宝 (${alipayBills.size})" else "Alipay (${alipayBills.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        alipayBills.forEach { bill ->
                            BillFileItem(
                                bill = bill,
                                billType = "alipay",
                                backupManager = backupManager,
                                isChinese = isChinese,
                                scope = scope,
                                categories = categories,
                                categoryViewModel = categoryViewModel,
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                onRefresh = onRefresh,
                                strings = strings,
                                onPreview = { previewBill = it to "alipay" },
                                onDelete = onDelete
                            )
                        }
                    }

                    // 其他账单分组
                    if (otherBills.isNotEmpty()) {
                        if (wechatBills.isNotEmpty() || alipayBills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = if (isChinese) "其他 (${otherBills.size})" else "Other (${otherBills.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        otherBills.forEach { bill ->
                            BillFileItem(
                                bill = bill,
                                billType = backupManager.detectBillType(bill),
                                backupManager = backupManager,
                                isChinese = isChinese,
                                scope = scope,
                                categories = categories,
                                categoryViewModel = categoryViewModel,
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState,
                                onRefresh = onRefresh,
                                strings = strings,
                                onPreview = { previewBill = it to backupManager.detectBillType(it) },
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
        shape = RoundedCornerShape(20.dp)
    )

    previewBill?.let { preview ->
        BillFilePreviewDialog(
            bill = preview.first,
            billType = preview.second,
            onDismiss = { previewBill = null }
        )
    }
}

@Composable
fun BillFileItem(
    bill: java.io.File,
    billType: String,
    backupManager: com.example.accountkeeper.utils.BackupManager,
    isChinese: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    categories: List<com.example.accountkeeper.data.model.Category>,
    categoryViewModel: CategoryViewModel,
    viewModel: TransactionViewModel,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    strings: AppStrings,
    onPreview: (java.io.File) -> Unit = {},
    onDelete: (java.io.File) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPreview(bill) },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    bill.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
                Text(
                    backupManager.getBillFileSize(bill),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                // 重新导入按钮
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val lines = if (bill.extension.equals("xlsx", ignoreCase = true)) {
                                    val workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(bill.inputStream())
                                    val lineList = mutableListOf<String>()
                                    val sheet = workbook.getSheetAt(0)
                                    for (row in sheet) {
                                        val cells = mutableListOf<String>()
                                        for (cell in row) {
                                            val cellValue = when (cell.cellType) {
                                                org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue ?: ""
                                                org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                                        cell.dateCellValue?.let {
                                                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(it)
                                                        } ?: ""
                                                    } else {
                                                        val num = cell.numericCellValue
                                                        if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                                                    }
                                                }
                                                org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                                                org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                                                    try { cell.stringCellValue ?: cell.numericCellValue.toString() } catch (e: Exception) { "" }
                                                }
                                                else -> ""
                                            }
                                            cells.add(cellValue.trim())
                                        }
                                        if (cells.any { it.isNotBlank() }) {
                                            lineList.add(cells.joinToString(","))
                                        }
                                    }
                                    workbook.close()
                                    lineList
                                } else {
                                    val bytes = bill.readBytes()
                                    val content = try {
                                        String(bytes, charset("GBK"))
                                    } catch (e: Exception) {
                                        String(bytes, Charsets.UTF_8)
                                    }
                                    content.lines().filter { it.isNotBlank() }
                                }

                                if (lines.isNullOrEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            if (isChinese) "无法读取文件内容" else "Unable to read file content"
                                        )
                                    }
                                    return@launch
                                }

                                val parseResult = when (billType) {
                                    "wechat" -> BillParser.parseWeChatBill(lines)
                                    "alipay" -> BillParser.parseAlipayBill(lines)
                                    else -> {
                                        withContext(Dispatchers.Main) {
                                            snackbarHostState.showSnackbar(
                                                if (isChinese) "无法识别的账单格式" else "Unable to recognize bill format"
                                            )
                                        }
                                        return@launch
                                    }
                                }
                                val parsedTransactions = parseResult.transactions

                                if (parsedTransactions.isEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        snackbarHostState.showSnackbar(
                                            if (isChinese) "未找到可导入的交易记录" else "No transaction records found to import"
                                        )
                                    }
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
                                            categoryId = categoryId,
                                            date = tx.date,
                                            source = tx.source,
                                            note = ""
                                        )
                                        viewModel.addTransaction(transaction)
                                        successCount++
                                    }
                                }

                                val excludedInfo = if (parseResult.excludedCount > 0) {
                                    if (isChinese) "（已排除 ${parseResult.excludedCount} 笔退款交易）"
                                    else " (${parseResult.excludedCount} refund transactions excluded)"
                                } else ""
                                withContext(Dispatchers.Main) {
                                    if (successCount > 0) {
                                        snackbarHostState.showSnackbar(
                                            if (isChinese) "成功导入 $successCount 条新记录$excludedInfo"
                                            else "Successfully imported $successCount new records$excludedInfo"
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            if (isChinese) "没有新记录可导入"
                                            else "No new records to import"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    snackbarHostState.showSnackbar(
                                        if (isChinese) "导入失败: ${e.localizedMessage}"
                                        else "Import failed: ${e.localizedMessage}"
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = if (isChinese) "重新导入" else "Re-import", tint = MaterialTheme.colorScheme.primary)
                }
                // 删除按钮
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            onDelete(bill)
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = if (isChinese) "删除" else "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
