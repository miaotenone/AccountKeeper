package com.example.accountkeeper.data.local

import androidx.room.TypeConverter
import com.example.accountkeeper.data.model.AssetRootType
import com.example.accountkeeper.data.model.AssetStatus
import com.example.accountkeeper.data.model.AttachmentOwnerType
import com.example.accountkeeper.data.model.BudgetApprovalStatus
import com.example.accountkeeper.data.model.BudgetApprovalType
import com.example.accountkeeper.data.model.TransactionSource
import com.example.accountkeeper.data.model.TransactionType

class Converters {
    @TypeConverter fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
    @TypeConverter fun fromTransactionSource(value: TransactionSource): String = value.name
    @TypeConverter fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)
    @TypeConverter fun fromAttachmentOwnerType(value: AttachmentOwnerType): String = value.name
    @TypeConverter fun toAttachmentOwnerType(value: String): AttachmentOwnerType = AttachmentOwnerType.valueOf(value)
    @TypeConverter fun fromAssetStatus(value: AssetStatus): String = value.name
    @TypeConverter fun toAssetStatus(value: String): AssetStatus = AssetStatus.valueOf(value)
    @TypeConverter fun fromAssetRootType(value: AssetRootType): String = value.name
    @TypeConverter fun toAssetRootType(value: String): AssetRootType = AssetRootType.valueOf(value)
    @TypeConverter fun fromBudgetApprovalType(value: BudgetApprovalType): String = value.name
    @TypeConverter fun toBudgetApprovalType(value: String): BudgetApprovalType = BudgetApprovalType.valueOf(value)
    @TypeConverter fun fromBudgetApprovalStatus(value: BudgetApprovalStatus): String = value.name
    @TypeConverter fun toBudgetApprovalStatus(value: String): BudgetApprovalStatus = BudgetApprovalStatus.valueOf(value)
}
