package com.example.accountkeeper.utils

import com.example.accountkeeper.data.model.Transaction
import com.example.accountkeeper.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
 * 第三方账单解析器
 * 支持微信和支付宝账单导入
 */
object BillParser {

    data class ParsedTransaction(
        val id: Long = IdGenerator.generateId(),
        val date: Long,
        val type: TransactionType,
        val amount: Double,
        val category: String,
        val note: String,
        val originalType: String = "" // 原始账单类型（收入/支出）
    )

    /**
     * 微信账单解析器
     * 微信账单CSV格式：
     * 微信账单详情（UTF-8编码）
     * 交易时间,交易类型,交易对方,金额,收/支,交易单号,商户单号,备注
     * 2024-01-01 12:00:00,商家,xxx,100.00,支出,xxx,xxx,xxx
     */
    fun parseWeChatBill(lines: List<String>): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        // 跳过BOM和标题行
        var startIndex = 0
        for (i in lines.indices) {
            if (lines[i].contains("交易时间") || lines[i].contains("TransTime")) {
                startIndex = i + 1
                break
            }
        }
        
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val parts = parseCsvLine(line)
            if (parts.size >= 6) {
                try {
                    val dateStr = parts[0]
                    val transactionType = parts[1]
                    val counterparty = parts[2]
                    val amountStr = parts[3].replace("¥", "").replace(",", "").trim()
                    val incomeExpense = parts[4]
                    val transactionId = parts[5]
                    
                    val date = dateFormat.parse(dateStr)
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val type = when (incomeExpense) {
                        "收入", "/", "Income" -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    }
                    
                    val category = mapWeChatCategory(transactionType, type)
                    val note = "$transactionType - $counterparty"
                    
                    if (date != null && amount > 0) {
                        transactions.add(
                            ParsedTransaction(
                                id = transactionId.hashCode().toLong().takeIf { it > 0 } ?: IdGenerator.generateId(),
                                date = date.time,
                                type = type,
                                amount = amount,
                                category = category,
                                note = note,
                                originalType = incomeExpense
                            )
                        )
                    }
                } catch (e: Exception) {
                    // 跳过解析失败的行
                }
            }
        }
        
        return transactions
    }

    /**
     * 支付宝账单解析器
     * 支付宝账单CSV格式：
     * 支付宝,账号,姓名,开始日期,结束日期
     * 交易时间,商品说明,交易对方,收/支,金额,交易状态,交易分类,记账时间,付款方式...
     * 2024-01-01 12:00:00,商品名称,商家,支出,100.00,交易成功,餐饮美食,xxx,xxx...
     */
    fun parseAlipayBill(lines: List<String>): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        // 跳过BOM和标题行
        var startIndex = 0
        for (i in lines.indices) {
            if (lines[i].contains("交易时间") || lines[i].contains("TransTime")) {
                startIndex = i + 1
                break
            }
        }
        
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val parts = parseCsvLine(line)
            if (parts.size >= 8) {
                try {
                    val dateStr = parts[0]
                    val productName = parts[1]
                    val counterparty = parts[2]
                    val incomeExpense = parts[3]
                    val amountStr = parts[4].replace("¥", "").replace(",", "").trim()
                    val status = parts[5]
                    val category = parts[6]
                    
                    // 只处理交易成功的记录
                    if (status != "交易成功" && status != "Success") continue
                    
                    val date = dateFormat.parse(dateStr)
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val type = when (incomeExpense) {
                        "收入", "/", "Income" -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    }
                    
                    val mappedCategory = mapAlipayCategory(category, type)
                    val note = if (productName.isNotBlank()) productName else counterparty
                    
                    if (date != null && amount > 0) {
                        transactions.add(
                            ParsedTransaction(
                                date = date.time,
                                type = type,
                                amount = amount,
                                category = mappedCategory,
                                note = note,
                                originalType = incomeExpense
                            )
                        )
                    }
                } catch (e: Exception) {
                    // 跳过解析失败的行
                }
            }
        }
        
        return transactions
    }

    /**
     * 安全的CSV行解析器（处理引号）
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString().replace("\"\"", "\"").trim())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().replace("\"\"", "\"").trim())
        
        return result
    }

    /**
     * 微信分类映射
     */
    private fun mapWeChatCategory(original: String, type: TransactionType): String {
        return when {
            // 支出分类
            type == TransactionType.EXPENSE -> when {
                original.contains("餐饮") || original.contains("食品") -> "餐饮美食"
                original.contains("交通") || original.contains("出行") -> "交通出行"
                original.contains("购物") || original.contains("商城") -> "购物消费"
                original.contains("娱乐") || original.contains("游戏") -> "休闲娱乐"
                original.contains("医疗") || original.contains("健康") -> "医疗健康"
                original.contains("教育") || original.contains("学习") -> "教育培训"
                original.contains("通讯") || original.contains("话费") -> "通讯费用"
                original.contains("住房") || original.contains("物业") -> "住房物业"
                else -> "其他支出"
            }
            // 收入分类
            else -> when {
                original.contains("工资") || original.contains("薪资") -> "工资收入"
                original.contains("红包") -> "红包收入"
                original.contains("转账") -> "转账收入"
                else -> "其他收入"
            }
        }
    }

    /**
     * 支付宝分类映射
     */
    private fun mapAlipayCategory(original: String, type: TransactionType): String {
        return when {
            // 支出分类
            type == TransactionType.EXPENSE -> when {
                original.contains("餐饮") || original.contains("美食") -> "餐饮美食"
                original.contains("交通") || original.contains("出行") -> "交通出行"
                original.contains("购物") || original.contains("消费") -> "购物消费"
                original.contains("娱乐") || original.contains("休闲") -> "休闲娱乐"
                original.contains("医疗") || original.contains("健康") -> "医疗健康"
                original.contains("教育") || original.contains("培训") -> "教育培训"
                original.contains("通讯") || original.contains("话费") -> "通讯费用"
                original.contains("住房") || original.contains("物业") -> "住房物业"
                original.contains("生活") || original.contains("日用") -> "生活日用"
                else -> "其他支出"
            }
            // 收入分类
            else -> when {
                original.contains("工资") || original.contains("薪资") -> "工资收入"
                original.contains("红包") -> "红包收入"
                original.contains("转账") -> "转账收入"
                original.contains("理财") -> "理财收益"
                else -> "其他收入"
            }
        }
    }

    /**
     * 自动检测账单类型
     * @return "wechat", "alipay", 或 "unknown"
     */
    fun detectBillType(lines: List<String>): String {
        val sample = lines.take(20).joinToString(" ")
        return when {
            sample.contains("微信账单") || sample.contains("WeChat") -> "wechat"
            sample.contains("支付宝") || sample.contains("Alipay") -> "alipay"
            else -> "unknown"
        }
    }
}
