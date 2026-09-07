package com.example.accountkeeper.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "approval_field_values",
    primaryKeys = ["approvalId", "fieldId"],
    foreignKeys = [
        ForeignKey(entity = BudgetApprovalRequest::class, parentColumns = ["id"], childColumns = ["approvalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = AssetFieldDefinition::class, parentColumns = ["id"], childColumns = ["fieldId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["fieldId"])]
)
data class ApprovalFieldValue(val approvalId: Long, val fieldId: Long, val value: String)