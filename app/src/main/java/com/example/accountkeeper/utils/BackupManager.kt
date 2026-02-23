package com.example.accountkeeper.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val context: Context) {

    private val backupDir = File(context.filesDir, "backups").apply {
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
}
