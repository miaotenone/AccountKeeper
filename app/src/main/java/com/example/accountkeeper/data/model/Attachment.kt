package com.example.accountkeeper.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 附件类型
 */
enum class AttachmentType {
    IMAGE,      // 图片 (png, jpg, jpeg, gif, webp等)
    EXCEL,      // Excel文件 (xls, xlsx)
    CSV,        // CSV文件
    OTHER       // 其他类型
}

/**
 * 附件数据模型
 * 用于存储资产记录的附件信息
 */
@Serializable
data class Attachment(
    val id: String,              // 唯一标识符
    val fileName: String,        // 原始文件名
    val filePath: String,        // 应用内部存储路径
    val fileType: AttachmentType,// 文件类型
    val fileSize: Long,          // 文件大小(字节)
    val mimeType: String,        // MIME类型
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * 根据文件扩展名判断附件类型
         */
        fun getTypeFromExtension(extension: String): AttachmentType {
            return when (extension.lowercase()) {
                "png", "jpg", "jpeg", "gif", "webp", "bmp" -> AttachmentType.IMAGE
                "xls", "xlsx" -> AttachmentType.EXCEL
                "csv" -> AttachmentType.CSV
                else -> AttachmentType.OTHER
            }
        }

        /**
         * 根据MIME类型判断附件类型
         */
        fun getTypeFromMimeType(mimeType: String): AttachmentType {
            return when {
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType.contains("spreadsheet") || mimeType.contains("excel") -> AttachmentType.EXCEL
                mimeType.contains("csv") -> AttachmentType.CSV
                else -> AttachmentType.OTHER
            }
        }
    }
}

/**
 * 附件列表的序列化/反序列化工具
 */
object AttachmentConverter {
    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(attachments: List<Attachment>): String {
        return if (attachments.isEmpty()) "" else json.encodeToString(attachments)
    }

    fun fromJson(jsonString: String): List<Attachment> {
        return if (jsonString.isBlank()) emptyList() 
        else json.decodeFromString(jsonString)
    }
}
