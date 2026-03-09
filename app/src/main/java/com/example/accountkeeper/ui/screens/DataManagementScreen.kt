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
    assetViewModel: AssetViewModel = hiltViewModel()
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

    // Launcher for Export (Create Document) - ZIP format
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val categoryMap = categories.associate { it.id to it.name }
                    val success = settingsViewModel.backupManager.exportZipToUri(
                        uri = uri,
                        transactions = transactions,
                        assets = assets,
                        categoryMap = categoryMap
                    )
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            snackbarHostState.showSnackbar(
                                if (strings.language == "界面语言") "导出成功！包含 ${transactions.size} 笔交易和 ${assets.size} 条资产记录"
                                else "Export successful! ${transactions.size} transactions and ${assets.size} assets"
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                if (strings.language == "界面语言") "导出失败"
                                else "Export failed"
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(
                            if (strings.language == "界面语言") "导出失败: ${e.localizedMessage}"
                            else "Export failed: ${e.localizedMessage}"
                        )
                    }
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
                                        if (strings.language == "界面语言") "成功融合 $successCount 笔数据！" else "Successfully merged $successCount records!"
                                    } else {
                                        if (strings.language == "界面语言") "合并完毕：但未识别出任何需要补充的新数据" else "Merge complete: no new data to add"
                                    })
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar(if (strings.language == "界面语言") "合并解析失败: ${e.localizedMessage}" else "Merge parsing failed: ${e.localizedMessage}")        }
    }

    // ZIP 导入处理 - 内联函数避免 return 标签问题
    suspend fun performZipImport(uri: Uri) {
        try {
            val result = settingsViewModel.backupManager.readZipBackup(uri)
            
            if (!result.success) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        if (strings.language == "界面语言") "导入失败: ${result.errorMessage}"
                        else "Import failed: ${result.errorMessage}"
                    )
                }
                return
            }
            
            var txCount = 0
            var assetCount = 0
            val importNewCategoriesMap = mutableMapOf<Pair<String, TransactionType>, Boolean>()
            val importNewAssetCategoriesMap = mutableMapOf<String, Boolean>()
            
            // 收集需要创建的新分类
            for (tx in result.transactions) {
                val type = if (tx.type.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val catMatch = categories.find { it.name.equals(tx.categoryName, ignoreCase = true) && it.type == type }
                if (catMatch == null && tx.categoryName.isNotBlank() && tx.categoryName != "Other") {
                    importNewCategoriesMap[tx.categoryName to type] = true
                }
            }
            
            for (asset in result.assets) {
                if (asset.categoryName != null && asset.categoryName.isNotBlank()) {
                    val catMatch = categories.find { it.name.equals(asset.categoryName, ignoreCase = true) }
                    if (catMatch == null) {
                        importNewAssetCategoriesMap[asset.categoryName] = true
                    }
                }
            }
            
            // 创建新分类
            for ((name, type) in importNewCategoriesMap.keys) {
                categoryViewModel.addCategory(com.example.accountkeeper.data.model.Category(name = name, type = type, isDefault = false))
            }
            for (name in importNewAssetCategoriesMap.keys) {
                // 资产分类默认为 EXPENSE 类型
                categoryViewModel.addCategory(com.example.accountkeeper.data.model.Category(name = name, type = TransactionType.EXPENSE, isDefault = false))
            }
            
            kotlinx.coroutines.delay(500)
            
            val latestCategories = categoryViewModel.categories.value
            val latestTransactions = viewModel.transactions.value
            val latestAssets = assetViewModel.assets.value
            
            // 处理附件文件映射
            val processedAttachments = mutableMapOf<String, Attachment>()
            for ((attachmentId, tempFile) in result.attachmentFiles) {
                val originalFileName = tempFile.name.substringAfter("_", tempFile.name)
                val newAttachment = settingsViewModel.backupManager.copyAttachmentToInternalStorage(
                    attachmentId = attachmentId,
                    tempFile = tempFile,
                    originalFileName = originalFileName
                )
                if (newAttachment != null) {
                    processedAttachments[attachmentId] = newAttachment
                }
            }
            
            // 导入交易记录
            for (tx in result.transactions) {
                if (latestTransactions.any { it.id == tx.id }) continue
                
                val type = if (tx.type.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                val catMatch = latestCategories.find { it.name.equals(tx.categoryName, ignoreCase = true) && it.type == type }
                val categoryId = catMatch?.id ?: latestCategories.firstOrNull { it.type == type }?.id
                
                if (tx.amount > 0 && categoryId != null) {
                    val transaction = Transaction(
                        id = tx.id,
                        type = type,
                        amount = tx.amount,
                        note = tx.note,
                        date = tx.date,
                        categoryId = categoryId
                    )
                    viewModel.addTransaction(transaction)
                    txCount++
                }
            }
            
            // 导入资产记录
            for (assetData in result.assets) {
                if (latestAssets.any { it.id == assetData.id }) continue
                
                val catMatch = latestCategories.find { it.name.equals(assetData.categoryName, ignoreCase = true) }
                val categoryId = catMatch?.id
                
                // 更新附件路径
                val updatedAttachments = assetData.attachments.map { att ->
                    processedAttachments[att.id] ?: att
                }
                
                val asset = Asset(
                    id = assetData.id,
                    date = assetData.date,
                    amount = assetData.amount,
                    status = try { AssetStatus.valueOf(assetData.status) } catch (e: Exception) { AssetStatus.NONE },
                    categoryId = categoryId,
                    targetPerson = assetData.targetPerson,
                    targetAccount = assetData.targetAccount,
                    note = assetData.note,
                    isCompleted = assetData.isCompleted,
                    attachments = AttachmentConverter.toJson(updatedAttachments),
                    createdAt = assetData.createdAt,
                    updatedAt = assetData.updatedAt
                )
                assetViewModel.addAsset(asset)
                assetCount++
            }
            
            // 清理临时文件
            settingsViewModel.backupManager.cleanupTempFiles()
            
            withContext(Dispatchers.Main) {
                val message = if (strings.language == "界面语言") {
                    "导入成功！交易记录: $txCount 笔，资产记录: $assetCount 条"
                } else {
                    "Import successful! Transactions: $txCount, Assets: $assetCount"
                }
                snackbarHostState.showSnackbar(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar(
                    if (strings.language == "界面语言") "导入失败: ${e.localizedMessage}"
                    else "Import failed: ${e.localizedMessage}"
                )
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                performZipImport(uri)
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

            // ZIP Data Management Section
            PremiumDataCard(
                icon = Icons.Default.Description,
                title = strings.manualDataManagement,
                description = if (strings.language == "界面语言") "ZIP 全量导入导出（含交易、资产和附件）" else "ZIP Full Import/Export (Transactions, Assets & Attachments)",
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
                        onClick = { importZipLauncher.launch("*/*") }
                    )

                    PremiumButton(
                        text = strings.exportAll,
                        icon = Icons.Default.CloudDownload,
                        onClick = {
                            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            val fileName = "AccountKeeper_Export_${dateFormat.format(Date())}.zip"
                            exportZipLauncher.launch(fileName)
                        }
                    )
                }
            }

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
                                // Create manual backup with complete data
                                val categoryMap = categories.associate { it.id to it.name }
                                val result = settingsViewModel.backupManager.writeZipBackup(
                                    transactions = transactions,
                                    assets = assets,
                                    categoryMap = categoryMap,
                                    maxKeep = appSettings.backupRetentionLimit,
                                    isAuto = false,
                                    customName = customBackupName.ifBlank { null }
                                )
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
            snackbarHostState = snackbarHostState
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
            snackbarHostState = snackbarHostState
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
                                                            snackbarHostState.showSnackbar(if (strings.language == "界面语言") "恢复功能暂未实现" else "Restore feature not yet implemented")
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
    onRefresh: () -> Unit,
    context: Context,
    categories: List<com.example.accountkeeper.data.model.Category>,
    categoryViewModel: CategoryViewModel,
    viewModel: TransactionViewModel,
    snackbarHostState: SnackbarHostState
) {
    val bills by remember(refreshTrigger) { mutableStateOf(backupManager.getAllBillFiles()) }
    val scope = rememberCoroutineScope()

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                strings = strings
                            ) 
                        }
                    }
                    
                    // 支付宝账单分组
                    if (alipayBills.isNotEmpty()) {
                        if (wechatBills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                                strings = strings
                            ) 
                        }
                    }
                    
                    // 其他账单分组
                    if (otherBills.isNotEmpty()) {
                        if (wechatBills.isNotEmpty() || alipayBills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                                strings = strings
                            ) 
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
    strings: AppStrings
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
                    bill.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                )
                Text(
                    backupManager.getBillFileSize(bill),
                    style = MaterialTheme.typography.bodySmall,
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
                                            note = tx.note,
                                            date = tx.date,
                                            categoryId = categoryId,
                                            source = tx.source
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
                        backupManager.deleteBillFile(bill)
                        onRefresh()
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isChinese) "账单文件已删除" else "Bill file deleted")
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = if (isChinese) "删除" else "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}