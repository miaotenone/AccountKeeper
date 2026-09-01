package com.example.accountkeeper.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class AttachmentType {
    IMAGE,
    PDF,
    EXCEL,
    CSV,
    DOCUMENT,
    TEXT,
    OTHER
}

@Serializable
data class Attachment(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileType: AttachmentType,
    val fileSize: Long,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun getTypeFromExtension(extension: String): AttachmentType {
            return when (extension.lowercase()) {
                "png", "jpg", "jpeg", "gif", "webp", "bmp" -> AttachmentType.IMAGE
                "pdf" -> AttachmentType.PDF
                "xls", "xlsx" -> AttachmentType.EXCEL
                "csv" -> AttachmentType.CSV
                "doc", "docx", "rtf", "odt" -> AttachmentType.DOCUMENT
                "txt", "md", "log", "json", "xml" -> AttachmentType.TEXT
                else -> AttachmentType.OTHER
            }
        }

        fun getTypeFromMimeType(mimeType: String): AttachmentType {
            return when {
                mimeType.startsWith("image/") -> AttachmentType.IMAGE
                mimeType == "application/pdf" -> AttachmentType.PDF
                mimeType.contains("spreadsheet") || mimeType.contains("excel") -> AttachmentType.EXCEL
                mimeType.contains("csv") -> AttachmentType.CSV
                mimeType.contains("wordprocessingml") || mimeType.contains("msword") || mimeType.contains("rtf") -> AttachmentType.DOCUMENT
                mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml") -> AttachmentType.TEXT
                else -> AttachmentType.OTHER
            }
        }
    }
}

object AttachmentConverter {
    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(attachments: List<Attachment>): String =
        if (attachments.isEmpty()) "" else json.encodeToString(attachments)

    fun fromJson(jsonString: String): List<Attachment> =
        if (jsonString.isBlank()) emptyList() else runCatching { json.decodeFromString<List<Attachment>>(jsonString) }.getOrDefault(emptyList())
}
