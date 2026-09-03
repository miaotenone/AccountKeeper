package com.example.accountkeeper.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream

fun copyAttachmentToInternalStorage(context: Context, uri: Uri): Attachment? = runCatching {
    val resolver = context.contentResolver
    val name = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
    } ?: "file_${System.currentTimeMillis()}"
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val target = File(context.filesDir, "attachments/${System.currentTimeMillis()}_$name").apply { parentFile?.mkdirs() }
    resolver.openInputStream(uri)?.use { input -> FileOutputStream(target).use { output -> input.copyTo(output) } } ?: return null
    Attachment(System.currentTimeMillis().toString(), name, target.absolutePath, Attachment.getTypeFromMimeType(mime), target.length(), mime)
}.getOrNull()

fun openAttachmentExternally(context: Context, attachment: Attachment): Result<Unit> = runCatching {
    val file = File(attachment.filePath)
    check(file.isFile) { "附件文件不存在" }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    check(context.packageManager.resolveActivity(intent, 0) != null) { "设备没有可打开此文件的应用" }
    context.startActivity(Intent.createChooser(intent, attachment.fileName))
}

fun saveAttachmentToDevice(context: Context, attachment: Attachment): Result<Unit> = runCatching {
    val file = File(attachment.filePath)
    check(file.isFile) { "附件文件不存在" }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    // Try to find a save-capable app
    val saveIntent = Intent(Intent.ACTION_SEND).apply {
        type = attachment.mimeType.ifBlank { "*/*" }
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    check(context.packageManager.resolveActivity(saveIntent, 0) != null) { "设备没有可用于保存此文件的应用" }
    context.startActivity(Intent.createChooser(saveIntent, "保存到"))
}

@Composable
fun AttachmentSection(title: String, attachments: List<Attachment>, onAddAttachment: () -> Unit, onRemoveAttachment: (Attachment) -> Unit, onPreviewAttachment: (Attachment) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAddAttachment) { Icon(Icons.Default.AttachFile, title) }
            }
            if (attachments.isEmpty()) Text("No attachments", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else attachments.forEach { file -> AttachmentRow(file, { onPreviewAttachment(file) }, { onRemoveAttachment(file) }) }
        }
    }
}

@Composable
fun AttachmentRow(attachment: Attachment, onClick: () -> Unit, onRemove: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val icon = when (attachment.fileType) {
        AttachmentType.IMAGE -> Icons.Default.Image
        AttachmentType.EXCEL, AttachmentType.CSV -> Icons.Default.TableChart
        AttachmentType.VIDEO -> Icons.Default.OpenInNew
        else -> Icons.Default.Description
    }
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))) {
        Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(attachment, icon, Modifier.size(56.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(attachment.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(formatFileSize(attachment.fileSize), style = MaterialTheme.typography.labelSmall)
            }
            onRemove?.let { remove -> IconButton(onClick = remove) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
private fun Thumbnail(attachment: Attachment, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    val bitmap = remember(attachment.filePath) {
        when (attachment.fileType) {
            AttachmentType.IMAGE -> BitmapFactory.decodeFile(attachment.filePath)
            AttachmentType.PDF -> renderPdfFirstPage(File(attachment.filePath))
            AttachmentType.VIDEO -> extractVideoThumbnail(attachment.filePath)
            else -> null
        }
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), attachment.fileName, Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
        else Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AttachmentPreviewDialog(attachment: Attachment, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var loading by remember(attachment.filePath) { mutableStateOf(true) }
    var error by remember(attachment.filePath) { mutableStateOf<String?>(null) }
    var bitmap by remember(attachment.filePath) { mutableStateOf<Bitmap?>(null) }
    var externalError by remember(attachment.filePath) { mutableStateOf<String?>(null) }
    var saveError by remember(attachment.filePath) { mutableStateOf<String?>(null) }

    val isImage = attachment.fileType == AttachmentType.IMAGE

    LaunchedEffect(attachment.filePath) {
        if (isImage) {
            runCatching {
                val file = File(attachment.filePath)
                check(file.isFile) { "附件文件不存在" }
                bitmap = BitmapFactory.decodeFile(file.absolutePath)
            }.onFailure { error = "无法读取附件：${it.message ?: "文件无效"}" }
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(attachment.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                if (isImage) {
                    when {
                        loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        error != null -> Text(error!!, Modifier.verticalScroll(rememberScrollState()))
                        bitmap != null -> Image(bitmap!!.asImageBitmap(), attachment.fileName, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                    }
                } else {
                    // Non-image: show file info
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = when (attachment.fileType) {
                                AttachmentType.PDF -> "PDF 文档"
                                AttachmentType.VIDEO -> "视频文件"
                                AttachmentType.EXCEL -> "Excel 表格"
                                AttachmentType.CSV -> "CSV 文件"
                                AttachmentType.DOCUMENT -> "文档文件"
                                AttachmentType.TEXT -> "文本文件"
                                else -> "文件"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "大小: ${formatFileSize(attachment.fileSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "类型: ${attachment.mimeType.ifBlank { "未知" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                externalError?.let { Text(it, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error) }
                saveError?.let { Text(it, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    externalError = openAttachmentExternally(context, attachment).exceptionOrNull()?.let {
                        "无法打开附件：${it.message ?: "没有可用应用"}"
                    }
                }) {
                    Icon(Icons.Default.OpenInNew, null)
                    Spacer(Modifier.width(6.dp))
                    Text("打开")
                }
                OutlinedButton(onClick = {
                    saveError = saveAttachmentToDevice(context, attachment).exceptionOrNull()?.let {
                        "保存失败：${it.message ?: "未知错误"}"
                    }
                }) {
                    Text("保存")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun SpreadsheetGrid(rows: List<List<String>>) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    Column(Modifier.horizontalScroll(horizontal).verticalScroll(vertical)) {
        rows.forEachIndexed { rowIndex, row ->
            Row {
                row.forEach { value ->
                    Text(
                        value,
                        modifier = Modifier.width(140.dp).padding(8.dp),
                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun renderPdfFirstPage(file: File): Bitmap? = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount == 0) null else renderer.openPage(0).use { page ->
                Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also {
                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }
    }
}.getOrNull()

private fun extractVideoThumbnail(filePath: String): Bitmap? = runCatching {
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(filePath)
        retriever.getFrameAtTime(0)
    }
}.getOrNull()

private fun extractSpreadsheet(file: File, type: AttachmentType): List<List<String>>? = runCatching {
    if (type == AttachmentType.CSV) {
        file.readLines().take(200).map(::parseCsvLine)
    } else {
        WorkbookFactory.create(file).use { book ->
            val formatter = DataFormatter()
            buildList {
                for (sheet in book) {
                    for (row in sheet) {
                        add(row.map { formatter.formatCellValue(it) })
                        if (size >= 200) return@buildList
                    }
                }
            }
        }
    }
}.getOrNull()?.filter { it.isNotEmpty() }

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    line.forEach { char ->
        when {
            char == '"' -> quoted = !quoted
            char == ',' && !quoted -> {
                result += current.toString().replace("\"\"", "\"").trim()
                current.setLength(0)
            }
            else -> current.append(char)
        }
    }
    result += current.toString().replace("\"\"", "\"").trim()
    return result
}

private fun extractDocument(file: File): String? = runCatching {
    if (file.extension.equals("docx", true)) {
        XWPFDocument(file.inputStream()).use { it.paragraphs.joinToString("\n") { p -> p.text } }.take(12000)
    } else {
        file.readText().take(12000)
    }
}.getOrNull()

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
