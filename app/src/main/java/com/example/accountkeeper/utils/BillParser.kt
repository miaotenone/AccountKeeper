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
     * 
     * 支持格式：
     * 1. Excel (.xlsx) 格式 - 微信导出的默认格式
     *    表头: 交易时间, 交易类型, 交易对方, 商品, 收/支, 金额(元), 支付方式, 当前状态, 交易单号, 商户单号, 备注
     * 
     * 2. CSV 格式 - 微信导出的备选格式
     *    表头: 交易时间, 交易类型, 交易对方, 金额, 收/支, 交易单号, 商户单号, 备注
     */
    fun parseWeChatBill(lines: List<String>): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        // 查找表头行
        var startIndex = 0
        var headerIndex = -1
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("交易时间") || line.contains("TransTime")) {
                headerIndex = i
                startIndex = i + 1
                break
            }
        }
        
        // 检测列格式（Excel 导出有11列，CSV 导出有8列）
        val isExcelFormat = if (headerIndex >= 0) {
            val headerParts = parseCsvLine(lines[headerIndex])
            headerParts.size >= 10 // Excel 格式有更多列
        } else false
        
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            // 跳过分隔线和统计信息
            if (line.startsWith("--") || line.contains("共") && line.contains("笔记录")) continue
            
            val parts = parseCsvLine(line)
            
            try {
                if (isExcelFormat && parts.size >= 9) {
                    // Excel 格式: 交易时间, 交易类型, 交易对方, 商品, 收/支, 金额(元), 支付方式, 当前状态, 交易单号, 商户单号, 备注
                    val dateStr = parts[0]
                    val transactionType = parts[1]
                    val counterparty = parts[2]
                    val product = parts[3]
                    val incomeExpense = parts[4]
                    val amountStr = parts[5].replace("¥", "").replace("￥", "").replace(",", "").trim()
                    val payMethod = parts[6]
                    val status = parts[7]
                    val transactionId = parts[8]
                    
                    // 跳过退款记录（已全额退款的不计入支出）
                    if (status.contains("已退款") || transactionType.contains("退款")) {
                        continue
                    }
                    
                    val date = dateFormat.parse(dateStr)
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    
                    // 收/支判断: "支出" 或 "收入"，"/" 表示中性交易（不记录）
                    val type = when {
                        incomeExpense == "支出" || incomeExpense == "Expense" -> TransactionType.EXPENSE
                        incomeExpense == "收入" || incomeExpense == "Income" -> TransactionType.INCOME
                        else -> continue // 跳过中性交易（如充值、提现等）
                    }
                    
                    val category = mapWeChatCategory(transactionType, type, counterparty)
                    val note = buildWeChatNote(transactionType, counterparty, product, payMethod)
                    
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
                } else if (parts.size >= 6) {
                    // CSV 格式: 交易时间, 交易类型, 交易对方, 金额, 收/支, 交易单号, 商户单号, 备注
                    val dateStr = parts[0]
                    val transactionType = parts[1]
                    val counterparty = parts[2]
                    val amountStr = parts[3].replace("¥", "").replace("￥", "").replace(",", "").trim()
                    val incomeExpense = parts[4]
                    val transactionId = parts[5]
                    
                    val date = dateFormat.parse(dateStr)
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val type = when (incomeExpense) {
                        "支出", "Expense" -> TransactionType.EXPENSE
                        "收入", "Income" -> TransactionType.INCOME
                        else -> continue
                    }
                    
                    val category = mapWeChatCategory(transactionType, type, counterparty)
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
                }
            } catch (e: Exception) {
                // 跳过解析失败的行
                e.printStackTrace()
            }
        }
        
        return transactions
    }

    /**
     * 构建微信交易备注
     */
    private fun buildWeChatNote(transactionType: String, counterparty: String, product: String, payMethod: String): String {
        val noteParts = mutableListOf<String>()
        
        if (transactionType.isNotBlank() && transactionType != "/") {
            noteParts.add(transactionType)
        }
        if (counterparty.isNotBlank() && counterparty != "/") {
            noteParts.add(counterparty)
        }
        if (product.isNotBlank() && product != "/" && !product.contains("收款方备注")) {
            noteParts.add(product)
        }
        if (payMethod.isNotBlank() && payMethod != "/") {
            noteParts.add("[$payMethod]")
        }
        
        return noteParts.joinToString(" - ").takeIf { it.isNotBlank() } ?: "微信交易"
    }

    /**
     * 支付宝账单解析器
     * 
     * 支持格式：
     * CSV 格式 (GBK 编码)
     * 表头: 记录时间, 分类, 收支类型, 金额, 备注, 账户, 来源, 标签
     * 
     * 示例数据：
     * 2026-02-20 18:03:55,餐饮,支出,64.30,美团外卖-某某商家,余额,账单同步,,
     */
    fun parseAlipayBill(lines: List<String>): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        // 查找表头行
        var startIndex = 0
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("记录时间") || line.contains("交易时间") || line.contains("TransTime")) {
                startIndex = i + 1
                break
            }
        }
        
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val parts = parseCsvLine(line)
            
            try {
                // 新格式: 记录时间, 分类, 收支类型, 金额, 备注, 账户, 来源, 标签
                // 旧格式: 交易时间, 商品说明, 交易对方, 收/支, 金额, 交易状态, 交易分类, ...
                
                val dateStr: String
                val category: String
                val incomeExpense: String
                val amountStr: String
                val note: String
                
                if (parts.size >= 5 && (parts[2] == "支出" || parts[2] == "收入" || parts[2] == "不计收支")) {
                    // 新格式 (8列): 记录时间, 分类, 收支类型, 金额, 备注, 账户, 来源, 标签
                    dateStr = parts[0]
                    category = parts[1]
                    incomeExpense = parts[2]
                    amountStr = parts[3].replace("¥", "").replace("￥", "").replace(",", "").trim()
                    note = parts[4].ifBlank { category }
                } else if (parts.size >= 7) {
                    // 旧格式: 交易时间, 商品说明, 交易对方, 收/支, 金额, 交易状态, 交易分类, ...
                    dateStr = parts[0]
                    val productName = parts[1]
                    val counterparty = parts[2]
                    incomeExpense = parts[3]
                    amountStr = parts[4].replace("¥", "").replace("￥", "").replace(",", "").trim()
                    val status = parts[5]
                    category = parts[6]
                    
                    // 只处理交易成功的记录
                    if (status != "交易成功" && status != "Success" && !status.contains("成功")) continue
                    
                    note = if (productName.isNotBlank()) productName else counterparty
                } else {
                    continue
                }
                
                // 跳过不计收支的项目
                if (incomeExpense == "不计收支") continue
                
                val date = dateFormat.parse(dateStr)
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                val type = when (incomeExpense) {
                    "收入", "Income" -> TransactionType.INCOME
                    "支出", "Expense" -> TransactionType.EXPENSE
                    else -> continue
                }
                
                val mappedCategory = mapAlipayCategory(category, type)
                
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
                e.printStackTrace()
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
    private fun mapWeChatCategory(original: String, type: TransactionType, counterparty: String = ""): String {
        return when {
            // 支出分类
            type == TransactionType.EXPENSE -> when {
                original.contains("餐饮") || original.contains("食品") || original.contains("外卖") -> "餐饮美食"
                original.contains("交通") || original.contains("出行") || original.contains("打车") || original.contains("滴滴") -> "交通出行"
                original.contains("购物") || original.contains("商城") || original.contains("京东") || original.contains("拼多多") -> "购物消费"
                original.contains("娱乐") || original.contains("游戏") -> "休闲娱乐"
                original.contains("医疗") || original.contains("健康") -> "医疗健康"
                original.contains("教育") || original.contains("学习") -> "教育培训"
                original.contains("通讯") || original.contains("话费") -> "通讯费用"
                original.contains("住房") || original.contains("物业") || original.contains("房租") -> "住房物业"
                original.contains("扫二维码") || original.contains("二维码") -> "日常消费"
                original.contains("商户消费") -> when {
                    counterparty.contains("餐饮") || counterparty.contains("食堂") -> "餐饮美食"
                    counterparty.contains("超市") || counterparty.contains("便利") -> "购物消费"
                    else -> "日常消费"
                }
                else -> "日常消费"
            }
            // 收入分类
            else -> when {
                original.contains("工资") || original.contains("薪资") -> "工资收入"
                original.contains("红包") -> "红包收入"
                original.contains("转账") -> "转账收入"
                original.contains("退款") -> "退款收入"
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
                original.contains("餐饮") || original.contains("美食") || original.contains("外卖") -> "餐饮美食"
                original.contains("交通") || original.contains("出行") -> "交通出行"
                original.contains("购物") || original.contains("消费") -> "购物消费"
                original.contains("娱乐") || original.contains("休闲") -> "休闲娱乐"
                original.contains("医疗") || original.contains("健康") -> "医疗健康"
                original.contains("教育") || original.contains("培训") -> "教育培训"
                original.contains("通讯") || original.contains("话费") -> "通讯费用"
                original.contains("住房") || original.contains("物业") -> "住房物业"
                original.contains("生活") || original.contains("日用") -> "生活日用"
                original.contains("转账") -> "转账支出"
                else -> "日常消费"
            }
            // 收入分类
            else -> when {
                original.contains("工资") || original.contains("薪资") -> "工资收入"
                original.contains("红包") -> "红包收入"
                original.contains("转账") -> "转账收入"
                original.contains("理财") -> "理财收益"
                original.contains("退款") -> "退款收入"
                else -> "其他收入"
            }
        }
    }

    /**
     * 自动检测账单类型
     * @return "wechat", "alipay", 或 "unknown"
     */
    fun detectBillType(lines: List<String>): String {
        val sample = lines.take(30).joinToString(" ")
        return when {
            sample.contains("微信支付账单") || sample.contains("微信账单") || sample.contains("WeChat") -> "wechat"
            sample.contains("支付宝") || sample.contains("Alipay") -> "alipay"
            // 检查表头特征
            lines.any { it.contains("记录时间") && it.contains("收支类型") } -> "alipay"
            lines.any { it.contains("交易时间") && it.contains("交易类型") && it.contains("收/支") } -> "wechat"
            else -> "unknown"
        }
    }
}