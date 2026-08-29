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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.accountkeeper.MainActivity
import com.example.accountkeeper.R
import com.example.accountkeeper.ui.theme.getAppStrings
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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
        
        private const val TAG = "UpdateDownloadService"
        private const val MAX_REDIRECTS = 10
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
                getAppStrings().appUpdateDownload,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getAppStrings().showUpdateDownloadProgress
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
        val notification = buildNotification(0, String.format(getAppStrings().downloadingVersion, versionName), getAppStrings().preparingDownload)
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
            var errorMsg: String? = null
            
            try {
                val file = downloadFile(url) { progress, downloaded, total ->
                    downloadProgress = progress
                    updateNotification(progress, downloaded, total)
                    sendProgressBroadcast(progress, downloaded, total)
                }

                if (file != null && file.exists() && file.length() > 0) {
                    // 下载完成
                    Log.d(TAG, "下载完成: ${file.absolutePath}, 大小: ${file.length()}")
                    updateNotificationComplete(file.absolutePath)
                    sendCompleteBroadcast(file.absolutePath)
                } else {
                    errorMsg = getAppStrings().downloadFailedFileSave
                }
            } catch (e: CancellationException) {
                errorMsg = getAppStrings().downloadCancelled
            } catch (e: Exception) {
                Log.e(TAG, "下载异常", e)
                errorMsg = String.format(getAppStrings().downloadFailedGeneric, e.javaClass.simpleName, e.message)
            }
            
            // 处理错误
            if (errorMsg != null) {
                Log.e(TAG, errorMsg)
                updateNotificationError(errorMsg)
                sendErrorBroadcast(errorMsg)
            }
            
            isDownloading = false
            delay(2000)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun downloadFile(
        url: String,
        onProgress: (Int, Long, Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        
        try {
            val downloadDir = File(cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(downloadDir, "AccountKeeper_update.apk")

            if (targetFile.exists()) {
                targetFile.delete()
            }

            // 处理重定向获取最终URL
            var currentUrl = url
            var redirectCount = 0
            
            while (redirectCount < MAX_REDIRECTS) {
                Log.d(TAG, "尝试连接: $currentUrl")
                
                connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "AccountKeeper-Downloader/1.0")
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Accept-Encoding", "identity")
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "响应码: $responseCode")
                
                when {
                    responseCode == HttpURLConnection.HTTP_OK -> {
                        Log.d(TAG, "连接成功，开始下载")
                        break
                    }
                    responseCode in listOf(301, 302, 303, 307, 308) -> {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        connection = null
                        
                        if (newUrl.isNullOrEmpty()) {
                            Log.e(TAG, "重定向URL为空")
                            return@withContext null
                        }
                        
                        Log.d(TAG, "重定向到: $newUrl")
                        currentUrl = newUrl
                        redirectCount++
                    }
                    else -> {
                        Log.e(TAG, "HTTP错误: $responseCode")
                        connection.disconnect()
                        return@withContext null
                    }
                }
            }
            
            if (connection == null) {
                Log.e(TAG, "无法建立连接")
                return@withContext null
            }
            
            val fileSize = connection.contentLengthLong
            Log.d(TAG, "文件大小: $fileSize bytes")
            
            if (fileSize <= 0) {
                // 有些服务器不返回Content-Length，继续下载
                Log.w(TAG, "服务器未返回文件大小")
            }
            
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) {
                            Log.d(TAG, "下载被取消")
                            return@withContext null
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (fileSize > 0) {
                            (downloadedBytes * 100 / fileSize).toInt().coerceIn(0, 100)
                        } else {
                            -1 // 未知大小
                        }
                        onProgress(progress, downloadedBytes, fileSize)
                    }
                }
            }
            
            connection.disconnect()
            connection = null

            Log.d(TAG, "下载完成，已下载: $downloadedBytes bytes")
            
            if (downloadedBytes > 0 && targetFile.exists()) {
                targetFile
            } else {
                Log.e(TAG, "文件不存在或大小为0")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载异常: ${e.message}", e)
            connection?.disconnect()
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
            .setProgress(100, progress, progress == 0 || progress < 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getAppStrings().cancel, cancelPendingIntent)
            .build()
    }

    private fun updateNotification(progress: Int, downloaded: Long, total: Long) {
        val downloadedMB = downloaded / (1024.0 * 1024.0)
        val content = if (total > 0) {
            val totalMB = total / (1024.0 * 1024.0)
            String.format("%.1f MB / %.1f MB", downloadedMB, totalMB)
        } else {
            String.format("%.1f MB", downloadedMB)
        }

        val notification = buildNotification(progress, getAppStrings().downloadingUpdate, content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationComplete(filePath: String) {
        val apkFile = File(filePath)
        val installIntent = updateManager.installApkAndGetIntent(apkFile)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getAppStrings().downloadComplete)
            .setContentText(getAppStrings().clickToInstallUpdate)
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
            .setContentTitle(getAppStrings().downloadFailed)
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
