package com.example.accountkeeper.data.model

/** Default asset categories, detail templates, and shared ordering. */

private val assetFieldEnglishNames = mapOf(
    "机构" to "Institution", "账户类型" to "Account type", "账号/卡号" to "Account/card number", "币种" to "Currency", "开户日期" to "Opening date", "利率" to "Interest rate", "到期日" to "Maturity date", "备注" to "Notes", "产品类型" to "Product type", "发行方" to "Issuer", "账户/代码" to "Account/code", "持有数量" to "Quantity held", "成本单价" to "Cost per unit", "买入日期" to "Purchase date", "风险等级" to "Risk level", "品牌" to "Brand", "型号" to "Model", "资产编号" to "Asset number", "序列号" to "Serial number", "供应商" to "Supplier", "购买日期" to "Purchase date", "保修到期" to "Warranty expiry", "存放地点" to "Storage location", "使用人/部门" to "User/department", "折旧年限" to "Depreciation years", "对方" to "Counterparty", "合同/借据编号" to "Contract/IOU number", "起始日" to "Start date", "还款方式" to "Repayment method", "担保信息" to "Collateral information", "收款方" to "Payee", "合同编号" to "Contract number", "支付日期" to "Payment date", "预计结算日期" to "Expected settlement date", "可退金额" to "Refundable amount", "结算状态" to "Settlement status", "发卡行" to "Card issuer", "卡类型" to "Card type", "尾号" to "Last four digits", "账单日" to "Statement day", "还款日" to "Payment day", "额度" to "Credit limit", "合同号" to "Contract number", "贷款类型" to "Loan type", "总期数" to "Total installments", "已还期数" to "Paid installments", "放款日" to "Loan date", "债权人" to "Creditor", "凭据编号" to "Reference number", "借入日期" to "Borrowing date", "负债类型" to "Liability type", "形成日期" to "Formation date"
)

fun localizedAssetFieldName(name: String, chinese: Boolean): String =
    if (chinese) assetFieldEnglishNames.entries.firstOrNull { it.value.equals(name, true) }?.key ?: name
    else assetFieldEnglishNames[name] ?: name

fun localizedAssetFieldType(type: AssetFieldType, chinese: Boolean): String = when (type) {
    AssetFieldType.TEXT -> if (chinese) "文本" else "Text"
    AssetFieldType.NUMBER -> if (chinese) "数字" else "Number"
    AssetFieldType.DATE -> if (chinese) "日期" else "Date"
    AssetFieldType.BOOLEAN -> if (chinese) "开关" else "Boolean"
    AssetFieldType.SINGLE_CHOICE -> if (chinese) "单选" else "Single choice"
}
object AssetCategoryDefaults {
    data class FieldTemplate(
        val name: String,
        val type: AssetFieldType = AssetFieldType.TEXT,
        val options: String = ""
    )

    val positiveCategoryNames = listOf("现金存款", "投资资产", "固定资产", "应收/借出", "押金/预付款")
    val negativeCategoryNames = listOf("信用卡", "贷款/分期", "借入/应付", "其他负债")

    val commonFieldTemplates = listOf(
        FieldTemplate("品牌/发行方"), FieldTemplate("机构/供应商"), FieldTemplate("编号/账号"),
        FieldTemplate("资产/负债类型"), FieldTemplate("币种"), FieldTemplate("启用/购买日期", AssetFieldType.DATE),
        FieldTemplate("到期日期", AssetFieldType.DATE), FieldTemplate("所在地"), FieldTemplate("责任人/部门"),
        FieldTemplate("保修/服务信息"), FieldTemplate("备注")
    )

    private val categoryFieldTemplates = mapOf(
        "现金存款" to listOf(FieldTemplate("机构"), FieldTemplate("账户类型"), FieldTemplate("账号/卡号"), FieldTemplate("币种"), FieldTemplate("开户日期", AssetFieldType.DATE), FieldTemplate("利率", AssetFieldType.NUMBER), FieldTemplate("到期日", AssetFieldType.DATE), FieldTemplate("备注")),
        "投资资产" to listOf(FieldTemplate("产品类型"), FieldTemplate("发行方"), FieldTemplate("账户/代码"), FieldTemplate("持有数量", AssetFieldType.NUMBER), FieldTemplate("成本单价", AssetFieldType.NUMBER), FieldTemplate("币种"), FieldTemplate("买入日期", AssetFieldType.DATE), FieldTemplate("到期日", AssetFieldType.DATE), FieldTemplate("风险等级", AssetFieldType.SINGLE_CHOICE, "低\n中\n高")),
        "固定资产" to listOf(FieldTemplate("品牌"), FieldTemplate("型号"), FieldTemplate("资产编号"), FieldTemplate("序列号"), FieldTemplate("供应商"), FieldTemplate("购买日期", AssetFieldType.DATE), FieldTemplate("保修到期", AssetFieldType.DATE), FieldTemplate("存放地点"), FieldTemplate("使用人/部门"), FieldTemplate("折旧年限", AssetFieldType.NUMBER)),
        "应收/借出" to listOf(FieldTemplate("对方"), FieldTemplate("合同/借据编号"), FieldTemplate("起始日", AssetFieldType.DATE), FieldTemplate("到期日", AssetFieldType.DATE), FieldTemplate("利率", AssetFieldType.NUMBER), FieldTemplate("还款方式"), FieldTemplate("担保信息"), FieldTemplate("备注")),
        "押金/预付款" to listOf(FieldTemplate("收款方"), FieldTemplate("合同编号"), FieldTemplate("支付日期", AssetFieldType.DATE), FieldTemplate("预计结算日期", AssetFieldType.DATE), FieldTemplate("可退金额", AssetFieldType.NUMBER), FieldTemplate("结算状态", AssetFieldType.SINGLE_CHOICE, "未结算\n部分结算\n已结算"), FieldTemplate("备注")),
        "信用卡" to listOf(FieldTemplate("发卡行"), FieldTemplate("卡类型"), FieldTemplate("尾号"), FieldTemplate("账单日", AssetFieldType.NUMBER), FieldTemplate("还款日", AssetFieldType.NUMBER), FieldTemplate("额度", AssetFieldType.NUMBER), FieldTemplate("币种")),
        "贷款/分期" to listOf(FieldTemplate("机构"), FieldTemplate("合同号"), FieldTemplate("贷款类型"), FieldTemplate("总期数", AssetFieldType.NUMBER), FieldTemplate("已还期数", AssetFieldType.NUMBER), FieldTemplate("利率", AssetFieldType.NUMBER), FieldTemplate("放款日", AssetFieldType.DATE), FieldTemplate("到期日", AssetFieldType.DATE), FieldTemplate("还款日", AssetFieldType.NUMBER)),
        "借入/应付" to listOf(FieldTemplate("债权人"), FieldTemplate("凭据编号"), FieldTemplate("借入日期", AssetFieldType.DATE), FieldTemplate("到期日", AssetFieldType.DATE), FieldTemplate("利率", AssetFieldType.NUMBER), FieldTemplate("还款方式"), FieldTemplate("担保信息")),
        "其他负债" to listOf(FieldTemplate("债权人"), FieldTemplate("负债类型"), FieldTemplate("凭据编号"), FieldTemplate("形成日期", AssetFieldType.DATE), FieldTemplate("到期日", AssetFieldType.DATE), FieldTemplate("币种"), FieldTemplate("备注"))
    )

    fun categoryNames(direction: AssetDirection): List<String> = when (direction) {
        AssetDirection.POSITIVE -> positiveCategoryNames
        AssetDirection.NEGATIVE -> negativeCategoryNames
    }

    fun fieldTemplatesFor(categoryName: String, isDefault: Boolean): List<FieldTemplate> =
        if (isDefault) categoryFieldTemplates[categoryName] ?: commonFieldTemplates else commonFieldTemplates

    fun opposite(direction: AssetDirection): AssetDirection = when (direction) {
        AssetDirection.POSITIVE -> AssetDirection.NEGATIVE
        AssetDirection.NEGATIVE -> AssetDirection.POSITIVE
    }
}

fun sortedAssetCategories(categories: List<AssetCategoryEntity>): List<AssetCategoryEntity> =
    categories.sortedWith(
        compareBy<AssetCategoryEntity> { if (it.direction == AssetDirection.NEGATIVE) 1 else 0 }
            .thenBy { defaultCategoryRank(it) }
            .thenBy { it.createdAt }
            .thenBy { it.name }
    )

private fun defaultCategoryRank(category: AssetCategoryEntity): Int {
    if (!category.isDefault) return Int.MAX_VALUE
    val index = AssetCategoryDefaults.categoryNames(category.direction)
        .indexOfFirst { it.equals(category.name, ignoreCase = true) }
    return if (index >= 0) index else Int.MAX_VALUE
}

fun sortedAssetLeafCategories(categories: List<AssetCategoryEntity>): List<AssetCategoryEntity> =
    sortedAssetCategories(categories)
