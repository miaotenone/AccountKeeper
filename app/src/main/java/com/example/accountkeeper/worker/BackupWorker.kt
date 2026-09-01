package com.example.accountkeeper.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.accountkeeper.data.repository.SettingsRepository
import com.example.accountkeeper.utils.AutoBackupCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val autoBackupCoordinator: AutoBackupCoordinator
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "scheduled_backup_work"

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

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            if (!settingsRepository.settingsFlow.first().isScheduledBackupEnabled) {
                Result.success()
            } else {
                autoBackupCoordinator.backupAfterDataChange()
                Result.success()
            }
        } catch (error: Exception) {
            error.printStackTrace()
            Result.retry()
        }
    }
}
