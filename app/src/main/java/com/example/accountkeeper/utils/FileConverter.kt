package com.example.accountkeeper.utils

import android.content.Context
import android.net.Uri
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 文件格式转换工具
 * 支持将Excel和CSV文件转换为行列表供解析
 */
object FileConverter {

    /**
     * 检测文件类型
     * @return "csv", "excel", "pdf", 或 "unknown"
     */
    fun detectFileType(fileName: String): String {
        return when {
            fileName.endsWith(".csv", ignoreCase = true) -> "csv"
            fileName.endsWith(".xls", ignoreCase = true) || 
            fileName.endsWith(".xlsx", ignoreCase = true) -> "excel"
            fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
            else -> "unknown"
        }
    }

    /**
     * 读取文件内容为行列表
     * 支持 CSV (UTF-8/GBK) 和 Excel 格式
     * 
     * @param context Android Context
     * @param uri 文件URI
     * @param encoding 编码格式，默认自动检测（优先UTF-8，失败则尝试GBK）
     * @return 行列表，失败返回null
     */
    fun readLines(context: Context, uri: Uri, encoding: String? = null): List<String>? {
        val fileName = getFileName(context, uri)
        val fileType = detectFileType(fileName)
        
        return when (fileType) {
            "excel" -> readExcelLines(context, uri)
            "csv" -> readCsvLines(context, uri, encoding)
            else -> readCsvLines(context, uri, encoding) // 尝试作为文本文件读取
        }
    }

    /**
     * 读取Excel文件内容为行列表
     * 将每行单元格用逗号连接成字符串
     */
    fun readExcelLines(context: Context, uri: Uri): List<String>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val lines = mutableListOf<String>()
            
            inputStream.use { stream ->
                val workbook = WorkbookFactory.create(stream)
                val sheet = workbook.getSheetAt(0)
                
                for (row in sheet) {
                    val cells = mutableListOf<String>()
                    for (cell in row) {
                        val cellValue = when (cell.cellType) {
                            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue ?: ""
                            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                // 检查是否是日期格式
                                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                    cell.dateCellValue?.let { 
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(it)
                                    } ?: ""
                                } else {
                                    // 数字格式，避免科学计数法
                                    val num = cell.numericCellValue
                                    if (num == num.toLong().toDouble()) {
                                        num.toLong().toString()
                                    } else {
                                        num.toString()
                                    }
                                }
                            }
                            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
                            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                                // 尝试获取公式计算结果
                                try {
                                    cell.stringCellValue ?: cell.numericCellValue.toString()
                                } catch (e: Exception) {
                                    ""
                                }
                            }
                            else -> ""
                        }
                        cells.add(cellValue.trim())
                    }
                    // 只有非空行才添加
                    if (cells.any { it.isNotBlank() }) {
                        lines.add(cells.joinToString(","))
                    }
                }
                
                workbook.close()
            }
            
            lines
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 读取CSV文件内容为行列表
     * 支持自动检测编码（UTF-8 或 GBK）
     */
    fun readCsvLines(context: Context, uri: Uri, encoding: String? = null): List<String>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // 如果指定了编码，直接使用
            if (encoding != null) {
                return readWithEncoding(inputStream, encoding)
            }
            
            // 尝试 UTF-8，如果失败则尝试 GBK
            inputStream.mark(1024 * 1024) // 标记位置以便重置
            val utf8Result = readWithEncoding(inputStream, "UTF-8")
            
            // 检查是否有乱码特征
            if (utf8Result != null && !hasGarbledText(utf8Result)) {
                return utf8Result
            }
            
            // 重置流并尝试 GBK
            try {
                val newStream = context.contentResolver.openInputStream(uri) ?: return utf8Result
                readWithEncoding(newStream, "GBK")
            } catch (e: Exception) {
                utf8Result // 返回 UTF-8 结果作为备选
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 使用指定编码读取输入流
     */
    private fun readWithEncoding(inputStream: java.io.InputStream, encoding: String): List<String>? {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream, encoding))
            val lines = reader.use { it.readLines() }
            // 移除BOM（字节顺序标记）
            if (lines.isNotEmpty() && lines[0].startsWith("\uFEFF")) {
                lines.mapIndexed { index, line ->
                    if (index == 0 && line.startsWith("\uFEFF")) line.substring(1) else line
                }
            } else {
                lines
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 检查文本是否有乱码特征
     */
    private fun hasGarbledText(lines: List<String>): Boolean {
        if (lines.isEmpty()) return false
        
        // 检查前几行是否有乱码特征
        val sampleLines = lines.take(5).joinToString("")
        
        // 常见乱码特征：替换字符、无效UTF-8序列
        val garbledPatterns = listOf(
            "���",  // 常见乱码
            "�",    // 替换字符
            ""
        )
        
        return garbledPatterns.any { sampleLines.contains(it) }
    }

    /**
     * 获取文件名
     */
    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    /**
     * PDF文件支持说明
     * 
     * 要支持PDF文件导入，需要添加以下依赖到 build.gradle.kts:
     * 
     * implementation("com.tom-roush:pdfbox-android:2.0.27.0")
     * 
     * 然后可以使用以下代码读取PDF:
     * 
     * val document = PDDocument.load(inputStream)
     * val textStripper = PDFTextStripper()
     * val text = textStripper.getText(document)
     * document.close()
     * 
     * 注意：PDF格式灵活，需要根据具体账单格式进行解析
     * PDFBox库会增加APK体积约3-5MB
     */
}
