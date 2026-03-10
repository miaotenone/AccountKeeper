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
            // 如果指定了编码，直接使用
            if (encoding != null) {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                return readWithEncoding(inputStream, encoding)
            }
            
            // 先尝试 GBK 编码（支付宝账单通常使用GBK）
            try {
                val gbkStream = context.contentResolver.openInputStream(uri) ?: return null
                val gbkResult = readWithEncoding(gbkStream, "GBK")
                if (gbkResult != null && isValidChineseText(gbkResult)) {
                    return gbkResult
                }
            } catch (e: Exception) {
                // GBK读取失败，尝试UTF-8
            }
            
            // 尝试 UTF-8
            try {
                val utf8Stream = context.contentResolver.openInputStream(uri) ?: return null
                readWithEncoding(utf8Stream, "UTF-8")
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 检查文本是否是有效的中文文本（没有乱码）
     */
    private fun isValidChineseText(lines: List<String>): Boolean {
        if (lines.isEmpty()) return false
        
        // 检查前10行是否有有效中文
        val sampleLines = lines.take(10).joinToString("")
        
        // 检查是否包含常见的中文字符或关键字
        val chineseKeywords = listOf(
            "交易", "时间", "金额", "支出", "收入", "分类", "备注", 
            "支付宝", "微信", "账单", "商品", "成功"
        )
        
        val hasChineseKeywords = chineseKeywords.any { sampleLines.contains(it) }
        
        // 检查是否有乱码
        val hasGarbled = hasGarbledText(lines)
        
        return hasChineseKeywords && !hasGarbled
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
        
        // 检查前10行是否有乱码特征
        val sampleLines = lines.take(10).joinToString("")
        
        // 常见乱码特征：替换字符、无效UTF-8序列
        // 检查是否有连续的替换字符或乱码模式
        var garbledCount = 0
        var totalChars = 0
        
        for (char in sampleLines) {
            totalChars++
            // 替换字符（U+FFFD）表示解码失败
            if (char == '\uFFFD') {
                garbledCount++
            }
        }
        
        // 如果替换字符占比超过5%，认为是乱码
        if (totalChars > 0 && garbledCount.toFloat() / totalChars > 0.05f) {
            return true
        }
        
        // 检查是否有常见的中文乱码特征
        // GBK编码的中文用UTF-8读取会产生特定的乱码模式
        val garbledPatterns = listOf(
            "鈥�",   // 常见GBK乱码
            "銆�",
            "鈹�",
            "鈺�"
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
