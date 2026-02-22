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
import com.example.accountkeeper.ui.viewmodel.CategoryViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
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
                            val safeNote = tx.note.replace(",", "，").replace("\n", " ") // Basic CSV escaping
                            writer.write("${tx.id},${dateString},${typeString},${tx.amount},${categoryName},${safeNote}\n")
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
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
                        // Skip header
                        val dataLines = lines.drop(1)
                        dataLines.forEach { line ->
                            val parts = line.split(",")
                            // Naive parsing: ID,Date,Type,Amount,CategoryName,Note
                            if (parts.size >= 6) {
                                val typeString = parts[2]
                                val amount = parts[3].toDoubleOrNull() ?: 0.0
                                val categoryName = parts[4]
                                val note = parts[5]
                                
                                val type = if (typeString.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
                                val catMatch = categories.find { it.name.equals(categoryName, ignoreCase = true) && it.type == type }
                                
                                val categoryId = catMatch?.id ?: categories.firstOrNull { it.type == type }?.id

                                if (amount > 0 && categoryId != null) {
                                    val transaction = Transaction(
                                        type = type,
                                        amount = amount,
                                        note = note,
                                        date = System.currentTimeMillis(), // In generic import, use current time to avoid complex exact-date parsing for now
                                        categoryId = categoryId
                                    )
                                    viewModel.addTransaction(transaction)
                                    successCount++
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
        topBar = { TopAppBar(title = { Text("Settings & Sync") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: API Sync (Fast Import)
            Text("One-Click Account Sync (API)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connect your e-wallets to automatically fetch transactions directly via official APIs.", style = MaterialTheme.typography.bodySmall)
                    
                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Connecting to WeChat Open Platform...")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF07C160)) // WeChat Green
                    ) {
                        Text("Sync WeChat Pay")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Connecting to Alipay SDK...")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF1677FF)) // Alipay Blue
                    ) {
                        Text("Sync Alipay")
                    }
                }
            }

            // Section 2: CSV Data Management
            Text("Manual Data Management", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Due to API limitations from WeChat and Alipay for personal developers, automatic syncing is disabled. Please upload a standard CSV file exported from this app to restore backups.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { importCsvLauncher.launch("text/csv") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload Standard Backup CSV")
                    }

                    OutlinedButton(
                        onClick = {
                            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                            val fileName = "AccountKeeper_Export_${dateFormat.format(Date())}.csv"
                            exportCsvLauncher.launch(fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export All Transactions to CSV")
                    }
                }
            }
            
            // Section 3: App Settings
            Text("General Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
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
                        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
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
                        Text("Language", style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = {
                            val newLang = if (appSettings.language == "zh") "en" else "zh"
                            settingsViewModel.updateLanguage(newLang)
                        }) {
                            Text(if (appSettings.language == "zh") "Chinese" else "English", fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Currency Symbol", style = MaterialTheme.typography.bodyLarge)
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
