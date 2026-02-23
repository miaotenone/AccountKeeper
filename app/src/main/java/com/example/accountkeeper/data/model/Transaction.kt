package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Transaction(
    @PrimaryKey
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val date: Long,
    val categoryId: Long?,
    val note: String,
    val source: TransactionSource = TransactionSource.MANUAL
)
