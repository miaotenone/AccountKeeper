package com.example.accountkeeper.utils

import com.example.accountkeeper.data.model.TransactionSource
import com.example.accountkeeper.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
 * 第三方账单解析器
 * 支持微信和支付宝账单导入
 */
object BillParser {

    /**
     * 解析后的交易数据
     */
    data class ParsedTransaction(
        val id: Long = IdGenerator.generateId(),
        val date: Long,
        val type: TransactionType,
        val amount: Double,
        val category: String,
        val note: String,
        val originalType: String = "",      // 原始账单类型（收入/支出）
        val source: TransactionSource = TransactionSource.MANUAL,
        val isRefund: Boolean = false,       // 是否为退款记录
        val relatedTransactionId: String? = null, // 关联的原交易单号
        val counterparty: String = ""        // 交易对方
    )

    /**
     * 解析结果
     * @param transactions 解析后的交易列表（已排除已退款的原交易）
     * @param excludedCount 被排除的交易数量（用于显示统计）
     */
    data class ParseResult(
        val transactions: List<ParsedTransaction>,
        val excludedCount: Int = 0
    )

    /**
     * 内部使用的原始交易数据（用于退款配对）
     */
    private data class RawTransaction(
        val transactionId: String,          // 交易单号
        val merchantOrderId: String,        // 商户单号
        val date: Long,
        val type: TransactionType,
        val amount: Double,
        val category: String,
        val note: String,
        val counterparty: String,
        val transactionType: String,        // 原始交易类型（如"商户消费"、"中铁网络-退款"）
        val status: String,                 // 当前状态
        val payMethod: String,              // 支付方式
        val source: TransactionSource,
        val isRefund: Boolean = false       // 是否为退款记录
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
     * 
     * 退款处理策略：
     * - 已全额退款的原交易：不导入
     * - 退款记录：尝试配对原交易，若配对成功则两者都不导入；若配对失败则保留退款记录作为收入
     */
    fun parseWeChatBill(lines: List<String>): ParseResult {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val rawTransactions = mutableListOf<RawTransaction>()
        
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
            if (line.startsWith("--") || (line.contains("共") && line.contains("笔记录"))) continue
            
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
                    val merchantOrderId = if (parts.size >= 10) parts[9] else ""
                    
                    val date = dateFormat.parse(dateStr)
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    
                    // 收/支判断
                    val type = when {
                        incomeExpense == "支出" || incomeExpense == "Expense" -> TransactionType.EXPENSE
                        incomeExpense == "收入" || incomeExpense == "Income" -> TransactionType.INCOME
                        else -> null // 中性交易（如充值、提现等）
                    }
                    
                    if (date != null && amount > 0 && type != null) {
                        val isRefund = transactionType.contains("退款") || status.contains("退款")
                        
                        rawTransactions.add(
                            RawTransaction(
                                transactionId = transactionId,
                                merchantOrderId = merchantOrderId,
                                date = date.time,
                                type = type,
                                amount = amount,
                                category = mapWeChatCategory(transactionType, type, counterparty),
                                note = buildWeChatNote(transactionType, counterparty, product, payMethod),
                                counterparty = counterparty,
                                transactionType = transactionType,
                                status = status,
                                payMethod = payMethod,
                                source = TransactionSource.WECHAT,
                                isRefund = isRefund
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
                    val merchantOrderId = if (parts.size >= 7) parts[6] else ""
                    
                    val date = dateFormat.parse(dateStr)
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    val type = when (incomeExpense) {
                        "支出", "Expense" -> TransactionType.EXPENSE
                        "收入", "Income" -> TransactionType.INCOME
                        else -> null
                    }
                    
                    if (date != null && amount > 0 && type != null) {
                        val isRefund = transactionType.contains("退款")
                        
                        rawTransactions.add(
                            RawTransaction(
                                transactionId = transactionId,
                                merchantOrderId = merchantOrderId,
                                date = date.time,
                                type = type,
                                amount = amount,
                                category = mapWeChatCategory(transactionType, type, counterparty),
                                note = "$transactionType - $counterparty",
                                counterparty = counterparty,
                                transactionType = transactionType,
                                status = "",
                                payMethod = "",
                                source = TransactionSource.WECHAT,
                                isRefund = isRefund
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 处理退款配对
        return processWeChatRefunds(rawTransactions)
    }

    /**
     * 处理微信退款配对逻辑
     */
    private fun processWeChatRefunds(rawTransactions: List<RawTransaction>): ParseResult {
        val resultTransactions = mutableListOf<ParsedTransaction>()
        val excludeIds = mutableSetOf<String>()
        var excludedCount = 0
        
        // 1. 识别所有需要排除的已退款交易
        val fullyRefundedTransactions = rawTransactions.filter { 
            it.status.contains("已全额退款") && it.type == TransactionType.EXPENSE 
        }
        fullyRefundedTransactions.forEach { 
            excludeIds.add(it.transactionId)
            excludedCount++
        }
        
        // 2. 处理退款记录，尝试配对
        val refundRecords = rawTransactions.filter { it.isRefund && it.type == TransactionType.INCOME }
        val normalTransactions = rawTransactions.filter { !it.isRefund && it.type != TransactionType.INCOME || (it.type == TransactionType.INCOME && !it.isRefund) }
        
        val matchedRefundIds = mutableSetOf<String>()
        
        for (refund in refundRecords) {
            // 尝试找到配对的原交易
            val matchedOriginal = normalTransactions.find { original ->
                original.type == TransactionType.EXPENSE &&
                original.amount == refund.amount &&
                // 时间相近（7天内）
                kotlin.math.abs(original.date - refund.date) <= 7 * 24 * 60 * 60 * 1000L &&
                // 商家名匹配（提取退款记录中的商家名）
                extractMerchantName(refund.transactionType) == extractMerchantName(original.transactionType) ||
                refund.counterparty == original.counterparty
            }
            
            if (matchedOriginal != null) {
                // 配对成功，都不导入
                excludeIds.add(refund.transactionId)
                excludeIds.add(matchedOriginal.transactionId)
                matchedRefundIds.add(refund.transactionId)
                excludedCount += 2
            }
        }
        
        // 3. 构建最终结果
        rawTransactions.forEach { raw ->
            if (raw.transactionId !in excludeIds) {
                resultTransactions.add(
                    ParsedTransaction(
                        id = kotlin.math.abs(raw.transactionId.hashCode().toLong()),
                        date = raw.date,
                        type = raw.type,
                        amount = raw.amount,
                        category = raw.category,
                        note = raw.note,
                        originalType = if (raw.type == TransactionType.EXPENSE) "支出" else "收入",
                        source = TransactionSource.WECHAT,
                        isRefund = raw.isRefund,
                        relatedTransactionId = null,
                        counterparty = raw.counterparty
                    )
                )
            }
        }
        
        return ParseResult(
            transactions = resultTransactions,
            excludedCount = excludedCount
        )
    }

    /**
     * 从退款交易类型中提取商家名
     * 例如: "中铁网络-退款" -> "中铁网络"
     */
    private fun extractMerchantName(transactionType: String): String {
        return transactionType
            .replace("-退款", "")
            .replace("退款-", "")
            .replace("退款", "")
            .trim()
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
    fun parseAlipayBill(lines: List<String>): ParseResult {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val rawTransactions = mutableListOf<RawTransaction>()
        
        // 查找表头行
        var startIndex = 0
        for (i in lines.indices) {
            val line = lines[i]
            if (line.contains("记录时间") || line.contains("交易时间") || line.contains("TransTime")) {
                startIndex = i + 1
                break
            }
        }
        
        var transactionIndex = 0  // 用于生成唯一ID的计数器
        
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
                val counterparty: String
                val status: String
                
                if (parts.size >= 5 && (parts[2] == "支出" || parts[2] == "收入" || parts[2] == "不计收支")) {
                    // 新格式 (8列): 记录时间, 分类, 收支类型, 金额, 备注, 账户, 来源, 标签
                    dateStr = parts[0]
                    category = parts[1]
                    incomeExpense = parts[2]
                    amountStr = parts[3].replace("¥", "").replace("￥", "").replace(",", "").trim()
                    note = parts[4].ifBlank { category }
                    counterparty = extractCounterpartyFromNote(parts[4])
                    status = ""
                } else if (parts.size >= 7) {
                    // 旧格式: 交易时间, 商品说明, 交易对方, 收/支, 金额, 交易状态, 交易分类, ...
                    dateStr = parts[0]
                    val productName = parts[1]
                    counterparty = parts[2]
                    incomeExpense = parts[3]
                    amountStr = parts[4].replace("¥", "").replace("￥", "").replace(",", "").trim()
                    status = parts[5]
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
                
                // 识别退款
                val isRefund = category == "退款" || note.contains("退款")
                
                if (date != null && amount > 0) {
                    transactionIndex++  // 递增计数器保证唯一性
                    // 使用时间戳+金额+类型+索引+备注hash来保证ID唯一
                    val uniqueId = "${date.time}_${amount}_${type.name}_${transactionIndex}_${note.hashCode()}"
                    
                    rawTransactions.add(
                        RawTransaction(
                            transactionId = uniqueId,
                            merchantOrderId = "",
                            date = date.time,
                            type = type,
                            amount = amount,
                            category = mapAlipayCategory(category, type, isRefund),
                            note = note,
                            counterparty = counterparty,
                            transactionType = category,
                            status = status,
                            payMethod = "",
                            source = TransactionSource.ALIPAY,
                            isRefund = isRefund
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 处理支付宝退款配对
        return processAlipayRefunds(rawTransactions)
    }

    /**
     * 处理支付宝退款配对逻辑
     */
    private fun processAlipayRefunds(rawTransactions: List<RawTransaction>): ParseResult {
        val resultTransactions = mutableListOf<ParsedTransaction>()
        val excludeIndices = mutableSetOf<Int>()
        var excludedCount = 0
        
        val refundRecords = rawTransactions.filter { it.isRefund }
        val normalExpenses = rawTransactions.filter { it.type == TransactionType.EXPENSE && !it.isRefund }
        
        for (refund in refundRecords) {
            // 尝试找到配对的原支出
            val matchedIndex = normalExpenses.indexOfFirst { expense ->
                expense.amount == refund.amount &&
                // 时间相近（7天内）
                kotlin.math.abs(expense.date - refund.date) <= 7 * 24 * 60 * 60 * 1000L &&
                // 备注中包含相似信息
                hasCommonKeywords(expense.note, refund.note) ||
                hasCommonKeywords(expense.counterparty, refund.counterparty)
            }
            
            if (matchedIndex >= 0) {
                val matchedOriginal = normalExpenses[matchedIndex]
                // 找到原始交易在rawTransactions中的索引
                val originalIndex = rawTransactions.indexOf(matchedOriginal)
                val refundIndex = rawTransactions.indexOf(refund)
                
                if (originalIndex >= 0 && refundIndex >= 0) {
                    excludeIndices.add(originalIndex)
                    excludeIndices.add(refundIndex)
                    excludedCount += 2
                }
            }
        }
        
        // 构建最终结果
        rawTransactions.forEachIndexed { index, raw ->
            if (index !in excludeIndices) {
                // 使用稳定的ID生成方式，确保同一笔交易每次解析都生成相同的ID
                val stableId = kotlin.math.abs(raw.transactionId.hashCode().toLong())
                resultTransactions.add(
                    ParsedTransaction(
                        id = stableId,
                        date = raw.date,
                        type = raw.type,
                        amount = raw.amount,
                        category = raw.category,
                        note = raw.note,
                        originalType = if (raw.type == TransactionType.EXPENSE) "支出" else "收入",
                        source = TransactionSource.ALIPAY,
                        isRefund = raw.isRefund,
                        relatedTransactionId = null,
                        counterparty = raw.counterparty
                    )
                )
            }
        }
        
        return ParseResult(
            transactions = resultTransactions,
            excludedCount = excludedCount
        )
    }

    /**
     * 从备注中提取交易对方
     */
    private fun extractCounterpartyFromNote(note: String): String {
        // 尝试从备注中提取商家名，格式如 "美团外卖-某某商家"
        val dashIndex = note.indexOf("-")
        return if (dashIndex > 0) {
            note.substring(0, dashIndex).trim()
        } else {
            note
        }
    }

    /**
     * 检查两个字符串是否有共同关键词
     */
    private fun hasCommonKeywords(str1: String, str2: String): Boolean {
        if (str1.isBlank() || str2.isBlank()) return false
        
        // 提取关键词（去除常见后缀）
        val keywords1 = extractKeywords(str1)
        val keywords2 = extractKeywords(str2)
        
        return keywords1.any { kw1 -> 
            keywords2.any { kw2 -> 
                kw1.length >= 2 && kw2.length >= 2 && (kw1.contains(kw2) || kw2.contains(kw1))
            }
        }
    }

    /**
     * 提取关键词
     */
    private fun extractKeywords(str: String): List<String> {
        return str
            .replace(Regex("""[\[\]【】()（）]"""), " ")
            .split(Regex("""[\s\-_—－,，、]"""))
            .filter { it.isNotBlank() && it.length >= 2 }
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
                // 特定商户识别
                counterparty.contains("中铁网络") || counterparty.contains("12306") || original.contains("12306") -> "交通出行"
                counterparty.contains("滴滴") || counterparty.contains("出行") -> "交通出行"
                counterparty.contains("京东") || counterparty.contains("拼多多") || counterparty.contains("淘宝") -> "购物消费"
                counterparty.contains("美团") || counterparty.contains("饿了么") -> "餐饮美食"
                counterparty.contains("腾讯") || counterparty.contains("音乐") || original.contains("音乐") -> "休闲娱乐"
                counterparty.contains("携程") -> "交通出行"
                
                // 通用分类
                original.contains("餐饮") || original.contains("食品") || original.contains("外卖") -> "餐饮美食"
                original.contains("交通") || original.contains("出行") || original.contains("打车") -> "交通出行"
                original.contains("购物") || original.contains("商城") -> "购物消费"
                original.contains("娱乐") || original.contains("游戏") -> "休闲娱乐"
                original.contains("医疗") || original.contains("健康") -> "医疗健康"
                original.contains("教育") || original.contains("学习") -> "教育培训"
                original.contains("通讯") || original.contains("话费") -> "通讯费用"
                original.contains("住房") || original.contains("物业") || original.contains("房租") -> "住房物业"
                original.contains("扫二维码") || original.contains("二维码") -> "日常消费"
                original.contains("商户消费") -> when {
                    counterparty.contains("餐饮") || counterparty.contains("食堂") || counterparty.contains("米线") || 
                    counterparty.contains("烤肉") || counterparty.contains("牛肉面") -> "餐饮美食"
                    counterparty.contains("超市") || counterparty.contains("便利") || counterparty.contains("零食") -> "购物消费"
                    counterparty.contains("咖啡") || counterparty.contains("奶茶") -> "餐饮美食"
                    else -> "日常消费"
                }
                original.contains("群收款") -> "日常消费"
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
    private fun mapAlipayCategory(original: String, type: TransactionType, isRefund: Boolean = false): String {
        return when {
            // 退款单独分类
            isRefund -> "退款收入"
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
                original.contains("投资") || original.contains("理财") -> "理财投资"
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
