package com.example.accountkeeper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.LocalCurrencySymbol
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
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

    // Launcher for Import CSV (Get Content)
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    var successCount = 0
                    val importNewCategoriesMap = mutableMapOf<Pair<String, TransactionType>, Boolean>()
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                        // Skip header
                        val dataLines = lines.drop(1)
                        // Regex to split by comma but ignore commas inside quotes
                        val csvRegex = ",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)".toRegex()
                        
                        dataLines.forEach { line ->
                            val parts = line.split(csvRegex).map { it.removeSurrounding("\"").replace("\"\"", "\"") }
                            if (parts.size >= 6) {
                                val typeString = parts[2]
                                val amount = parts[3].toDoubleOrNull() ?: 0.0
                                val categoryName = parts[4].trim()
                                val note = parts[5]
                                
                                val type = if (typeString.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                                var catMatch = categories.find { it.name.equals(categoryName, ignoreCase = true) && it.type == type }
                                
                                // Auto-create category if missing
                                if (catMatch == null && categoryName.isNotBlank() && categoryName != "Other") {
                                    importNewCategoriesMap[categoryName to type] = true
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
                        
                        // Re-read file or we could have stored data
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { secondLines ->
                            val secondDataLines = secondLines.drop(1)
                            val latestTransactions = viewModel.transactions.value // Get local data for conflict resolution
                            
                            secondDataLines.forEach { line ->
                                val parts = line.split(csvRegex).map { it.removeSurrounding("\"").replace("\"\"", "\"") }
                                if (parts.size >= 6) {
                                    val idString = parts[0]
                                    val parsedId = idString.toLongOrNull() ?: IdGenerator.generateId()
                                    
                                    // Conflict handling: App data takes precedence. Skip if ID exists in local DB.
                                    if (latestTransactions.any { it.id == parsedId }) {
                                        return@forEach
                                    }

                                    val typeString = parts[2]
                                    val amount = parts[3].toDoubleOrNull() ?: 0.0
                                    val categoryName = parts[4].trim()
                                    val note = parts[5]
                                    val dateStr = parts[1]
                                    val dateMillis = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)?.time ?: System.currentTimeMillis()
                                    
                                    val type = if (typeString.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                                    var catMatch = latestCategories.find { it.name.equals(categoryName, ignoreCase = true) && it.type == type }
                                    
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
                    }
                    snackbarHostState.showSnackbar("Successfully imported $successCount transactions!")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to parse CSV: ${e.localizedMessage}")
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
            // Section 1: Platform Bill Import
            Text("第三方账单导入 (微信/支付宝)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请在微信或支付宝的账单页面中通过“导出账单”生成 CSV 文件，传输到手机后在此处导入。", style = MaterialTheme.typography.bodySmall)
                    
                    Button(
                        onClick = {
                            // Reusing the same generic CSV import launcher for now, 
                            // a robust real app would parse the specific 17-line header of WeChat and 5-line of Alipay.
                            // We will prompt the user to use the generic import for now but parse according to standard AccountKeeper CSV format.
                            scope.launch {
                                snackbarHostState.showSnackbar("提示：目前支持标准格式导入，请确保微信/支付宝数据已转换为标准模板，或直接使用下方的导入备份功能。")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF07C160)) // WeChat Green
                    ) {
                        Text("导入微信账单 (CSV)")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("提示：目前支持标准格式导入，请确保微信/支付宝数据已转换为标准模板，或直接使用下方的导入备份功能。")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1677FF)) // Alipay Blue
                    ) {
                        Text("导入支付宝账单 (CSV)")
                    }
                }
            }

            // Section 2: Category Management
            Text("分类配置", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("统一管理收入与支出的分类信息", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = onNavigateToCategorySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("类别与标签管理")
                    }
                }
            }

            // Section 3: CSV Data Management
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
                        onClick = { importCsvLauncher.launch("text/csv") },
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

        }
    }
}
