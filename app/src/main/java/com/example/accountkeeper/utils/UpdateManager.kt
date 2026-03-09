package com.example.accountkeeper.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * OTA 更新管理器
 * 支持从 GitHub Releases 检查更新和下载安装
 */
class UpdateManager(internal val context: Context) {

    companion object {
        // GitHub 仓库信息
        private const val GITHUB_OWNER = "miaotenone"
        private const val GITHUB_REPO = "AccountKeeper"
        
        // GitHub API 地址
        private const val GITHUB_API_RELEASES = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        
        // 备用 API 地址（可配置自己的服务器）
        private const val CUSTOM_UPDATE_API = ""  // 留空则使用 GitHub
        
        // 下载超时时间
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 30000
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 获取当前版本名
     */
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    /**
     * 获取当前版本号
     */
    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            1
        }
    }

    /**
     * 检查更新
     * @return 更新信息，如果已经是最新版本则返回 null
     */
    suspend fun checkUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val releaseInfo = fetchLatestRelease() ?: return@withContext null
            
            // 比较版本号
            val currentVersion = getCurrentVersionName()
            val latestVersion = releaseInfo.tagName.removePrefix("v")
            
            if (compareVersions(latestVersion, currentVersion) > 0) {
                // 找到 APK 下载链接
                val apkAsset = releaseInfo.assets.find { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                UpdateInfo(
                    versionName = latestVersion,
                    versionCode = extractVersionCode(releaseInfo.tagName),
                    downloadUrl = apkAsset?.browserDownloadUrl ?: "",
                    fileSize = apkAsset?.size ?: 0,
                    releaseNotes = releaseInfo.body ?: "",
                    releaseDate = releaseInfo.publishedAt,
                    githubUrl = releaseInfo.htmlUrl
                )
            } else {
                null  // 已经是最新版本
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从 GitHub API 获取最新 Release 信息
     */
    private fun fetchLatestRelease(): GitHubRelease? {
        val apiUrl = if (CUSTOM_UPDATE_API.isNotEmpty()) {
            CUSTOM_UPDATE_API
        } else {
            GITHUB_API_RELEASES
        }

        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        
        return try {
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "AccountKeeper-UpdateChecker")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                json.decodeFromString<GitHubRelease>(response)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 比较版本号
     * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    /**
     * 从 tag 提取版本号（如 v1.1.20 -> 20）
     */
    private fun extractVersionCode(tag: String): Int {
        val parts = tag.removePrefix("v").split(".")
        return parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
    }

    /**
     * 下载 APK 文件
     * @param url 下载地址
     * @param onProgress 进度回调 (已下载字节数, 总字节数)
     * @return 下载完成的文件，失败返回 null
     */
    suspend fun downloadApk(
        url: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        try {
            val downloadDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val fileName = "AccountKeeper_update.apk"
            val targetFile = File(downloadDir, fileName)
            
            // 如果文件已存在，先删除
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val downloadUrl = URL(url)
            val connection = downloadUrl.openConnection() as HttpURLConnection
            
            connection.apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "AccountKeeper-Downloader")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val fileSize = connection.contentLengthLong
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(downloadedBytes, fileSize)
                    }
                }
            }

            if (downloadedBytes == fileSize && fileSize > 0) {
                targetFile
            } else {
                targetFile.delete()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 安装 APK
     */
    fun installApk(apkFile: File): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            intent.apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清理下载缓存
     */
    fun clearDownloadCache() {
        val downloadDir = File(context.cacheDir, "updates")
        if (downloadDir.exists() && downloadDir.isDirectory) {
            downloadDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * 获取已下载的 APK 文件（如果存在）
     */
    fun getDownloadedApk(): File? {
        val apkFile = File(context.cacheDir, "updates/AccountKeeper_update.apk")
        return if (apkFile.exists() && apkFile.length() > 0) apkFile else null
    }
}

/**
 * 更新信息
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val fileSize: Long,
    val releaseNotes: String,
    val releaseDate: String,
    val githubUrl: String
) {
    val fileSizeFormatted: String
        get() {
            val mb = fileSize / (1024.0 * 1024.0)
            return "%.1f MB".format(mb)
        }
}

/**
 * GitHub Release API 响应模型
 */
@Serializable
data class GitHubRelease(
    @kotlinx.serialization.SerialName("tag_name")
    val tagName: String,
    val name: String?,
    val body: String?,
    @kotlinx.serialization.SerialName("published_at")
    val publishedAt: String,
    @kotlinx.serialization.SerialName("html_url")
    val htmlUrl: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val size: Long,
    @kotlinx.serialization.SerialName("browser_download_url")
    val browserDownloadUrl: String
)
