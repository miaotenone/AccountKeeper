package com.example.accountkeeper.data.model

enum class TransactionType {
    INCOME, EXPENSE, ASSET
}

enum class TransactionSource {
    MANUAL, ALIPAY, WECHAT
}

enum class AssetStatus {
    NONE,            // 未选择（默认）
    OWNED,           // 确定拥有（正资产）
    NOT_OWNED,       // 确定没有（负资产）
    IN_PROGRESS,     // 进行中（正负资产共用）
    TEMPORARILY_WITH_ME,    // 暂时在自己手里（借入）- 兼容旧数据
    TEMPORARILY_WITH_OTHERS  // 暂时在别人手里（借出）- 兼容旧数据
}
