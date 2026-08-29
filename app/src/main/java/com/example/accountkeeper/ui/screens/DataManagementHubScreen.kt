package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.accountkeeper.ui.theme.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLegacyManagement: () -> Unit,
    onNavigateToFinancialArchive: () -> Unit
) {
    val strings = LocalAppStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.dataManagement) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.dataManagement, style = MaterialTheme.typography.titleMedium)
                    Text(strings.dataManagementDescription)
                    Button(onClick = onNavigateToLegacyManagement) {
                        Icon(Icons.Default.Archive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.dataManagement)
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Financial archive", style = MaterialTheme.typography.titleMedium)
                    Text("Export or import a ZIP containing transactions, assets, attachments, asset types, and budgets.")
                    Button(onClick = onNavigateToFinancialArchive) {
                        Icon(Icons.Default.FolderZip, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open archive")
                    }
                }
            }
        }
    }
}
