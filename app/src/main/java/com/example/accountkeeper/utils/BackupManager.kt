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

    fun createBackupFileName(isAuto: Boolean = true): String {
        val prefix = if (isAuto) "AK_AutoBackup_" else "AK_ManualBackup_"
        return "$prefix${dateFormat.format(Date())}.csv"
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
    fun writeNewBackup(csvLineSequence: Sequence<String>, maxKeep: Int = 15, isAuto: Boolean = true) {
        val newFile = File(backupDir, createBackupFileName(isAuto))
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
     * Returns all manual backups sorted by latest first.
     */
    fun getAllManualBackups(): List<File> {
        val files = backupDir.listFiles { _, name -> 
            name.endsWith(".csv") && name.contains("ManualBackup") 
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
            name.endsWith(".csv") && !name.contains("ManualBackup")
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
}
