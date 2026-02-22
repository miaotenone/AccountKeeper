package com.example.accountkeeper.data.local

import androidx.room.TypeConverter
import com.example.accountkeeper.data.model.TransactionSource
import com.example.accountkeeper.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource {
        return TransactionSource.valueOf(value)
    }
}
