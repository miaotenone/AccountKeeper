package com.example.accountkeeper.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

object IdGenerator {
    private val counter = AtomicInteger(0)

    /**
     * 根据时间+该时间段数量生成18位的唯一ID。
     * 例如：yyyyMMddHHmmss (14位) + 0000 (4位) = 18位
     * 使用 Long 存储，最大支持19位数字。
     */
    fun generateId(): Long {
        val formatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val timePrefix = formatter.format(Date()) // 14位
        val count = counter.getAndIncrement() % 10000 // 取模保障不超过4位
        val countString = String.format(Locale.getDefault(), "%04d", count)
        return (timePrefix + countString).toLong()
    }
}
