package com.example.accountkeeper.utils

object CurrencyUtils {
    // 假设基准货币为系统默认的 ¥
    // 这是一个简单固定的汇率表示，真实应用中可能会通过 API 获取并在 DataStore 中缓存。
    private val exchangeRates = mapOf(
        "¥" to 1.0,
        "$" to 0.14,
        "€" to 0.13
    )

    /**
     * 将基准货币的数值转换为目标货币的数值
     * @param baseAmount 基准货币 (¥) 数值
     * @param targetCurrency 目标货币符号 (例如 "$")
     * @return 转换后数值
     */
    fun convertToDisplay(baseAmount: Double, targetCurrency: String): Double {
        val rate = exchangeRates[targetCurrency] ?: 1.0
        return baseAmount * rate
    }

    /**
     * 将目标货币的数值转换为基准货币的数值
     * @param displayAmount 目标货币数值
     * @param sourceCurrency 目标货币符号
     * @return 基准货币 (¥) 数值
     */
    fun convertToBase(displayAmount: Double, sourceCurrency: String): Double {
        val rate = exchangeRates[sourceCurrency] ?: 1.0
        return if (rate > 0) displayAmount / rate else displayAmount
    }
}
