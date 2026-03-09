package com.example.accountkeeper.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accountkeeper.utils.UpdateDownloadService
import com.example.accountkeeper.utils.UpdateInfo
import com.example.accountkeeper.utils.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 更新状态
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    object NoUpdate : UpdateState()
    data class Downloading(val progress: Int = 0, val downloadedBytes: Long = 0, val totalBytes: Long = 0) : UpdateState()
    data class DownloadComplete(val filePath: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val updateManager = UpdateManager(application)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _currentVersion = MutableStateFlow(updateManager.getCurrentVersionName())
    val currentVersion: StateFlow<String> = _currentVersion.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private var updateInfo: UpdateInfo? = null

    // 下载进度广播接收器
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UpdateDownloadService.ACTION_DOWNLOAD_PROGRESS -> {
                    val progress = intent.getIntExtra(UpdateDownloadService.EXTRA_PROGRESS, 0)
                    val downloaded = intent.getLongExtra(UpdateDownloadService.EXTRA_DOWNLOADED, 0)
                    val total = intent.getLongExtra(UpdateDownloadService.EXTRA_TOTAL, 0)
                    
                    _downloadProgress.value = progress
                    _updateState.value = UpdateState.Downloading(progress, downloaded, total)
                }
                UpdateDownloadService.ACTION_DOWNLOAD_COMPLETE -> {
                    val filePath = intent.getStringExtra(UpdateDownloadService.EXTRA_FILE_PATH) ?: ""
                    _updateState.value = UpdateState.DownloadComplete(filePath)
                    _downloadProgress.value = 100
                }
                UpdateDownloadService.ACTION_DOWNLOAD_ERROR -> {
                    val message = intent.getStringExtra(UpdateDownloadService.EXTRA_ERROR_MESSAGE) ?: "下载失败"
                    _updateState.value = UpdateState.Error(message)
                }
            }
        }
    }

    init {
        registerDownloadReceiver()
    }

    private fun registerDownloadReceiver() {
        val filter = IntentFilter().apply {
            addAction(UpdateDownloadService.ACTION_DOWNLOAD_PROGRESS)
            addAction(UpdateDownloadService.ACTION_DOWNLOAD_COMPLETE)
            addAction(UpdateDownloadService.ACTION_DOWNLOAD_ERROR)
        }
        
        ContextCompat.registerReceiver(
            getApplication(),
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * 检查更新
     */
    fun checkUpdate(force: Boolean = false) {
        if (_updateState.value is UpdateState.Checking) return

        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            
            try {
                val info = updateManager.checkUpdate()
                
                if (info != null) {
                    updateInfo = info
                    _updateState.value = UpdateState.UpdateAvailable(info)
                } else {
                    if (force) {
                        _updateState.value = UpdateState.NoUpdate
                    } else {
                        _updateState.value = UpdateState.Idle
                    }
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "检查更新失败")
            }
        }
    }

    /**
     * 下载更新
     */
    fun downloadUpdate() {
        val info = updateInfo ?: return
        if (_updateState.value is UpdateState.Downloading) return

        _updateState.value = UpdateState.Downloading()
        _downloadProgress.value = 0

        // 启动下载服务
        val intent = Intent(getApplication(), UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_START_DOWNLOAD
            putExtra(UpdateDownloadService.EXTRA_DOWNLOAD_URL, info.downloadUrl)
            putExtra(UpdateDownloadService.EXTRA_VERSION_NAME, info.versionName)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        val intent = Intent(getApplication(), UpdateDownloadService::class.java).apply {
            action = UpdateDownloadService.ACTION_CANCEL_DOWNLOAD
        }
        getApplication<Application>().startService(intent)
        _updateState.value = UpdateState.Idle
        _downloadProgress.value = 0
    }

    /**
     * 安装更新
     */
    fun installUpdate(): Boolean {
        val downloadedApk = updateManager.getDownloadedApk()
        
        return if (downloadedApk != null && downloadedApk.exists()) {
            updateManager.installApk(downloadedApk)
        } else {
            false
        }
    }

    /**
     * 安装指定路径的 APK
     */
    fun installApk(filePath: String): Boolean {
        val apkFile = File(filePath)
        return if (apkFile.exists()) {
            updateManager.installApk(apkFile)
        } else {
            false
        }
    }

    /**
     * 清理下载缓存
     */
    fun clearCache() {
        updateManager.clearDownloadCache()
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _updateState.value = UpdateState.Idle
        _downloadProgress.value = 0
    }

    /**
     * 获取当前版本名
     */
    fun getCurrentVersionName(): String = updateManager.getCurrentVersionName()

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            // 忽略
        }
    }
}
