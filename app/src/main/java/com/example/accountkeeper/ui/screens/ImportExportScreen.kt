package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Import & Export") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Data Management", style = MaterialTheme.typography.headlineMedium)
            
            Button(
                onClick = { /* TODO: Implement Alipay/Wechat CSV Parsing */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import from Alipay/Wechat CSV")
            }

            Button(
                onClick = { /* TODO: Implement Data Export */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Data to CSV")
            }
        }
    }
}
