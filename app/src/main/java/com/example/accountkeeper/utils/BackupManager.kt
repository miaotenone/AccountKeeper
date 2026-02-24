package com.example.accountkeeper.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    // Date format for the backup files
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

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
     * @return 保存后的文件
     */
    fun saveBillFile(sourceUri: Uri, billType: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val fileName = getBillFileName(sourceUri, billType)
            val destFile = File(billDir, fileName)
            
            destFile.outputStream().use { output ->
                inputStream.copyTo(output)
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
}
