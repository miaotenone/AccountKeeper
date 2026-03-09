package com.example.accountkeeper.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.accountkeeper.MainActivity
import com.example.accountkeeper.R
import kotlinx.coroutines.*
import java.io.File
import java.net.URL

/**
 * OTA 更新下载服务
 * 在前台运行，显示下载进度通知
 */
class UpdateDownloadService : Service() {

    companion object {
        const val ACTION_START_DOWNLOAD = "com.example.accountkeeper.action.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.accountkeeper.action.CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_VERSION_NAME = "version_name"

        const val NOTIFICATION_CHANNEL_ID = "update_download_channel"
        const val NOTIFICATION_ID = 1001

        // 广播 Action
        const val ACTION_DOWNLOAD_PROGRESS = "com.example.accountkeeper.action.DOWNLOAD_PROGRESS"
        const val ACTION_DOWNLOAD_COMPLETE = "com.example.accountkeeper.action.DOWNLOAD_COMPLETE"
        const val ACTION_DOWNLOAD_ERROR = "com.example.accountkeeper.action.DOWNLOAD_ERROR"
        
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_DOWNLOADED = "downloaded"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_ERROR_MESSAGE = "error_message"
    }

    private val binder = LocalBinder()
    private var downloadJob: Job? = null
    private var isDownloading = false
    private var downloadProgress = 0
    private lateinit var notificationManager: NotificationManager
    private lateinit var updateManager: UpdateManager

    inner class LocalBinder : Binder() {
        fun getService(): UpdateDownloadService = this@UpdateDownloadService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        updateManager = UpdateManager(this)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
                val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: ""
                startForegroundDownload(url, versionName)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "应用更新下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示应用更新下载进度"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundDownload(url: String, versionName: String) {
        if (isDownloading) return

        isDownloading = true
        downloadProgress = 0

        // 启动前台服务
        val notification = buildNotification(0, "正在下载 v$versionName...", "准备下载...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 开始下载
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = downloadFile(url) { progress, downloaded, total ->
                    downloadProgress = progress
                    updateNotification(progress, downloaded, total)
                    sendProgressBroadcast(progress, downloaded, total)
                }

                if (file != null) {
                    // 下载完成
                    updateNotificationComplete(file.absolutePath)
                    sendCompleteBroadcast(file.absolutePath)
                } else {
                    // 下载失败
                    updateNotificationError("下载失败")
                    sendErrorBroadcast("下载失败")
                }
            } catch (e: CancellationException) {
                // 用户取消
                updateNotificationError("下载已取消")
                sendErrorBroadcast("下载已取消")
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotificationError(e.message ?: "下载出错")
                sendErrorBroadcast(e.message ?: "下载出错")
            } finally {
                isDownloading = false
                delay(2000)  // 延迟停止服务，让用户看到结果
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun downloadFile(
        url: String,
        onProgress: (Int, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(downloadDir, "AccountKeeper_update.apk")

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val downloadUrl = URL(url)
            val connection = downloadUrl.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            val fileSize = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.getInputStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) return@withContext null  // 已取消

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (fileSize > 0) {
                            (downloadedBytes * 100 / fileSize).toInt()
                        } else {
                            0
                        }
                        onProgress(progress, downloadedBytes, fileSize)
                    }
                }
            }

            if (downloadedBytes > 0 && targetFile.exists()) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildNotification(progress: Int, title: String, content: String): Notification {
        val cancelIntent = Intent(this, UpdateDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "取消", cancelPendingIntent)
            .build()
    }

    private fun updateNotification(progress: Int, downloaded: Long, total: Long) {
        val downloadedMB = downloaded / (1024.0 * 1024.0)
        val totalMB = total / (1024.0 * 1024.0)
        val content = String.format("%.1f MB / %.1f MB", downloadedMB, totalMB)

        val notification = buildNotification(progress, "正在下载更新...", content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationComplete(filePath: String) {
        val apkFile = File(filePath)
        val installIntent = updateManager.installApkAndGetIntent(apkFile)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("下载完成")
            .setContentText("点击安装更新")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(true)
            .apply {
                if (installIntent != null) {
                    setContentIntent(installIntent)
                }
            }
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationError(message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("下载失败")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendProgressBroadcast(progress: Int, downloaded: Long, total: Long) {
        val intent = Intent(ACTION_DOWNLOAD_PROGRESS).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_DOWNLOADED, downloaded)
            putExtra(EXTRA_TOTAL, total)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun sendCompleteBroadcast(filePath: String) {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE).apply {
            putExtra(EXTRA_FILE_PATH, filePath)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun sendErrorBroadcast(message: String) {
        val intent = Intent(ACTION_DOWNLOAD_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        isDownloading = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun isDownloading(): Boolean = isDownloading

    fun getProgress(): Int = downloadProgress

    override fun onDestroy() {
        downloadJob?.cancel()
        super.onDestroy()
    }
}

/**
 * UpdateManager 扩展：安装 APK 并返回 PendingIntent
 */
fun UpdateManager.installApkAndGetIntent(apkFile: File): PendingIntent? {
    return try {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            android.net.Uri.fromFile(apkFile)
        }

        intent.apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
