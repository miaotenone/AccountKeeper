package com.example.accountkeeper.utils

import android.content.Context
import android.net.Uri
import com.example.accountkeeper.data.model.Asset
import com.example.accountkeeper.data.model.Attachment
import com.example.accountkeeper.data.model.AttachmentConverter
import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * ZIP 备份数据结构
 */
@Serializable
data class ZipBackupData(
    val transactions: List<TransactionData>,
    val assets: List<AssetData>,
    val version: Int = 1
)

@Serializable
data class TransactionData(
    val id: Long,
    val date: Long,
    val type: String,
    val amount: Double,
    val categoryName: String,
    val note: String
)

@Serializable
data class AssetData(
    val id: Long,
    val date: Long,
    val amount: Double,
    val status: String,
    val categoryName: String?,
    val targetPerson: String,
    val targetAccount: String,
    val note: String,
    val isCompleted: Boolean,
    val attachments: List<Attachment>,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * ZIP 备份导入结果
 */
data class ZipImportResult(
    val transactions: List<TransactionData>,
    val assets: List<AssetData>,
    val attachmentFiles: Map<String, File>, // attachmentId -> temp file
    val success: Boolean,
    val errorMessage: String? = null
)

class BackupManager(private val context: Context) {

    private val backupDir = File(context.filesDir, "backups").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    // 第三方账单目录
    private val billDir = File(context.filesDir, "bills").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    // 附件目录
    private val attachmentsDir = File(context.filesDir, "attachments").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    // Date format for the backup files
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun createBackupFileName(isAuto: Boolean = true, customName: String? = null): String {
        return when {
            customName != null -> {
                // Sanitize custom name and append timestamp
                val sanitizedName = customName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
                "AK_Manual_${sanitizedName}_${dateFormat.format(Date())}.csv"
            }
            isAuto -> "AK_AutoBackup_${dateFormat.format(Date())}.csv"
            else -> "AK_ManualBackup_${dateFormat.format(Date())}.csv"
        }
    }

    /**
     * Get the file object of the latest backup CSV in the directory.
     * Returns null if no backup exists.
     */
    fun getLatestBackupFile(): File? {
        val files = backupDir.listFiles { _, name -> name.endsWith(".csv") }
        if (files.isNullOrEmpty()) return null

        return files.maxByOrNull { it.lastModified() }
    }

    /**
     * Write raw CSV content directly to a new internal backup file.
     * Also manages the retention limit.
     */
    fun writeNewBackup(csvLineSequence: Sequence<String>, maxKeep: Int = 15, isAuto: Boolean = true, customName: String? = null) {
        val newFile = File(backupDir, createBackupFileName(isAuto, customName))
        try {
            newFile.bufferedWriter().use { writer ->
                csvLineSequence.forEach { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
            if (isAuto) {
                cleanUpOldBackups(maxKeep)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retains the most recent `maxKeep` AUTObvackup files and deletes the rest.
     */
    fun cleanUpOldBackups(maxKeep: Int = 15) {
        val files = backupDir.listFiles { _, name -> 
            name.endsWith(".csv") && !name.contains("ManualBackup") // Protect manual backups
        }
        if (files != null && files.size > maxKeep) {
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            val filesToDelete = sortedFiles.drop(maxKeep)
            filesToDelete.forEach { it.delete() }
        }
    }

    /**
     * Returns all auto backups sorted by latest first.
     */
    fun getAllAutoBackups(): List<File> {
        val files = backupDir.listFiles { _, name -> 
            name.endsWith(".csv") && name.contains("AutoBackup") 
        } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * Returns all manual backups sorted by latest first.
     */
    fun getAllManualBackups(): List<File> {
        val files = backupDir.listFiles { _, name -> 
            name.endsWith(".csv") && (name.contains("Manual") || name.contains("AK_Manual"))
        } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * Delete a specific backup file.
     */
    fun deleteBackupFile(file: File): Boolean {
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    /**
     * Clear all current existing auto backups.
     */
    fun clearAllAutoBackups() {
        val files = backupDir.listFiles { _, name -> 
            name.endsWith(".csv") && !name.contains("Manual")
        }
        files?.forEach { it.delete() }
    }

    /**
     * Clear all current existing manual backups.
     */
    fun clearAllManualBackups() {
        val files = backupDir.listFiles { _, name -> 
            name.endsWith(".csv") && (name.contains("Manual") || name.contains("AK_Manual"))
        }
        files?.forEach { it.delete() }
    }

    /**
     * Returns the formatted date string of when the latest auto backup was modified.
     */
    fun getLatestAutoBackupDateStr(): String? {
        val files = backupDir.listFiles { _, name -> name.endsWith(".csv") && !name.contains("ManualBackup") }
        if (files.isNullOrEmpty()) return null
        val latest = files.maxByOrNull { it.lastModified() } ?: return null
        
        val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return displayFormat.format(Date(latest.lastModified()))
    }

    /**
     * Returns the formatted date string of when the latest manual backup was modified.
     */
    fun getLatestManualBackupDateStr(): String? {
        val files = backupDir.listFiles { _, name -> name.endsWith(".csv") && name.contains("ManualBackup") }
        if (files.isNullOrEmpty()) return null
        val latest = files.maxByOrNull { it.lastModified() } ?: return null
        
        val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return displayFormat.format(Date(latest.lastModified()))
    }

    // ========== 第三方账单管理 ==========

    /**
     * 保存第三方账单文件
     * @param sourceUri 源文件URI
     * @param billType 账单类型（wechat/alipay）
     * @return 保存后的文件，如果文件已存在则返回null
     */
    fun saveBillFile(sourceUri: Uri, billType: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val sourceBytes = inputStream.readBytes()
            inputStream.close()
            
            // 计算文件内容的MD5哈希用于查重
            val sourceHash = java.security.MessageDigest.getInstance("MD5")
                .digest(sourceBytes)
                .joinToString("") { "%02x".format(it) }
            
            // 检查是否已存在相同内容的文件
            val existingFiles = getBillFilesByType(billType)
            for (existingFile in existingFiles) {
                try {
                    val existingBytes = existingFile.readBytes()
                    val existingHash = java.security.MessageDigest.getInstance("MD5")
                        .digest(existingBytes)
                        .joinToString("") { "%02x".format(it) }
                    
                    if (sourceHash == existingHash) {
                        // 文件内容相同，不重复保存
                        return null
                    }
                } catch (e: Exception) {
                    // 忽略读取错误，继续检查下一个文件
                }
            }
            
            // 文件内容不同，保存新文件
            val fileName = getBillFileName(sourceUri, billType)
            val destFile = File(billDir, fileName)
            
            destFile.outputStream().use { output ->
                output.write(sourceBytes)
            }
            
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取账单文件名
     */
    private fun getBillFileName(uri: Uri, billType: String): String {
        val timestamp = dateFormat.format(Date())
        val typePrefix = when (billType.lowercase()) {
            "wechat" -> "微信"
            "alipay" -> "支付宝"
            else -> "账单"
        }
        return "${typePrefix}_${timestamp}.csv"
    }

    /**
     * 获取所有第三方账单文件
     */
    fun getAllBillFiles(): List<File> {
        val files = billDir.listFiles { _, name -> 
            name.endsWith(".csv") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".pdf")
        } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * 获取指定类型的账单文件
     */
    fun getBillFilesByType(billType: String): List<File> {
        val prefix = when (billType.lowercase()) {
            "wechat" -> "微信"
            "alipay" -> "支付宝"
            else -> ""
        }
        
        val files = if (prefix.isEmpty()) {
            getAllBillFiles()
        } else {
            billDir.listFiles { _, name -> 
                name.startsWith(prefix) && (name.endsWith(".csv") || name.endsWith(".xls") || 
                name.endsWith(".xlsx") || name.endsWith(".pdf"))
            }?.toList() ?: emptyList()
        }
        
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * 删除账单文件
     */
    fun deleteBillFile(file: File): Boolean {
        if (file.exists() && file.parentFile == billDir) {
            return file.delete()
        }
        return false
    }

    /**
     * 清空所有账单文件
     */
    fun clearAllBillFiles() {
        billDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * 检测账单类型
     */
    fun detectBillType(file: File): String {
        return when {
            file.name.startsWith("微信") -> "wechat"
            file.name.startsWith("支付宝") -> "alipay"
            else -> "unknown"
        }
    }

    /**
     * 获取账单文件大小（格式化）
     */
    fun getBillFileSize(file: File): String {
        val bytes = file.length()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    // ========== ZIP 备份管理 ==========

    /**
     * 创建 ZIP 备份文件名
     */
    fun createZipBackupFileName(isAuto: Boolean = true, customName: String? = null): String {
        return when {
            customName != null -> {
                val sanitizedName = customName.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]"), "_")
                "AK_Manual_${sanitizedName}_${dateFormat.format(Date())}.zip"
            }
            isAuto -> "AK_AutoBackup_${dateFormat.format(Date())}.zip"
            else -> "AK_ManualBackup_${dateFormat.format(Date())}.zip"
        }
    }

    /**
     * 写入 ZIP 格式备份（包含交易记录、资产记录和附件文件）
     */
    fun writeZipBackup(
        transactions: List<Transaction>,
        assets: List<Asset>,
        categoryMap: Map<Long, String>,
        maxKeep: Int = 15,
        isAuto: Boolean = true,
        customName: String? = null
    ): File? {
        return try {
            val zipFile = File(backupDir, createZipBackupFileName(isAuto, customName))
            
            ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                // 1. 写入交易记录 CSV
                val transactionsCsv = buildTransactionsCsv(transactions, categoryMap)
                zipOut.putNextEntry(ZipEntry("transactions.csv"))
                zipOut.write(transactionsCsv.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
                
                // 2. 写入资产记录 JSON
                val assetsData = assets.map { asset ->
                    AssetData(
                        id = asset.id,
                        date = asset.date,
                        amount = asset.amount,
                        status = asset.status.name,
                        categoryName = categoryMap[asset.categoryId],
                        targetPerson = asset.targetPerson,
                        targetAccount = asset.targetAccount,
                        note = asset.note,
                        isCompleted = asset.isCompleted,
                        attachments = AttachmentConverter.fromJson(asset.attachments),
                        createdAt = asset.createdAt,
                        updatedAt = asset.updatedAt
                    )
                }
                val backupData = ZipBackupData(
                    transactions = transactions.map { tx ->
                        TransactionData(
                            id = tx.id,
                            date = tx.date,
                            type = if (tx.type == TransactionType.INCOME) "Income" else "Expense",
                            amount = tx.amount,
                            categoryName = categoryMap[tx.categoryId] ?: "Other",
                            note = tx.note
                        )
                    },
                    assets = assetsData,
                    version = 1
                )
                
                zipOut.putNextEntry(ZipEntry("data.json"))
                zipOut.write(json.encodeToString(backupData).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
                
                // 3. 写入附件文件
                val attachmentEntries = mutableSetOf<String>()
                for (asset in assets) {
                    val attachmentList = AttachmentConverter.fromJson(asset.attachments)
                    for (attachment in attachmentList) {
                        val attachmentFile = File(attachment.filePath)
                        if (attachmentFile.exists()) {
                            // 使用附件ID作为文件名，避免重名
                            val entryName = "attachments/${attachment.id}_${attachment.fileName}"
                            if (entryName !in attachmentEntries) {
                                attachmentEntries.add(entryName)
                                zipOut.putNextEntry(ZipEntry(entryName))
                                attachmentFile.inputStream().use { input ->
                                    input.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                        }
                    }
                }
            }
            
            if (isAuto) {
                cleanUpOldZipBackups(maxKeep)
            }
            
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 构建交易记录 CSV 内容
     */
    private fun buildTransactionsCsv(
        transactions: List<Transaction>,
        categoryMap: Map<Long, String>
    ): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Type,Amount,Category,Note\n")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        for (tx in transactions) {
            val categoryName = categoryMap[tx.categoryId] ?: "Other"
            val typeString = if (tx.type == TransactionType.INCOME) "Income" else "Expense"
            val dateString = dateFormat.format(Date(tx.date))
            val safeNote = tx.note.replace("\"", "\"\"")
            sb.append("${tx.id},${dateString},${typeString},${tx.amount},${categoryName},\"${safeNote}\"\n")
        }
        
        return sb.toString()
    }

    /**
     * 从 ZIP 文件读取备份数据
     */
    fun readZipBackup(uri: Uri): ZipImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ZipImportResult(
                    transactions = emptyList(),
                    assets = emptyList(),
                    attachmentFiles = emptyMap(),
                    success = false,
                    errorMessage = "Cannot open input stream"
                )
            
            val transactions = mutableListOf<TransactionData>()
            val assets = mutableListOf<AssetData>()
            val attachmentFiles = mutableMapOf<String, File>()
            
            // 创建临时目录存放解压的附件
            val tempDir = File(context.cacheDir, "zip_extract_${System.currentTimeMillis()}").apply {
                mkdirs()
            }
            
            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> {
                            val content = zipIn.readBytes().toString(Charsets.UTF_8)
                            val backupData = json.decodeFromString<ZipBackupData>(content)
                            transactions.addAll(backupData.transactions)
                            assets.addAll(backupData.assets)
                        }
                        entry.name == "transactions.csv" -> {
                            // 也支持旧格式：从 CSV 解析交易记录
                            val content = zipIn.readBytes().toString(Charsets.UTF_8)
                            val parsed = parseTransactionsFromCsv(content)
                            if (parsed.isNotEmpty() && transactions.isEmpty()) {
                                transactions.addAll(parsed)
                            }
                        }
                        entry.name.startsWith("attachments/") -> {
                            // 解压附件到临时目录
                            val fileName = entry.name.substringAfter("attachments/")
                            if (fileName.isNotEmpty()) {
                                val tempFile = File(tempDir, fileName)
                                tempFile.outputStream().use { output ->
                                    zipIn.copyTo(output)
                                }
                                // 从文件名提取附件ID（格式：{id}_{originalName}）
                                val attachmentId = fileName.substringBefore("_", fileName)
                                attachmentFiles[attachmentId] = tempFile
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            ZipImportResult(
                transactions = transactions,
                assets = assets,
                attachmentFiles = attachmentFiles,
                success = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ZipImportResult(
                transactions = emptyList(),
                assets = emptyList(),
                attachmentFiles = emptyMap(),
                success = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    /**
     * 从 CSV 解析交易记录（向后兼容）
     */
    private fun parseTransactionsFromCsv(csvContent: String): List<TransactionData> {
        val result = mutableListOf<TransactionData>()
        val lines = csvContent.lines()
        if (lines.isEmpty()) return result
        
        // 跳过表头
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            try {
                val parts = parseCsvLine(line)
                if (parts.size >= 6) {
                    result.add(TransactionData(
                        id = parts[0].toLongOrNull() ?: continue,
                        date = parseDateToMillis(parts[1]),
                        type = parts[2],
                        amount = parts[3].toDoubleOrNull() ?: continue,
                        categoryName = parts[4],
                        note = parts[5]
                    ))
                }
            } catch (e: Exception) {
                // 跳过解析失败的行
            }
        }
        
        return result
    }

    /**
     * 解析 CSV 行
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().replace("\"\"", "\"").trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().replace("\"\"", "\"").trim())
        
        return result
    }

    /**
     * 解析日期字符串为毫秒时间戳
     */
    private fun parseDateToMillis(dateStr: String): Long {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        )
        
        for (format in formats) {
            try {
                val parsed = SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)
                if (parsed != null) return parsed.time
            } catch (e: Exception) { }
        }
        
        return System.currentTimeMillis()
    }

    /**
     * 将导入的附件文件复制到内部存储
     * @param attachmentId 附件ID
     * @param tempFile 临时文件
     * @param originalFileName 原始文件名
     * @return 新的附件对象
     */
    fun copyAttachmentToInternalStorage(
        attachmentId: String,
        tempFile: File,
        originalFileName: String
    ): Attachment? {
        return try {
            if (!tempFile.exists()) return null
            
            val extension = originalFileName.substringAfterLast(".", "")
            val newFileName = "${System.currentTimeMillis()}_$originalFileName"
            val destFile = File(attachmentsDir, newFileName)
            
            tempFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val mimeType = when (extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "csv" -> "text/csv"
                else -> "application/octet-stream"
            }
            
            Attachment(
                id = attachmentId,
                fileName = originalFileName,
                filePath = destFile.absolutePath,
                fileType = Attachment.getTypeFromExtension(extension),
                fileSize = destFile.length(),
                mimeType = mimeType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 清理导入时创建的临时文件
     */
    fun cleanupTempFiles() {
        val cacheDir = context.cacheDir
        cacheDir.listFiles()?.filter { it.name.startsWith("zip_extract_") }?.forEach { dir ->
            dir.deleteRecursively()
        }
    }

    /**
     * 清理旧的 ZIP 自动备份
     */
    fun cleanUpOldZipBackups(maxKeep: Int = 15) {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && name.contains("AutoBackup")
        }
        if (files != null && files.size > maxKeep) {
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            val filesToDelete = sortedFiles.drop(maxKeep)
            filesToDelete.forEach { it.delete() }
        }
    }

    /**
     * 获取所有 ZIP 自动备份
     */
    fun getAllZipAutoBackups(): List<File> {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && name.contains("AutoBackup")
        } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * 获取所有 ZIP 手动备份
     */
    fun getAllZipManualBackups(): List<File> {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && (name.contains("Manual") || name.contains("AK_Manual"))
        } ?: return emptyList()
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * 获取最新的 ZIP 自动备份日期字符串
     */
    fun getLatestZipAutoBackupDateStr(): String? {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && !name.contains("ManualBackup")
        }
        if (files.isNullOrEmpty()) return null
        val latest = files.maxByOrNull { it.lastModified() } ?: return null
        
        val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return displayFormat.format(Date(latest.lastModified()))
    }

    /**
     * 获取最新的 ZIP 手动备份日期字符串
     */
    fun getLatestZipManualBackupDateStr(): String? {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && name.contains("ManualBackup")
        }
        if (files.isNullOrEmpty()) return null
        val latest = files.maxByOrNull { it.lastModified() } ?: return null
        
        val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return displayFormat.format(Date(latest.lastModified()))
    }

    /**
     * 清空所有 ZIP 自动备份
     */
    fun clearAllZipAutoBackups() {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && !name.contains("Manual")
        }
        files?.forEach { it.delete() }
    }

    /**
     * 清空所有 ZIP 手动备份
     */
    fun clearAllZipManualBackups() {
        val files = backupDir.listFiles { _, name ->
            name.endsWith(".zip") && (name.contains("Manual") || name.contains("AK_Manual"))
        }
        files?.forEach { it.delete() }
    }

    /**
     * 将 ZIP 备份导出到指定 URI（用于用户导出）
     */
    fun exportZipToUri(
        uri: Uri,
        transactions: List<Transaction>,
        assets: List<Asset>,
        categoryMap: Map<Long, String>
    ): Boolean {
        return try {
            val tempZip = writeZipBackup(
                transactions = transactions,
                assets = assets,
                categoryMap = categoryMap,
                isAuto = false
            ) ?: return false
            
            context.contentResolver.openOutputStream(uri)?.use { output ->
                tempZip.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            
            // 删除临时文件（如果需要）
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
