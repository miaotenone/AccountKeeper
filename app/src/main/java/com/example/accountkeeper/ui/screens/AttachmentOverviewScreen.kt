package com.example.accountkeeper.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentEntity
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.AttachmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentOverviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: AttachmentViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val attachments by viewModel.attachments.collectAsState()
    var selected by remember { mutableStateOf<Attachment?>(null) }
    var deleteTarget by remember { mutableStateOf<AttachmentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.attachments) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (attachments.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.attachments, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(strings.noAttachments)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attachments, key = { it.id }) { entity ->
                    val model = entity.toAttachmentModel()
                    Column(Modifier.fillMaxWidth()) {
                        AttachmentRow(
                            attachment = model,
                            onClick = { selected = model },
                            onRemove = { deleteTarget = entity }
                        )
                        Text(
                            "${entity.ownerType} · ${entity.ownerId}",
                            modifier = Modifier.padding(start = 68.dp, top = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selected?.let { attachment ->
        AttachmentPreviewDialog(attachment = attachment, onDismiss = { selected = null })
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.delete) },
            text = { Text(target.fileName) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    deleteTarget = null
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}

private fun AttachmentEntity.toAttachmentModel(): Attachment = Attachment(
    id = id,
    fileName = fileName,
    filePath = filePath,
    fileType = Attachment.getTypeFromMimeType(mimeType),
    fileSize = fileSize,
    mimeType = mimeType,
    createdAt = createdAt
)
