package com.example.accountkeeper.utils

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 文件格式转换工具
 * 支持将Excel和PDF文件转换为CSV格式供解析
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
     * 目前主要支持CSV格式
     * Excel和PDF需要额外依赖库支持
     */
    fun readLines(context: Context, uri: Uri): List<String>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val lines = reader.use { it.readLines() }
            // 移除BOM（字节顺序标记）
            if (lines.isNotEmpty() && lines[0].startsWith("\uFEFF")) {
                lines[0].substring(1)
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
     * Excel文件支持说明
     * 
     * 要支持Excel文件导入，需要添加以下依赖到 build.gradle.kts:
     * 
     * implementation("org.apache.poi:poi:5.2.5")
     * implementation("org.apache.poi:poi-ooxml:5.2.5")
     * 
     * 然后可以使用以下代码读取Excel:
     * 
     * val workbook = WorkbookFactory.create(inputStream)
     * val sheet = workbook.getSheetAt(0)
     * val rows = mutableListOf<String>()
     * for (row in sheet) {
     *     val cells = row.map { it.toString() }
     *     rows.add(cells.joinToString(","))
     * }
     * workbook.close()
     * 
     * 注意：Apache POI库较大，会增加APK体积约5-10MB
     */

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

    /**
     * 当前版本支持说明
     * 
     * 微信账单：
     * - 支持：CSV格式
     * - 导出方式：微信钱包 -> 账单 -> 下载账单 -> CSV格式
     * 
     * 支付宝账单：
     * - 支持：CSV格式
     * - 导出方式：支付宝 -> 账单 -> 导出账单 -> CSV格式
     * 
     * Excel格式：
     * - 需要添加Apache POI依赖
     * - 微信和支付宝都支持导出Excel格式
     * 
     * PDF格式：
     * - 需要添加PDFBox依赖
     * - 解析复杂度较高，建议优先使用CSV格式
     */
}