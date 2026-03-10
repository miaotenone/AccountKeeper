package com.example.accountkeeper.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.accountkeeper.data.repository.AssetRepository
import com.example.accountkeeper.data.repository.CategoryRepository
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.data.repository.TransactionRepository
import com.example.accountkeeper.utils.BackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers

/**
 * 定时备份 Worker
 * 用于执行每日自动备份任务
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val categoryRepository: CategoryRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "scheduled_backup_work"
        
        /**
         * 调度定时备份任务
         * @param intervalHours 备份间隔（小时）
         */
        fun schedule(context: Context, intervalHours: Int = 24) {
            val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                intervalHours.toLong(),
                TimeUnit.HOURS
            )
                .addTag("backup")
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
        
        /**
         * 取消定时备份任务
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.settingsFlow.first()
            
            // 检查是否启用定时备份
            if (!settings.isScheduledBackupEnabled) {
                return Result.success()
            }
            
            // 获取所有数据
            val transactions = transactionRepository.getAllTransactions().first()
            val assets = assetRepository.getAllAssets().first()
            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associate { it.id to it.name }
            
            // 执行备份
            withContext(Dispatchers.IO) {
                val backupManager = BackupManager(context)
                backupManager.writeZipBackup(
                    transactions = transactions,
                    assets = assets,
                    categoryMap = categoryMap,
                    maxKeep = settings.backupRetentionLimit,
                    isAuto = true
                )
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
