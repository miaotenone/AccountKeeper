package com.example.accountkeeper.ui.theme

import android.content.Context

data class GeneralStrings(
    val home: String,
    val statistics: String,
    val categoryStatistics: String,
    val settings: String,
    val dataManagement: String,
    val quickSettings: String,
    val customizeExperience: String,
    val dataManagementDescription: String,
    val generalSettings: String,
    val generalSettingsDescription: String,
    val about: String,
    val aboutDescription: String,
    val totalAssets: String,
    val totalBalance: String,
    val thisMonth: String,
    val income: String,
    val expense: String,
    val balanceOverall: String,
    val startDate: String,
    val clearFilter: String,
    val endDate: String,
    val recentTransactions: String,
    val addTransaction: String,
    val editTransaction: String,
    val deleteTransaction: String,
    val deleteConfirm: String,
    val deleteConfirmTitle: String,
    val deleteConfirmMessage: String,
    val amount: String,
    val date: String,
    val category: String,
    val note: String,
    val save: String,
    val daily: String,
    val weekly: String,
    val monthly: String,
    val semiAnnual: String,
    val yearly: String,
    val custom: String,
    val totalIncome: String,
    val totalExpense: String,
    val total: String,
    val categoryRanking: String,
    val noTransactions: String,
    val selectRange: String,
    val manualDataManagement: String,
    val uploadBackup: String,
    val exportAll: String,
    val darkMode: String,
    val language: String,
    val currencySymbol: String,
    val newCategory: String,
    val name: String,
    val nameEmptyError: String,
    val nameExistsError: String,
    val add: String,
    val cancel: String,
    val ok: String,
    val change: String,
    val other: String,
    val hideAmount: String,
    val showAmount: String,
    val monthlyBalance: String,
    val navigate: String,
    val back: String,
    val billFiles: String,
    val transactions: String,
    val search: String,
    val searchResults: String,
    val searchHint: String,
    val noSearchResults: String,
    val filter: String,
    val all: String,
    val sortByTime: String,
    val sortByAmount: String,
    val sortSettings: String,
    val ascending: String,
    val descending: String,
    val timeRange: String,
    val dateRange: String,
    val sort: String,
    val categoryFilter: String,
    val allCategories: String,
    val timeDescending: String,
    val timeAscending: String,
    val amountDescending: String,
    val amountAscending: String,
    val attachments: String,
    val addAttachment: String,
    val removeAttachment: String,
    val noAttachments: String,
    val selectFile: String,
    val fileSizeTooLarge: String,
    val checkUpdate: String,
    val currentVersion: String,
    val downloadNow: String,
    val cancelDownload: String,
    val installNow: String,
    val downloadComplete: String,
    val alreadyLatest: String,
    val checkUpdateFailed: String,
    val downloading: String,
    val downloadHint: String,
    val downloadFailed: String,
    val openInBrowser: String,
    val updateContents: String,
    val feedback: String,
    val feedbackDescription: String,
    val problemDescription: String,
    val problemDescriptionHint: String,
    val contactInfo: String,
    val contactInfoHint: String,
    val sendFeedback: String,
    val feedbackSent: String,
    val version: String,
    val helpTutorial: String,
    val helpTutorialDescription: String,
    val helpTutorialShort: String,
    val github: String,
    val githubDescription: String,
    val contactAuthor: String,
    val contactAuthorDescription: String,
    val poweredBy: String,
    val authorName: String,
    val expenseRatio: String,
    val incomeRatio: String,
    val overallRatio: String,
    val thirdPartyBillImport: String,
    val thirdPartyBillImportDescription: String,
    val importWeChatBill: String,
    val importAlipayBill: String,
    val importWeChatAlipayBill: String,
    val manageImportedBills: String,
    val selected: String,
    val customizeAppExperience: String,
    val darkThemeEnabled: String,
    val lightThemeEnabled: String,
    val currentLanguage: String,
    val currentCurrency: String,
    val settingsInfo: String,
    val settingsInfoDescription: String,
    val restartAppForChanges: String,
    val chinese: String,
    val english: String,
    val categoryManagement: String,
    val categoryManagementDescription: String,
    val categoryAndTagManagement: String,
    val languageChineseLabel: String,
    val toggleOn: String,
    val toggleOff: String,
    val appUpdateDownload: String,
    val showUpdateDownloadProgress: String,
    val downloadingVersion: String,
    val preparingDownload: String,
    val downloadFailedFileSave: String,
    val downloadCancelled: String,
    val downloadFailedGeneric: String,
    val downloadingUpdate: String,
    val clickToInstallUpdate: String
)

data class BackupStrings(
    val close: String,
    val enterBackupName: String,
    val backupNamePlaceholder: String,
    val localBackupVault: String,
    val enableAutoBackup: String,
    val autoBackupDescription: String,
    val backupRetentionLimit: String,
    val backupRetentionDescription: String,
    val backupRetentionUnit: String,
    val backupThresholdDescription: String,
    val currentBackupStatus: String,
    val latestBackupFile: String,
    val noBackupFound: String,
    val createManualBackup: String,
    val manualBackupSuccess: String,
    val clearAutoBackups: String,
    val clearManualBackups: String,
    val backupsCleared: String,
    val manualBackupsCleared: String,
    val backupVault: String,
    val autoBackup: String,
    val manualBackup: String,
    val restore: String,
    val delete: String,
    val deleteBackupSuccess: String,
    val noManualBackups: String,
    val latestAutoBackup: String,
    val latestManualBackup: String,
    val noAutoBackup: String,
    val noManualBackup: String,
    val openManualBackupVault: String,
    val infoLimitation: String,
    val swipeDeleteConfirm: String,
    val swipeDeleteConfirmEnabled: String,
    val swipeDeleteConfirmDisabled: String,
    val defaultCategory: String,
    val addCategory: String,
    val categoryName: String,
    val renameCategory: String,
    val newName: String,
    val deleteCategory: String,
    val deleteCategoryConfirm: String,
    val appName: String,
    val appSubtitle: String
)

data class FeatureStrings(
    val asset: String,
    val assets: String,
    val assetsDescription: String,
    val netAssets: String,
    val totalLiabilities: String,
    val addAsset: String,
    val editAsset: String,
    val deleteAsset: String,
    val assetStatus: String,
    val none: String,
    val owned: String,
    val notOwned: String,
    val lost: String,
    val inProgress: String,
    val temporarilyWithMe: String,
    val temporarilyWithOthers: String,
    val targetPerson: String,
    val targetAccount: String,
    val includeInTotal: String,
    val isCompleted: String,
    val noAssets: String,
    val assetRecord: String,
    val positiveAsset: String,
    val negativeAsset: String,
    val assetFlowCompleted: String,
    val assetFlowInProgress: String,
    val markComplete: String,
    val markAsOwned: String,
    val markAsNotOwned: String,
    val markInProgress: String,
    val confirmDelete: String,
    val selectCategory: String,
    val budget: String,
    val showUnbudgetedSpend: String,
    val showBudgetedOnly: String,
    val budgetNotSet: String,
    val budgetSpent: String,
    val budgetRemaining: String,
    val budgetExceeded: String,
    val budgetInvalidAmount: String,
    val budgetZeroWarning: String,
    val budgetSubtitle: String,
    val financialArchive: String = "Financial Archive",
    val financialArchiveDescription: String = "Export or import a complete ZIP archive with business data, settings, attachments, and bill files.",
    val openArchive: String = "Open Archive",
    val attachmentOverviewDescription: String = "Review all linked files across transactions, assets, approvals, and bills.",
    val itemName: String = "Item Name",
    val specification: String = "Specification",
    val quantity: String = "Quantity",
    val expenseCategory: String = "Expense Category",
    val assetCategory: String = "Asset Category",
    val approvalCenter: String = "Approval Center",
    val approvalApplicant: String = "Applicant",
    val approvalApprover: String = "Approver",
    val approvalTodo: String = "To Do",
    val approvalHistory: String = "History",
    val approvalNewRequest: String = "New Request",
    val approvalBudgetAdjustment: String = "Budget Adjustment",
    val approvalPurchaseBudget: String = "Purchase Budget",
    val approvalPending: String = "Pending",
    val approvalApproved: String = "Approved",
    val approvalRejected: String = "Rejected",
    val approvalWithdrawn: String = "Withdrawn",
    val approvalReason: String = "Reason",
    val approvalPurchaseDate: String = "Purchase Date",
    val approvalSubmit: String = "Submit",
    val approvalWithdraw: String = "Withdraw",
    val approvalResubmit: String = "Resubmit",
    val approvalApprove: String = "Approve",
    val approvalReject: String = "Reject",
    val approvalDecisionNote: String = "Decision Note",
    val approvalCurrentBudget: String = "Current Budget",
    val approvalRemaining: String = "Remaining",
    val approvalNoRequests: String = "No approval requests",
    val approvalMissingCategory: String = "Please select a category",
    val approvalMissingItemName: String = "Please enter an item name",
    val approvalMissingReasonDetail: String = "Please enter a detailed reason",
    val approvalInvalidAmount: String = "Please enter an amount greater than zero",
    val approvalSubmitted: String = "Request submitted",
    val approvalView: String = "View",
    val approvalDetailTitle: String = "Approval Details",
    val approvalRequestId: String = "Request ID",
    val approvalRequestType: String = "Request Type",
    val approvalCreatedAt: String = "Created At",
    val approvalCurrentStatus: String = "Current Status",
    val approvalDecidedAt: String = "Decision Time",
    val approvalNotApplicable: String = "Not applicable",
    val approvalPurchaseProgress: String = "Procurement Progress",
    val approvalAwaitingPurchase: String = "Approved, awaiting purchase",
    val approvalPurchaseInProgress: String = "Purchase in progress",
    val approvalConfirmedOwned: String = "Owned confirmed",
    val approvalNotStarted: String = "Not started",
    val approvalRelatedAsset: String = "Related Asset",
    val approvalRealTransaction: String = "Real Expense Transaction",
    val approvalNotSet: String = "Not set",
    val approvalMissingAssetCategory: String = "Please select an asset category",
    val approvalInvalidQuantity: String = "Please enter a quantity greater than zero",
    val budgetsDescription: String,
    val addAssetCategoryFirst: String,
    val enterValidAmount: String,
    val mergeSuccess: String,
    val mergeNoNewData: String,
    val mergeParseFailed: String,
    val cannotReadFile: String,
    val unrecognizedBillFormat: String,
    val noTransactionsToImport: String,
    val wechat: String,
    val alipay: String,
    val excludedRefundInfo: String,
    val billImportSuccess: String,
    val importFailed: String,
    val clearAllTransactionsButton: String,
    val dangerWarning: String,
    val clearAllDataWarningMessage: String,
    val allRecordsCleared: String,
    val clearAnyway: String,
    val clearAllData: String,
    val clearAllDataDescription: String,
    val clearAllDataWarning: String,
    val clearAllDataConfirmTitle: String,
    val clearAllDataWillDelete: String,
    val clearAllDataTransactions: String,
    val clearAllDataAssets: String,
    val clearAllDataAttachments: String,
    val clearAllDataCannotUndo: String,
    val clearAllDataConfirm: String,
    val clearAllDataSuccess: String,
    val clearTransactions: String,
    val clearAssets: String,
    val clearTransactionsConfirmTitle: String,
    val clearTransactionsConfirm: String,
    val clearTransactionsSuccess: String,
    val clearAssetsConfirmTitle: String,
    val clearAssetsConfirm: String,
    val clearAssetsSuccess: String,
    val reimportBill: String,
    val reimportSuccess: String,
    val noNewRecordsToImport: String,
    val thirdPartyBillFileManagement: String,
    val noImportedBillFiles: String,
    val wechatBill: String,
    val alipayBill: String,
    val unknownBill: String,
    val archiveExportSuccess: String,
    val archiveExportFailed: String
)

data class HelpStrings(
    val helpAboutProject: String,
    val helpAboutDescription1: String,
    val helpAboutDescription2: String,
    val helpHomeFeatures: String,
    val helpBalanceCard: String,
    val helpBalanceCardDetail1: String,
    val helpBalanceCardDetail2: String,
    val helpBalanceCardDetail3: String,
    val helpSearchFeature: String,
    val helpSearchDetail1: String,
    val helpSearchDetail2: String,
    val helpSearchDetail3: String,
    val helpTransactionListOps: String,
    val helpTxTapDetail: String,
    val helpTxSwipeDetail: String,
    val helpTxLongPressDetail: String,
    val helpTxBatchDelete: String,
    val helpAddTransaction: String,
    val helpAddTxDetail1: String,
    val helpAddTxDetail2: String,
    val helpAddTxDetail3: String,
    val helpAddTxDetail4: String,
    val helpStatistics: String,
    val helpTimeRangeSelection: String,
    val helpTimeRangeDay: String,
    val helpTimeRangeWeek: String,
    val helpTimeRangeMonth: String,
    val helpTimeRangeYear: String,
    val helpTimeRangeCustom: String,
    val helpChartDisplay: String,
    val helpChartLine: String,
    val helpChartPie: String,
    val helpChartRanking: String,
    val helpCategoryFilter: String,
    val helpCategoryFilterDetail1: String,
    val helpCategoryFilterDetail2: String,
    val helpCategoryFilterDetail3: String,
    val helpAssetManagement: String,
    val helpAssetStatistics: String,
    val helpAssetNetWorth: String,
    val helpCurrentAssets: String,
    val helpCurrentLiabilities: String,
    val helpTransactionBalance: String,
    val helpAddAssetRecord: String,
    val helpAddAssetDetail1: String,
    val helpAddAssetDetail2: String,
    val helpAddAssetDetail3: String,
    val helpAddAssetDetail4: String,
    val helpStatusExplanation: String,
    val helpStatusInProgress: String,
    val helpStatusCompleted: String,
    val helpStatusCancelled: String,
    val helpDataManagement: String,
    val helpExportData: String,
    val helpExportDetail1: String,
    val helpExportDetail2: String,
    val helpExportDetail3: String,
    val helpImportData: String,
    val helpImportDetail1: String,
    val helpImportDetail2: String,
    val helpImportDetail3: String,
    val helpBillImport: String,
    val helpBillImportDetail1: String,
    val helpBillImportDetail2: String,
    val helpBillImportDetail3: String,
    val helpBillImportDetail4: String,
    val helpBillImportDetail5: String,
    val helpHowToExportBill: String,
    val helpExportBillWechat: String,
    val helpExportBillAlipay: String,
    val helpExportBillFormat: String,
    val helpExportBillImport: String,
    val helpAutoBackup: String,
    val helpAutoBackupDetail1: String,
    val helpAutoBackupDetail2: String,
    val helpAutoBackupDetail3: String,
    val helpAutoBackupDetail4: String,
    val helpCategoryManagement: String,
    val helpCategoryTypes: String,
    val helpExpenseCategories: String,
    val helpIncomeCategories: String,
    val helpAssetCategories: String,
    val helpManagementOps: String,
    val helpAddCategory: String,
    val helpRenameCategory: String,
    val helpDeleteCategory: String,
    val helpPresetCategoryNoDelete: String,
    val helpPersonalization: String,
    val helpAppearanceSettings: String,
    val helpDarkMode: String,
    val helpLightMode: String,
    val helpLanguage: String,
    val helpCurrency: String,
    val helpNotes: String,
    val helpRestartRequired: String,
    val helpOldSettingsBeforeRestart: String,
    val helpTips: String,
    val helpDailyTips: String,
    val helpDailyTip1: String,
    val helpDailyTip2: String,
    val helpDailyTip3: String,
    val helpDataSafety: String,
    val helpDataSafetyTip1: String,
    val helpDataSafetyTip2: String,
    val helpDataSafetyTip3: String,
    val helpBillImportTips: String,
    val helpBillImportTip1: String,
    val helpBillImportTip2: String,
    val helpBillImportTip3: String,
    val helpFAQ: String,
    val helpFAQRecover: String,
    val helpFAQRecoverDetail1: String,
    val helpFAQRecoverDetail2: String,
    val helpFAQPartialImport: String,
    val helpFAQPartialImportDetail1: String,
    val helpFAQPartialImportDetail2: String,
    val helpFAQPartialImportDetail3: String,
    val helpFAQSync: String,
    val helpFAQSyncDetail1: String,
    val helpFAQSyncDetail2: String,
    val helpFAQSyncDetail3: String,
    val helpHomeTimeRange: String,
    val helpHomeTimeRangeDetail1: String,
    val helpHomeTimeRangeDetail2: String,
    val helpHomeTimeRangeDetail3: String,
    val helpHomeFilter: String,
    val helpHomeFilterDetail1: String,
    val helpHomeFilterDetail2: String,
    val helpHomeFilterDetail3: String,
    val helpBudgetPeriods: String,
    val helpBudgetPeriodsDetail1: String,
    val helpBudgetPeriodsDetail2: String,
    val helpBudgetPeriodsDetail3: String,
    val helpBillPreview: String,
    val helpBillPreviewDetail1: String,
    val helpBillPreviewDetail2: String,
    val helpAssetAttachment: String,
    val helpAssetAttachmentDetail1: String,
    val helpAssetAttachmentDetail2: String
)

data class AppStrings(
    val general: GeneralStrings,
    val backup: BackupStrings,
    val feature: FeatureStrings,
    val help: HelpStrings
) {
    val home get() = general.home
    val statistics get() = general.statistics
    val categoryStatistics get() = general.categoryStatistics
    val settings get() = general.settings
    val dataManagement get() = general.dataManagement
    val quickSettings get() = general.quickSettings
    val customizeExperience get() = general.customizeExperience
    val dataManagementDescription get() = general.dataManagementDescription
    val generalSettings get() = general.generalSettings
    val generalSettingsDescription get() = general.generalSettingsDescription
    val about get() = general.about
    val aboutDescription get() = general.aboutDescription
    val totalAssets get() = general.totalAssets
    val totalBalance get() = general.totalBalance
    val thisMonth get() = general.thisMonth
    val income get() = general.income
    val expense get() = general.expense
    val balanceOverall get() = general.balanceOverall
    val startDate get() = general.startDate
    val clearFilter get() = general.clearFilter
    val endDate get() = general.endDate
    val recentTransactions get() = general.recentTransactions
    val addTransaction get() = general.addTransaction
    val editTransaction get() = general.editTransaction
    val deleteTransaction get() = general.deleteTransaction
    val deleteConfirm get() = general.deleteConfirm
    val deleteConfirmTitle get() = general.deleteConfirmTitle
    val deleteConfirmMessage get() = general.deleteConfirmMessage
    val amount get() = general.amount
    val date get() = general.date
    val category get() = general.category
    val note get() = general.note
    val save get() = general.save
    val daily get() = general.daily
    val weekly get() = general.weekly
    val monthly get() = general.monthly
    val semiAnnual get() = general.semiAnnual
    val yearly get() = general.yearly
    val custom get() = general.custom
    val totalIncome get() = general.totalIncome
    val totalExpense get() = general.totalExpense
    val total get() = general.total
    val categoryRanking get() = general.categoryRanking
    val noTransactions get() = general.noTransactions
    val selectRange get() = general.selectRange
    val manualDataManagement get() = general.manualDataManagement
    val uploadBackup get() = general.uploadBackup
    val exportAll get() = general.exportAll
    val darkMode get() = general.darkMode
    val language get() = general.language
    val currencySymbol get() = general.currencySymbol
    val newCategory get() = general.newCategory
    val name get() = general.name
    val nameEmptyError get() = general.nameEmptyError
    val nameExistsError get() = general.nameExistsError
    val add get() = general.add
    val cancel get() = general.cancel
    val ok get() = general.ok
    val change get() = general.change
    val other get() = general.other
    val hideAmount get() = general.hideAmount
    val showAmount get() = general.showAmount
    val monthlyBalance get() = general.monthlyBalance
    val navigate get() = general.navigate
    val back get() = general.back
    val billFiles get() = general.billFiles
    val transactions get() = general.transactions
    val search get() = general.search
    val searchResults get() = general.searchResults
    val searchHint get() = general.searchHint
    val noSearchResults get() = general.noSearchResults
    val filter get() = general.filter
    val all get() = general.all
    val sortByTime get() = general.sortByTime
    val sortByAmount get() = general.sortByAmount
    val sortSettings get() = general.sortSettings
    val ascending get() = general.ascending
    val descending get() = general.descending
    val timeRange get() = general.timeRange
    val dateRange get() = general.dateRange
    val sort get() = general.sort
    val categoryFilter get() = general.categoryFilter
    val allCategories get() = general.allCategories
    val timeDescending get() = general.timeDescending
    val timeAscending get() = general.timeAscending
    val amountDescending get() = general.amountDescending
    val amountAscending get() = general.amountAscending
    val attachments get() = general.attachments
    val addAttachment get() = general.addAttachment
    val removeAttachment get() = general.removeAttachment
    val noAttachments get() = general.noAttachments
    val selectFile get() = general.selectFile
    val fileSizeTooLarge get() = general.fileSizeTooLarge
    val checkUpdate get() = general.checkUpdate
    val currentVersion get() = general.currentVersion
    val downloadNow get() = general.downloadNow
    val cancelDownload get() = general.cancelDownload
    val installNow get() = general.installNow
    val downloadComplete get() = general.downloadComplete
    val alreadyLatest get() = general.alreadyLatest
    val checkUpdateFailed get() = general.checkUpdateFailed
    val downloading get() = general.downloading
    val downloadHint get() = general.downloadHint
    val downloadFailed get() = general.downloadFailed
    val openInBrowser get() = general.openInBrowser
    val updateContents get() = general.updateContents
    val feedback get() = general.feedback
    val feedbackDescription get() = general.feedbackDescription
    val problemDescription get() = general.problemDescription
    val problemDescriptionHint get() = general.problemDescriptionHint
    val contactInfo get() = general.contactInfo
    val contactInfoHint get() = general.contactInfoHint
    val sendFeedback get() = general.sendFeedback
    val feedbackSent get() = general.feedbackSent
    val version get() = general.version
    val helpTutorial get() = general.helpTutorial
    val helpTutorialDescription get() = general.helpTutorialDescription
    val helpTutorialShort get() = general.helpTutorialShort
    val github get() = general.github
    val githubDescription get() = general.githubDescription
    val contactAuthor get() = general.contactAuthor
    val contactAuthorDescription get() = general.contactAuthorDescription
    val poweredBy get() = general.poweredBy
    val authorName get() = general.authorName
    val expenseRatio get() = general.expenseRatio
    val incomeRatio get() = general.incomeRatio
    val overallRatio get() = general.overallRatio
    val thirdPartyBillImport get() = general.thirdPartyBillImport
    val thirdPartyBillImportDescription get() = general.thirdPartyBillImportDescription
    val importWeChatBill get() = general.importWeChatBill
    val importAlipayBill get() = general.importAlipayBill
    val importWeChatAlipayBill get() = general.importWeChatAlipayBill
    val manageImportedBills get() = general.manageImportedBills
    val selected get() = general.selected
    val appName get() = backup.appName
    val appSubtitle get() = backup.appSubtitle
    val customizeAppExperience get() = general.customizeAppExperience
    val darkThemeEnabled get() = general.darkThemeEnabled
    val lightThemeEnabled get() = general.lightThemeEnabled
    val currentLanguage get() = general.currentLanguage
    val currentCurrency get() = general.currentCurrency
    val settingsInfo get() = general.settingsInfo
    val settingsInfoDescription get() = general.settingsInfoDescription
    val restartAppForChanges get() = general.restartAppForChanges
    val chinese get() = general.chinese
    val english get() = general.english
    val categoryManagement get() = general.categoryManagement
    val categoryManagementDescription get() = general.categoryManagementDescription
    val categoryAndTagManagement get() = general.categoryAndTagManagement
    val languageChineseLabel get() = general.languageChineseLabel
    val toggleOn get() = general.toggleOn
    val toggleOff get() = general.toggleOff
    val appUpdateDownload get() = general.appUpdateDownload
    val showUpdateDownloadProgress get() = general.showUpdateDownloadProgress
    val downloadingVersion get() = general.downloadingVersion
    val preparingDownload get() = general.preparingDownload
    val downloadFailedFileSave get() = general.downloadFailedFileSave
    val downloadCancelled get() = general.downloadCancelled
    val downloadFailedGeneric get() = general.downloadFailedGeneric
    val downloadingUpdate get() = general.downloadingUpdate
    val clickToInstallUpdate get() = general.clickToInstallUpdate

    val close get() = backup.close
    val enterBackupName get() = backup.enterBackupName
    val backupNamePlaceholder get() = backup.backupNamePlaceholder
    val localBackupVault get() = backup.localBackupVault
    val enableAutoBackup get() = backup.enableAutoBackup
    val autoBackupDescription get() = backup.autoBackupDescription
    val backupRetentionLimit get() = backup.backupRetentionLimit
    val backupRetentionDescription get() = backup.backupRetentionDescription
    val backupRetentionUnit get() = backup.backupRetentionUnit
    val backupThresholdDescription get() = backup.backupThresholdDescription
    val currentBackupStatus get() = backup.currentBackupStatus
    val latestBackupFile get() = backup.latestBackupFile
    val noBackupFound get() = backup.noBackupFound
    val createManualBackup get() = backup.createManualBackup
    val manualBackupSuccess get() = backup.manualBackupSuccess
    val clearAutoBackups get() = backup.clearAutoBackups
    val clearManualBackups get() = backup.clearManualBackups
    val backupsCleared get() = backup.backupsCleared
    val manualBackupsCleared get() = backup.manualBackupsCleared
    val backupVault get() = backup.backupVault
    val autoBackup get() = backup.autoBackup
    val manualBackup get() = backup.manualBackup
    val restore get() = backup.restore
    val delete get() = backup.delete
    val deleteBackupSuccess get() = backup.deleteBackupSuccess
    val noManualBackups get() = backup.noManualBackups
    val latestAutoBackup get() = backup.latestAutoBackup
    val latestManualBackup get() = backup.latestManualBackup
    val noAutoBackup get() = backup.noAutoBackup
    val noManualBackup get() = backup.noManualBackup
    val openManualBackupVault get() = backup.openManualBackupVault
    val infoLimitation get() = backup.infoLimitation
    val swipeDeleteConfirm get() = backup.swipeDeleteConfirm
    val swipeDeleteConfirmEnabled get() = backup.swipeDeleteConfirmEnabled
    val swipeDeleteConfirmDisabled get() = backup.swipeDeleteConfirmDisabled
    val defaultCategory get() = backup.defaultCategory
    val addCategory get() = backup.addCategory
    val categoryName get() = backup.categoryName
    val renameCategory get() = backup.renameCategory
    val newName get() = backup.newName
    val deleteCategory get() = backup.deleteCategory
    val deleteCategoryConfirm get() = backup.deleteCategoryConfirm

    val asset get() = feature.asset
    val assets get() = feature.assets
    val assetsDescription get() = feature.assetsDescription
    val netAssets get() = feature.netAssets
    val totalLiabilities get() = feature.totalLiabilities
    val addAsset get() = feature.addAsset
    val editAsset get() = feature.editAsset
    val deleteAsset get() = feature.deleteAsset
    val assetStatus get() = feature.assetStatus
    val none get() = feature.none
    val owned get() = feature.owned
    val notOwned get() = feature.notOwned
    val lost get() = feature.lost
    val inProgress get() = feature.inProgress
    val temporarilyWithMe get() = feature.temporarilyWithMe
    val temporarilyWithOthers get() = feature.temporarilyWithOthers
    val targetPerson get() = feature.targetPerson
    val targetAccount get() = feature.targetAccount
    val includeInTotal get() = feature.includeInTotal
    val isCompleted get() = feature.isCompleted
    val noAssets get() = feature.noAssets
    val assetRecord get() = feature.assetRecord
    val positiveAsset get() = feature.positiveAsset
    val negativeAsset get() = feature.negativeAsset
    val assetFlowCompleted get() = feature.assetFlowCompleted
    val assetFlowInProgress get() = feature.assetFlowInProgress
    val markComplete get() = feature.markComplete
    val markAsOwned get() = feature.markAsOwned
    val markAsNotOwned get() = feature.markAsNotOwned
    val markInProgress get() = feature.markInProgress
    val confirmDelete get() = feature.confirmDelete
    val selectCategory get() = feature.selectCategory
    val budget get() = feature.budget
    val showUnbudgetedSpend get() = feature.showUnbudgetedSpend
    val showBudgetedOnly get() = feature.showBudgetedOnly
    val budgetNotSet get() = feature.budgetNotSet
    val budgetSpent get() = feature.budgetSpent
    val budgetRemaining get() = feature.budgetRemaining
    val budgetExceeded get() = feature.budgetExceeded
    val budgetInvalidAmount get() = feature.budgetInvalidAmount
    val budgetZeroWarning get() = feature.budgetZeroWarning
    val budgetSubtitle get() = feature.budgetSubtitle
    val budgetsDescription get() = feature.budgetsDescription
    val financialArchive get() = feature.financialArchive
    val financialArchiveDescription get() = feature.financialArchiveDescription
    val openArchive get() = feature.openArchive
    val attachmentOverviewDescription get() = feature.attachmentOverviewDescription
    val itemName get() = feature.itemName
    val specification get() = feature.specification
    val quantity get() = feature.quantity
    val expenseCategory get() = feature.expenseCategory
    val assetCategory get() = feature.assetCategory
    val approvalCenter get() = feature.approvalCenter
    val approvalApplicant get() = feature.approvalApplicant
    val approvalApprover get() = feature.approvalApprover
    val approvalTodo get() = feature.approvalTodo
    val approvalHistory get() = feature.approvalHistory
    val approvalNewRequest get() = feature.approvalNewRequest
    val approvalBudgetAdjustment get() = feature.approvalBudgetAdjustment
    val approvalPurchaseBudget get() = feature.approvalPurchaseBudget
    val approvalPending get() = feature.approvalPending
    val approvalApproved get() = feature.approvalApproved
    val approvalRejected get() = feature.approvalRejected
    val approvalWithdrawn get() = feature.approvalWithdrawn
    val approvalReason get() = feature.approvalReason
    val approvalPurchaseDate get() = feature.approvalPurchaseDate
    val approvalSubmit get() = feature.approvalSubmit
    val approvalWithdraw get() = feature.approvalWithdraw
    val approvalResubmit get() = feature.approvalResubmit
    val approvalApprove get() = feature.approvalApprove
    val approvalReject get() = feature.approvalReject
    val approvalDecisionNote get() = feature.approvalDecisionNote
    val approvalCurrentBudget get() = feature.approvalCurrentBudget
    val approvalRemaining get() = feature.approvalRemaining
    val approvalNoRequests get() = feature.approvalNoRequests
    val approvalMissingCategory get() = feature.approvalMissingCategory
    val approvalMissingItemName get() = feature.approvalMissingItemName
    val approvalMissingReasonDetail get() = feature.approvalMissingReasonDetail
    val approvalInvalidAmount get() = feature.approvalInvalidAmount
    val approvalSubmitted get() = feature.approvalSubmitted
    val approvalView get() = feature.approvalView
    val approvalDetailTitle get() = feature.approvalDetailTitle
    val approvalRequestId get() = feature.approvalRequestId
    val approvalRequestType get() = feature.approvalRequestType
    val approvalCreatedAt get() = feature.approvalCreatedAt
    val approvalCurrentStatus get() = feature.approvalCurrentStatus
    val approvalDecidedAt get() = feature.approvalDecidedAt
    val approvalNotApplicable get() = feature.approvalNotApplicable
    val approvalPurchaseProgress get() = feature.approvalPurchaseProgress
    val approvalAwaitingPurchase get() = feature.approvalAwaitingPurchase
    val approvalPurchaseInProgress get() = feature.approvalPurchaseInProgress
    val approvalConfirmedOwned get() = feature.approvalConfirmedOwned
    val approvalNotStarted get() = feature.approvalNotStarted
    val approvalRelatedAsset get() = feature.approvalRelatedAsset
    val approvalRealTransaction get() = feature.approvalRealTransaction
    val approvalNotSet get() = feature.approvalNotSet
    val approvalMissingAssetCategory get() = feature.approvalMissingAssetCategory
    val approvalInvalidQuantity get() = feature.approvalInvalidQuantity
    val addAssetCategoryFirst get() = feature.addAssetCategoryFirst
    val enterValidAmount get() = feature.enterValidAmount
    val mergeSuccess get() = feature.mergeSuccess
    val mergeNoNewData get() = feature.mergeNoNewData
    val mergeParseFailed get() = feature.mergeParseFailed
    val cannotReadFile get() = feature.cannotReadFile
    val unrecognizedBillFormat get() = feature.unrecognizedBillFormat
    val noTransactionsToImport get() = feature.noTransactionsToImport
    val wechat get() = feature.wechat
    val alipay get() = feature.alipay
    val excludedRefundInfo get() = feature.excludedRefundInfo
    val billImportSuccess get() = feature.billImportSuccess
    val importFailed get() = feature.importFailed
    val clearAllTransactionsButton get() = feature.clearAllTransactionsButton
    val dangerWarning get() = feature.dangerWarning
    val clearAllDataWarningMessage get() = feature.clearAllDataWarningMessage
    val allRecordsCleared get() = feature.allRecordsCleared
    val clearAnyway get() = feature.clearAnyway
    val clearAllData get() = feature.clearAllData
    val clearAllDataDescription get() = feature.clearAllDataDescription
    val clearAllDataWarning get() = feature.clearAllDataWarning
    val clearAllDataConfirmTitle get() = feature.clearAllDataConfirmTitle
    val clearAllDataWillDelete get() = feature.clearAllDataWillDelete
    val clearAllDataTransactions get() = feature.clearAllDataTransactions
    val clearAllDataAssets get() = feature.clearAllDataAssets
    val clearAllDataAttachments get() = feature.clearAllDataAttachments
    val clearAllDataCannotUndo get() = feature.clearAllDataCannotUndo
    val clearAllDataConfirm get() = feature.clearAllDataConfirm
    val clearAllDataSuccess get() = feature.clearAllDataSuccess
    val clearTransactions get() = feature.clearTransactions
    val clearAssets get() = feature.clearAssets
    val clearTransactionsConfirmTitle get() = feature.clearTransactionsConfirmTitle
    val clearTransactionsConfirm get() = feature.clearTransactionsConfirm
    val clearTransactionsSuccess get() = feature.clearTransactionsSuccess
    val clearAssetsConfirmTitle get() = feature.clearAssetsConfirmTitle
    val clearAssetsConfirm get() = feature.clearAssetsConfirm
    val clearAssetsSuccess get() = feature.clearAssetsSuccess
    val reimportBill get() = feature.reimportBill
    val reimportSuccess get() = feature.reimportSuccess
    val noNewRecordsToImport get() = feature.noNewRecordsToImport
    val thirdPartyBillFileManagement get() = feature.thirdPartyBillFileManagement
    val noImportedBillFiles get() = feature.noImportedBillFiles
    val wechatBill get() = feature.wechatBill
    val alipayBill get() = feature.alipayBill
    val unknownBill get() = feature.unknownBill
    val archiveExportSuccess get() = feature.archiveExportSuccess
    val archiveExportFailed get() = feature.archiveExportFailed

    val helpAboutProject get() = help.helpAboutProject
    val helpAboutDescription1 get() = help.helpAboutDescription1
    val helpAboutDescription2 get() = help.helpAboutDescription2
    val helpHomeFeatures get() = help.helpHomeFeatures
    val helpBalanceCard get() = help.helpBalanceCard
    val helpBalanceCardDetail1 get() = help.helpBalanceCardDetail1
    val helpBalanceCardDetail2 get() = help.helpBalanceCardDetail2
    val helpBalanceCardDetail3 get() = help.helpBalanceCardDetail3
    val helpSearchFeature get() = help.helpSearchFeature
    val helpSearchDetail1 get() = help.helpSearchDetail1
    val helpSearchDetail2 get() = help.helpSearchDetail2
    val helpSearchDetail3 get() = help.helpSearchDetail3
    val helpTransactionListOps get() = help.helpTransactionListOps
    val helpTxTapDetail get() = help.helpTxTapDetail
    val helpTxSwipeDetail get() = help.helpTxSwipeDetail
    val helpTxLongPressDetail get() = help.helpTxLongPressDetail
    val helpTxBatchDelete get() = help.helpTxBatchDelete
    val helpAddTransaction get() = help.helpAddTransaction
    val helpAddTxDetail1 get() = help.helpAddTxDetail1
    val helpAddTxDetail2 get() = help.helpAddTxDetail2
    val helpAddTxDetail3 get() = help.helpAddTxDetail3
    val helpAddTxDetail4 get() = help.helpAddTxDetail4
    val helpStatistics get() = help.helpStatistics
    val helpTimeRangeSelection get() = help.helpTimeRangeSelection
    val helpTimeRangeDay get() = help.helpTimeRangeDay
    val helpTimeRangeWeek get() = help.helpTimeRangeWeek
    val helpTimeRangeMonth get() = help.helpTimeRangeMonth
    val helpTimeRangeYear get() = help.helpTimeRangeYear
    val helpTimeRangeCustom get() = help.helpTimeRangeCustom
    val helpChartDisplay get() = help.helpChartDisplay
    val helpChartLine get() = help.helpChartLine
    val helpChartPie get() = help.helpChartPie
    val helpChartRanking get() = help.helpChartRanking
    val helpCategoryFilter get() = help.helpCategoryFilter
    val helpCategoryFilterDetail1 get() = help.helpCategoryFilterDetail1
    val helpCategoryFilterDetail2 get() = help.helpCategoryFilterDetail2
    val helpCategoryFilterDetail3 get() = help.helpCategoryFilterDetail3
    val helpAssetManagement get() = help.helpAssetManagement
    val helpAssetStatistics get() = help.helpAssetStatistics
    val helpAssetNetWorth get() = help.helpAssetNetWorth
    val helpCurrentAssets get() = help.helpCurrentAssets
    val helpCurrentLiabilities get() = help.helpCurrentLiabilities
    val helpTransactionBalance get() = help.helpTransactionBalance
    val helpAddAssetRecord get() = help.helpAddAssetRecord
    val helpAddAssetDetail1 get() = help.helpAddAssetDetail1
    val helpAddAssetDetail2 get() = help.helpAddAssetDetail2
    val helpAddAssetDetail3 get() = help.helpAddAssetDetail3
    val helpAddAssetDetail4 get() = help.helpAddAssetDetail4
    val helpStatusExplanation get() = help.helpStatusExplanation
    val helpStatusInProgress get() = help.helpStatusInProgress
    val helpStatusCompleted get() = help.helpStatusCompleted
    val helpStatusCancelled get() = help.helpStatusCancelled
    val helpDataManagement get() = help.helpDataManagement
    val helpExportData get() = help.helpExportData
    val helpExportDetail1 get() = help.helpExportDetail1
    val helpExportDetail2 get() = help.helpExportDetail2
    val helpExportDetail3 get() = help.helpExportDetail3
    val helpImportData get() = help.helpImportData
    val helpImportDetail1 get() = help.helpImportDetail1
    val helpImportDetail2 get() = help.helpImportDetail2
    val helpImportDetail3 get() = help.helpImportDetail3
    val helpBillImport get() = help.helpBillImport
    val helpBillImportDetail1 get() = help.helpBillImportDetail1
    val helpBillImportDetail2 get() = help.helpBillImportDetail2
    val helpBillImportDetail3 get() = help.helpBillImportDetail3
    val helpBillImportDetail4 get() = help.helpBillImportDetail4
    val helpBillImportDetail5 get() = help.helpBillImportDetail5
    val helpHowToExportBill get() = help.helpHowToExportBill
    val helpExportBillWechat get() = help.helpExportBillWechat
    val helpExportBillAlipay get() = help.helpExportBillAlipay
    val helpExportBillFormat get() = help.helpExportBillFormat
    val helpExportBillImport get() = help.helpExportBillImport
    val helpAutoBackup get() = help.helpAutoBackup
    val helpAutoBackupDetail1 get() = help.helpAutoBackupDetail1
    val helpAutoBackupDetail2 get() = help.helpAutoBackupDetail2
    val helpAutoBackupDetail3 get() = help.helpAutoBackupDetail3
    val helpAutoBackupDetail4 get() = help.helpAutoBackupDetail4
    val helpCategoryManagement get() = help.helpCategoryManagement
    val helpCategoryTypes get() = help.helpCategoryTypes
    val helpExpenseCategories get() = help.helpExpenseCategories
    val helpIncomeCategories get() = help.helpIncomeCategories
    val helpAssetCategories get() = help.helpAssetCategories
    val helpManagementOps get() = help.helpManagementOps
    val helpAddCategory get() = help.helpAddCategory
    val helpRenameCategory get() = help.helpRenameCategory
    val helpDeleteCategory get() = help.helpDeleteCategory
    val helpPresetCategoryNoDelete get() = help.helpPresetCategoryNoDelete
    val helpPersonalization get() = help.helpPersonalization
    val helpAppearanceSettings get() = help.helpAppearanceSettings
    val helpDarkMode get() = help.helpDarkMode
    val helpLightMode get() = help.helpLightMode
    val helpLanguage get() = help.helpLanguage
    val helpCurrency get() = help.helpCurrency
    val helpNotes get() = help.helpNotes
    val helpRestartRequired get() = help.helpRestartRequired
    val helpOldSettingsBeforeRestart get() = help.helpOldSettingsBeforeRestart
    val helpTips get() = help.helpTips
    val helpDailyTips get() = help.helpDailyTips
    val helpDailyTip1 get() = help.helpDailyTip1
    val helpDailyTip2 get() = help.helpDailyTip2
    val helpDailyTip3 get() = help.helpDailyTip3
    val helpDataSafety get() = help.helpDataSafety
    val helpDataSafetyTip1 get() = help.helpDataSafetyTip1
    val helpDataSafetyTip2 get() = help.helpDataSafetyTip2
    val helpDataSafetyTip3 get() = help.helpDataSafetyTip3
    val helpBillImportTips get() = help.helpBillImportTips
    val helpBillImportTip1 get() = help.helpBillImportTip1
    val helpBillImportTip2 get() = help.helpBillImportTip2
    val helpBillImportTip3 get() = help.helpBillImportTip3
    val helpFAQ get() = help.helpFAQ
    val helpFAQRecover get() = help.helpFAQRecover
    val helpFAQRecoverDetail1 get() = help.helpFAQRecoverDetail1
    val helpFAQRecoverDetail2 get() = help.helpFAQRecoverDetail2
    val helpFAQPartialImport get() = help.helpFAQPartialImport
    val helpFAQPartialImportDetail1 get() = help.helpFAQPartialImportDetail1
    val helpFAQPartialImportDetail2 get() = help.helpFAQPartialImportDetail2
    val helpFAQPartialImportDetail3 get() = help.helpFAQPartialImportDetail3
    val helpFAQSync get() = help.helpFAQSync
    val helpFAQSyncDetail1 get() = help.helpFAQSyncDetail1
    val helpFAQSyncDetail2 get() = help.helpFAQSyncDetail2
    val helpFAQSyncDetail3 get() = help.helpFAQSyncDetail3
    val helpHomeTimeRange get() = help.helpHomeTimeRange
    val helpHomeTimeRangeDetail1 get() = help.helpHomeTimeRangeDetail1
    val helpHomeTimeRangeDetail2 get() = help.helpHomeTimeRangeDetail2
    val helpHomeTimeRangeDetail3 get() = help.helpHomeTimeRangeDetail3
    val helpHomeFilter get() = help.helpHomeFilter
    val helpHomeFilterDetail1 get() = help.helpHomeFilterDetail1
    val helpHomeFilterDetail2 get() = help.helpHomeFilterDetail2
    val helpHomeFilterDetail3 get() = help.helpHomeFilterDetail3
    val helpBudgetPeriods get() = help.helpBudgetPeriods
    val helpBudgetPeriodsDetail1 get() = help.helpBudgetPeriodsDetail1
    val helpBudgetPeriodsDetail2 get() = help.helpBudgetPeriodsDetail2
    val helpBudgetPeriodsDetail3 get() = help.helpBudgetPeriodsDetail3
    val helpBillPreview get() = help.helpBillPreview
    val helpBillPreviewDetail1 get() = help.helpBillPreviewDetail1
    val helpBillPreviewDetail2 get() = help.helpBillPreviewDetail2
    val helpAssetAttachment get() = help.helpAssetAttachment
    val helpAssetAttachmentDetail1 get() = help.helpAssetAttachmentDetail1
    val helpAssetAttachmentDetail2 get() = help.helpAssetAttachmentDetail2
}

fun Context.getAppStrings(): AppStrings {
    val locale = resources.configuration.locales[0]
    return if (locale.language == "zh") ZhStrings else EnStrings
}

val LocalAppStrings = androidx.compose.runtime.compositionLocalOf { EnStrings }

val EnStrings = AppStrings(
    general = GeneralStrings(
        home = "Home",
        statistics = "Statistics",
        categoryStatistics = "Category Statistics",
        settings = "Settings",
        dataManagement = "Data Management",
        quickSettings = "Quick Settings",
        customizeExperience = "Customize your experience",
        dataManagementDescription = "Import, export and backup your data",
        generalSettings = "General Settings",
        generalSettingsDescription = "Theme, language and currency preferences",
        about = "About",
        aboutDescription = "Version info and help tutorial",
        totalAssets = "Total Assets",
        totalBalance = "Total Balance",
        thisMonth = "This Month",
        income = "Income",
        expense = "Expense",
        balanceOverall = "Overall",
        startDate = "Start",
        clearFilter = "Clear Filter",
        endDate = "End",
        recentTransactions = "Recent Transactions",
        addTransaction = "Add Transaction",
        editTransaction = "Edit Transaction",
        deleteTransaction = "Delete Transaction",
        deleteConfirm = "Are you sure you want to delete this transaction?",
        deleteConfirmTitle = "Delete Transaction?",
        deleteConfirmMessage = "This action cannot be undone. The transaction will be permanently deleted.",
        amount = "Amount",
        date = "Date",
        category = "Category",
        note = "Note",
        save = "Save",
        daily = "Daily",
        weekly = "Weekly",
        monthly = "Monthly",
        semiAnnual = "Semi-Annual",
        yearly = "Yearly",
        custom = "Custom",
        totalIncome = "Total Income",
        totalExpense = "Total Expense",
        total = "Total",
        categoryRanking = "Category Ranking",
        noTransactions = "No transactions found for this period.",
        selectRange = "Select Range",
        manualDataManagement = "Manual Data Management",
        uploadBackup = "Import ZIP Backup",
        exportAll = "Export All Data to ZIP",
        darkMode = "Dark Mode",
        language = "Language",
        currencySymbol = "Currency Symbol",
        newCategory = "New Category",
        name = "Name",
        nameEmptyError = "Name cannot be empty",
        nameExistsError = "Category already exists in this type",
        add = "Add",
        cancel = "Cancel",
        ok = "OK",
        change = "Change",
        other = "Other",
        hideAmount = "Hide Amount",
        showAmount = "Show Amount",
        monthlyBalance = "Monthly Balance",
        navigate = "Navigate",
        back = "Back",
        billFiles = "Bill Files",
        transactions = "transactions",
        search = "Search",
        searchResults = "Search Results",
        searchHint = "Search by note or category",
        noSearchResults = "No results found for",
        filter = "Filter",
        all = "All",
        sortByTime = "Sort by Time",
        sortByAmount = "Sort by Amount",
        sortSettings = "Sort Settings",
        ascending = "Ascending",
        descending = "Descending",
        timeRange = "Time Range",
        dateRange = "Date Range",
        sort = "Sort",
        categoryFilter = "Category",
        allCategories = "All Categories",
        timeDescending = "Time (Newest First)",
        timeAscending = "Time (Oldest First)",
        amountDescending = "Amount (High to Low)",
        amountAscending = "Amount (Low to High)",
        attachments = "Attachments",
        addAttachment = "Add Attachment",
        removeAttachment = "Remove",
        noAttachments = "No attachments",
        selectFile = "Select File",
        fileSizeTooLarge = "File size exceeds limit",
        checkUpdate = "Check Update",
        currentVersion = "Current Version",
        downloadNow = "Download",
        cancelDownload = "Cancel Download",
        installNow = "Install",
        downloadComplete = "Download Complete",
        alreadyLatest = "Already Latest Version",
        checkUpdateFailed = "Check Update Failed",
        downloading = "Downloading...",
        downloadHint = "Please wait while downloading",
        downloadFailed = "Download Failed",
        openInBrowser = "Open in Browser",
        updateContents = "Update Contents",
        feedback = "Feedback",
        feedbackDescription = "Report issues or suggest improvements",
        problemDescription = "Problem Description",
        problemDescriptionHint = "Please describe your issue or suggestion...",
        contactInfo = "Contact Info",
        contactInfoHint = "Email or phone (optional)",
        sendFeedback = "Send",
        feedbackSent = "Feedback sent successfully",
        version = "Version 1.0.0",
        helpTutorial = "Help & Tutorial",
        helpTutorialShort = "View complete usage guide and feature descriptions",
        helpTutorialDescription = "",
        github = "GitHub",
        githubDescription = "Follow our open source project",
        contactAuthor = "Contact Author",
        contactAuthorDescription = "Feel free to contact us if you have questions or suggestions",
        poweredBy = "Powered by",
        authorName = "Ricky Miao",
        expenseRatio = "Expense Ratio",
        incomeRatio = "Income Ratio",
        overallRatio = "Overall Ratio",
        thirdPartyBillImport = "Third-party Bill Import",
        thirdPartyBillImportDescription = "Support WeChat and Alipay bill import (Excel/CSV)",
        importWeChatBill = "Import WeChat Bill",
        importAlipayBill = "Import Alipay Bill",
        importWeChatAlipayBill = "Import WeChat/Alipay Bill",
        manageImportedBills = "Manage Imported Bills",
        selected = "selected",
        customizeAppExperience = "Customize your app experience",
        darkThemeEnabled = "Dark theme is enabled",
        lightThemeEnabled = "Light theme is enabled",
        currentLanguage = "Current language",
        currentCurrency = "Current currency",
        settingsInfo = "Settings Info",
        settingsInfoDescription = "Restart the app for language and currency changes to take effect",
        restartAppForChanges = "Restart the app for the changes to take effect",
        chinese = "Chinese",
        english = "English",
        categoryManagement = "Category Management",
        categoryManagementDescription = "Unified management of income and expense category information",
        categoryAndTagManagement = "Category & Tag Management",
        languageChineseLabel = "Chinese",
        toggleOn = "On",
        toggleOff = "Off",
        appUpdateDownload = "App Update Download",
        showUpdateDownloadProgress = "Show update download progress",
        downloadingVersion = "Downloading version %s",
        preparingDownload = "Preparing to download...",
        downloadFailedFileSave = "Failed to save the downloaded file",
        downloadCancelled = "Download cancelled",
        downloadFailedGeneric = "Download failed: %s - %s",
        downloadingUpdate = "Downloading update...",
        clickToInstallUpdate = "Tap to install update"
    ),
    backup = BackupStrings(
        close = "Close",
        enterBackupName = "Enter a name for this backup:",
        backupNamePlaceholder = "My Backup",
        localBackupVault = "Local Auto Backup Vault",
        enableAutoBackup = "Enable Auto Backup",
        autoBackupDescription = "Automatically save to local sandbox on any add, delete, or modify operation",
        backupRetentionLimit = "Backup Retention Limit",
        backupRetentionDescription = "New backup retention limit",
        backupRetentionUnit = " copies",
        backupThresholdDescription = "Automatically delete oldest backups when threshold is exceeded",
        currentBackupStatus = "Current Backup Status",
        latestBackupFile = "Latest backup file: ",
        noBackupFound = "No available backup file found",
        createManualBackup = "Create Manual Backup Now",
        manualBackupSuccess = "Manual backup created successfully!",
        clearAutoBackups = "Clear Auto Backups",
        clearManualBackups = "Clear Manual Backups",
        backupsCleared = "Auto backup pool has been forcibly cleared",
        manualBackupsCleared = "Manual backup pool has been forcibly cleared",
        backupVault = "Backup Vault",
        autoBackup = "Auto",
        manualBackup = "Manual",
        restore = "Restore",
        delete = "Delete",
        deleteBackupSuccess = "Successfully deleted the selected backup",
        noManualBackups = "You haven't created any manual backups yet.",
        latestAutoBackup = "Latest Auto: ",
        latestManualBackup = "Latest Manual: ",
        noAutoBackup = "No auto backup",
        noManualBackup = "No manual backup",
        openManualBackupVault = "Open Private Manual Backup Vault",
        infoLimitation = "ZIP backup contains transactions, assets, and attachments. Import to merge data with existing records.",
        swipeDeleteConfirm = "Swipe Delete Confirmation",
        swipeDeleteConfirmEnabled = "Swipe to delete directly triggers confirmation",
        swipeDeleteConfirmDisabled = "Click delete button after swipe to confirm",
        defaultCategory = "Default category",
        addCategory = "Add Category",
        categoryName = "Category Name",
        renameCategory = "Rename Category",
        newName = "New Name",
        deleteCategory = "Delete Category",
        deleteCategoryConfirm = "Are you sure you want to delete custom category \"{name}\"? This action cannot be undone.",
        appName = "AccountKeeper",
        appSubtitle = "Manage your finances"
    ),
    feature = FeatureStrings(
        asset = "Asset",
        assets = "Assets",
        assetsDescription = "Manage your assets and liabilities",
        netAssets = "Net Assets",
        totalLiabilities = "Total Liabilities",
        addAsset = "Add Asset",
        editAsset = "Edit Asset Record",
        deleteAsset = "Delete Asset",
        assetStatus = "Asset Status",
        none = "Not Selected",
        owned = "Owned",
        notOwned = "Not Owned",
        lost = "Lost",
        inProgress = "In Progress",
        temporarilyWithMe = "Temporarily With Me",
        temporarilyWithOthers = "Temporarily With Others",
        targetPerson = "Target Person",
        targetAccount = "Target Account",
        includeInTotal = "Include in Total",
        isCompleted = "Completed",
        noAssets = "No asset records found.",
        assetRecord = "Asset Record",
        positiveAsset = "Positive Asset",
        negativeAsset = "Negative Asset",
        assetFlowCompleted = "Completed",
        assetFlowInProgress = "In Progress",
        markComplete = "Mark Complete",
        markAsOwned = "Acquire Asset",
        markAsNotOwned = "Remove Liability",
        markInProgress = "Change to In Progress",
        confirmDelete = "Confirm delete this asset?",
        selectCategory = "Select Category",
        budget = "Budget",
        showUnbudgetedSpend = "Show Unbudgeted Spend",
        showBudgetedOnly = "Show Budgeted Only",
        budgetNotSet = "Budget not set",
        budgetSpent = "Spent:",
        budgetRemaining = "Remaining:",
        budgetExceeded = "Exceeded:",
        budgetInvalidAmount = "Please enter a valid amount",
        budgetZeroWarning = "Budget amount cannot be zero",
        budgetSubtitle = "Budget Management",
        financialArchive = "Financial Archive",
        financialArchiveDescription = "Export or import a complete ZIP archive with business data, settings, attachments, and bill files.",
        openArchive = "Open Archive",
        attachmentOverviewDescription = "Review all linked files across transactions, assets, approvals, and bills.",
        itemName = "Item Name",
        specification = "Specification",
        quantity = "Quantity",
        expenseCategory = "Expense Category",
        assetCategory = "Asset Category",
        budgetsDescription = "Track and manage your budgets",
        addAssetCategoryFirst = "Please add an asset category first",
        enterValidAmount = "Please enter a valid amount",
        mergeSuccess = "Successfully imported %d new records",
        mergeNoNewData = "No new records to import",
        mergeParseFailed = "Parse failed: %s",
        cannotReadFile = "Cannot read the selected file",
        unrecognizedBillFormat = "Unrecognized bill format",
        noTransactionsToImport = "No transactions to import",
        wechat = "WeChat",
        alipay = "Alipay",
        excludedRefundInfo = " (%d refund transactions excluded)",
        billImportSuccess = "%s bill import: %d transactions imported%s",
        importFailed = "Import failed: %s",
        clearAllTransactionsButton = "Clear All Transactions",
        dangerWarning = "Danger Warning",
        clearAllDataWarningMessage = "This will permanently delete all transactions and assets. Export a backup first is recommended.",
        allRecordsCleared = "All records have been cleared",
        clearAnyway = "Clear Anyway",
        clearAllData = "Clear All Data",
        clearAllDataDescription = "Delete all transactions and assets",
        clearAllDataWarning = "This will permanently delete all transactions and assets. Export a backup first is recommended.",
        clearAllDataConfirmTitle = "Clear All Data?",
        clearAllDataWillDelete = "This will permanently delete:",
        clearAllDataTransactions = "All transactions",
        clearAllDataAssets = "All assets",
        clearAllDataAttachments = "Related attachments",
        clearAllDataCannotUndo = "This action cannot be undone!",
        clearAllDataConfirm = "Confirm Clear",
        clearAllDataSuccess = "All data has been cleared",
        clearTransactions = "Clear Transactions",
        clearAssets = "Clear Assets",
        clearTransactionsConfirmTitle = "Clear All Transactions?",
        clearTransactionsConfirm = "This will delete all transaction records. This action cannot be undone!",
        clearTransactionsSuccess = "All transactions have been cleared",
        clearAssetsConfirmTitle = "Clear All Assets?",
        clearAssetsConfirm = "This will delete all asset records and attachments. This action cannot be undone!",
        clearAssetsSuccess = "All assets have been cleared",
        reimportBill = "Reimport",
        reimportSuccess = "Successfully imported %d new records",
        noNewRecordsToImport = "No new records to import",
        thirdPartyBillFileManagement = "Third-party Bill File Management",
        noImportedBillFiles = "No imported bill files",
        wechatBill = "WeChat Bill",
        alipayBill = "Alipay Bill",
        unknownBill = "Unknown Bill",
        archiveExportSuccess = "Archive exported successfully",
        archiveExportFailed = "Archive export failed"
    ),
    help = HelpStrings(
        helpAboutProject = "About Project",
        helpAboutDescription1 = "AccountKeeper is an open-source personal finance management app.",
        helpAboutDescription2 = "It helps you easily record and manage your daily expenses and income.",
        helpHomeFeatures = "Home Screen Features",
        helpBalanceCard = "Balance Card",
        helpBalanceCardDetail1 = "Total Balance: Current net balance (Income - Expenses)",
        helpBalanceCardDetail2 = "Total Income: Total income within the selected time range",
        helpBalanceCardDetail3 = "Total Expenses: Total expenses within the selected time range",
        helpSearchFeature = "Search Feature",
        helpSearchDetail1 = "Search by note or category name",
        helpSearchDetail2 = "Results update in real-time as you type",
        helpSearchDetail3 = "Search across all transactions",
        helpTransactionListOps = "Transaction List Operations",
        helpTxTapDetail = "Tap to edit a transaction",
        helpTxSwipeDetail = "Swipe left to delete a transaction",
        helpTxLongPressDetail = "Long press to enter selection mode for batch operations",
        helpTxBatchDelete = "Select multiple transactions and batch delete them",
        helpAddTransaction = "Add Transaction",
        helpAddTxDetail1 = "Enter amount using the number keypad",
        helpAddTxDetail2 = "Select income or expense type",
        helpAddTxDetail3 = "Choose a category",
        helpAddTxDetail4 = "Add optional note and save",
        helpStatistics = "Statistics Analysis",
        helpTimeRangeSelection = "Time Range Selection",
        helpTimeRangeDay = "Day: View single day data",
        helpTimeRangeWeek = "Week: View this week's data (Mon-Sun)",
        helpTimeRangeMonth = "Month: View this month's data",
        helpTimeRangeYear = "Year: View full year data",
        helpTimeRangeCustom = "Custom: Select start and end date",
        helpChartDisplay = "Chart Display",
        helpChartLine = "Line chart showing trends over time",
        helpChartPie = "Pie chart showing category proportions",
        helpChartRanking = "Category ranking by amount",
        helpCategoryFilter = "Category Filter",
        helpCategoryFilterDetail1 = "Filter by expense or income category",
        helpCategoryFilterDetail2 = "View specific category transactions",
        helpCategoryFilterDetail3 = "Combine with time range for detailed analysis",
        helpAssetManagement = "Asset Management",
        helpAssetStatistics = "Asset Statistics",
        helpAssetNetWorth = "Net Worth: Total assets minus total liabilities",
        helpCurrentAssets = "Current Assets: Assets you currently own",
        helpCurrentLiabilities = "Current Liabilities: Debts and obligations",
        helpTransactionBalance = "Transaction Balance: Net from all transactions",
        helpAddAssetRecord = "Add Asset Record",
        helpAddAssetDetail1 = "Select asset type and category",
        helpAddAssetDetail2 = "Enter asset name and amount",
        helpAddAssetDetail3 = "Set asset status (Owned, In Progress, etc.)",
        helpAddAssetDetail4 = "Add optional target person or account",
        helpStatusExplanation = "Status Explanation",
        helpStatusInProgress = "In Progress: Asset flow is ongoing",
        helpStatusCompleted = "Completed: Asset has been acquired or removed",
        helpStatusCancelled = "Cancelled: Asset flow was cancelled",
        helpDataManagement = "Data Management",
        helpExportData = "Export Data",
        helpExportDetail1 = "Export all data as ZIP backup",
        helpExportDetail2 = "Export CSV for spreadsheet analysis",
        helpExportDetail3 = "Backups include transactions, assets, and attachments",
        helpImportData = "Import Data",
        helpImportDetail1 = "Import ZIP backup to restore data",
        helpImportDetail2 = "Import CSV to merge with existing data",
        helpImportDetail3 = "Duplicate IDs will be skipped during import",
        helpBillImport = "Bill Import",
        helpBillImportDetail1 = "Import WeChat Pay bills",
        helpBillImportDetail2 = "Import Alipay bills",
        helpBillImportDetail3 = "Auto-detect bill type and parse transactions",
        helpBillImportDetail4 = "Refund transactions recognized as income",
        helpBillImportDetail5 = "Duplicate IDs will not be imported again",
        helpHowToExportBill = "How to Export Bills",
        helpExportBillWechat = "WeChat: Me -> Services -> Wallet -> Bill -> Export",
        helpExportBillAlipay = "Alipay: My -> Top-right menu -> Statement -> Export",
        helpExportBillFormat = "Export as CSV format",
        helpExportBillImport = "Then import the CSV file in this app",
        helpAutoBackup = "Auto Backup",
        helpAutoBackupDetail1 = "Automatically backup on every add, delete, or modify",
        helpAutoBackupDetail2 = "No manual operation required",
        helpAutoBackupDetail3 = "Set retention limit (5-50 backups)",
        helpAutoBackupDetail4 = "Oldest backups auto-deleted when limit exceeded",
        helpCategoryManagement = "Category Management",
        helpCategoryTypes = "Category Types",
        helpExpenseCategories = "Expense: Dining, Transport, Shopping, etc.",
        helpIncomeCategories = "Income: Salary, Bonus, Investment, etc.",
        helpAssetCategories = "Asset: Positive (owned) and Negative (owed) types",
        helpManagementOps = "Management Operations",
        helpAddCategory = "Add custom category with + button",
        helpRenameCategory = "Rename with edit icon",
        helpDeleteCategory = "Delete with trash icon (custom categories only)",
        helpPresetCategoryNoDelete = "Default categories cannot be deleted",
        helpPersonalization = "Personalization",
        helpAppearanceSettings = "Appearance Settings",
        helpDarkMode = "Dark Mode: For nighttime use",
        helpLightMode = "Light Mode: For daytime use",
        helpLanguage = "Language: Chinese or English",
        helpCurrency = "Currency: Multiple currency symbols supported",
        helpNotes = "Important Notes",
        helpRestartRequired = "Language and currency changes require app restart",
        helpOldSettingsBeforeRestart = "Old settings remain until restart",
        helpTips = "Usage Tips",
        helpDailyTips = "Daily Bookkeeping Habits",
        helpDailyTip1 = "Record expenses immediately to avoid forgetting",
        helpDailyTip2 = "Add detailed notes to recall transaction details",
        helpDailyTip3 = "Use categorization to better manage finances",
        helpDataSafety = "Data Safety",
        helpDataSafetyTip1 = "Create manual backup once a week",
        helpDataSafetyTip2 = "Enable auto backup for every operation",
        helpDataSafetyTip3 = "Use CSV export to sync data between devices",
        helpBillImportTips = "Bill Import Tips",
        helpBillImportTip1 = "Import bills once a month regularly",
        helpBillImportTip2 = "Verify data after import",
        helpBillImportTip3 = "Add useful notes for bill transactions",
        helpFAQ = "FAQ",
        helpFAQRecover = "How to recover deleted transactions?",
        helpFAQRecoverDetail1 = "Deleted transactions cannot be directly recovered",
        helpFAQRecoverDetail2 = "If you have a backup, restore the backup file",
        helpFAQPartialImport = "Why some transactions were not imported?",
        helpFAQPartialImportDetail1 = "IDs may be duplicate or amounts are zero",
        helpFAQPartialImportDetail2 = "System automatically skips these transactions",
        helpFAQPartialImportDetail3 = "Check original bill for details",
        helpFAQSync = "How to sync data between devices?",
        helpFAQSyncDetail1 = "Export CSV file on one device",
        helpFAQSyncDetail2 = "Import on other devices",
        helpFAQSyncDetail3 = "Duplicate IDs will be skipped",
        helpHomeTimeRange = "Home Time Range Toggle",
        helpHomeTimeRangeDetail1 = "Month: View current month income and expenses",
        helpHomeTimeRangeDetail2 = "Year: View current year income and expenses",
        helpHomeTimeRangeDetail3 = "All: View all-time income and expenses",
        helpHomeFilter = "Home Transaction Filter",
        helpHomeFilterDetail1 = "Filter by date range, category, or sort order",
        helpHomeFilterDetail2 = "Active filters shown as chips above the list",
        helpHomeFilterDetail3 = "Tap chip to clear a specific filter",
        helpBudgetPeriods = "Budget Period Options",
        helpBudgetPeriodsDetail1 = "Monthly: Standard monthly budget",
        helpBudgetPeriodsDetail2 = "Semi-Annual: 6-month budget period",
        helpBudgetPeriodsDetail3 = "Annual: Full year budget period",
        helpBillPreview = "Bill File Preview",
        helpBillPreviewDetail1 = "Tap a bill file card to preview parsed transactions",
        helpBillPreviewDetail2 = "Supports WeChat and Alipay bill formats",
        helpAssetAttachment = "Asset Attachments",
        helpAssetAttachmentDetail1 = "Attach images, Excel, or CSV files to assets",
        helpAssetAttachmentDetail2 = "Tap attachment icon to preview file content"
    )
)

val ZhStrings = AppStrings(
    general = GeneralStrings(
        home = "首页",
        statistics = "统计",
        categoryStatistics = "分类统计",
        settings = "设置",
        dataManagement = "数据管理",
        quickSettings = "快速设置",
        customizeExperience = "自定义您的体验",
        dataManagementDescription = "导入、导出和备份数据",
        generalSettings = "个性化设置",
        generalSettingsDescription = "主题、语言和货币偏好",
        about = "关于",
        aboutDescription = "版本信息和帮助教程",
        totalAssets = "总资产",
        totalBalance = "总余额",
        thisMonth = "本月",
        income = "收入",
        expense = "支出",
        balanceOverall = "综合",
        startDate = "开始",
        clearFilter = "清除筛选",
        endDate = "结束",
        recentTransactions = "近期交易",
        addTransaction = "记录交易",
        editTransaction = "修改交易",
        deleteTransaction = "删除交易",
        deleteConfirm = "确定要删除这条交易吗？",
        deleteConfirmTitle = "删除交易？",
        deleteConfirmMessage = "此操作不可撤销。交易将被永久删除。",
        amount = "金额",
        date = "日期",
        category = "分类",
        note = "备注",
        save = "保存",
        daily = "日",
        weekly = "周",
        monthly = "月",
        semiAnnual = "半年",
        yearly = "年",
        custom = "自定义",
        totalIncome = "总收入",
        totalExpense = "总支出",
        total = "总计",
        categoryRanking = "分类开销排行",
        noTransactions = "该时段内没有交易记录。",
        selectRange = "选择区间",
        manualDataManagement = "本地数据归档",
        uploadBackup = "导入 ZIP 备份",
        exportAll = "导出全量数据为 ZIP",
        darkMode = "深色模式",
        language = "界面语言",
        currencySymbol = "货币符号",
        newCategory = "自定义分类",
        name = "分类名称",
        nameEmptyError = "分类名不能为空",
        nameExistsError = "该分类已经存在",
        add = "添加",
        cancel = "取消",
        ok = "确定",
        change = "修改",
        other = "其他",
        hideAmount = "隐藏金额",
        showAmount = "显示金额",
        monthlyBalance = "月度余额",
        navigate = "导航",
        back = "返回",
        billFiles = "账单文件",
        transactions = "条记录",
        search = "搜索",
        searchResults = "搜索结果",
        searchHint = "搜索备注或分类",
        noSearchResults = "未找到相关记录",
        filter = "筛选",
        all = "全部",
        sortByTime = "按时间排序",
        sortByAmount = "按金额排序",
        sortSettings = "排序设置",
        ascending = "升序",
        descending = "降序",
        timeRange = "时间范围",
        dateRange = "日期范围",
        sort = "排序",
        categoryFilter = "分类",
        allCategories = "全部分类",
        timeDescending = "时间（最新优先）",
        timeAscending = "时间（最早优先）",
        amountDescending = "金额（从高到低）",
        amountAscending = "金额（从低到高）",
        attachments = "附件",
        addAttachment = "添加附件",
        removeAttachment = "移除",
        noAttachments = "暂无附件",
        selectFile = "选择文件",
        fileSizeTooLarge = "文件大小超出限制",
        checkUpdate = "检查更新",
        currentVersion = "当前版本",
        downloadNow = "立即下载",
        cancelDownload = "取消下载",
        installNow = "立即安装",
        downloadComplete = "下载完成",
        alreadyLatest = "已是最新版本",
        checkUpdateFailed = "检查更新失败",
        downloading = "正在下载...",
        downloadHint = "请等待下载完成",
        downloadFailed = "下载失败",
        openInBrowser = "浏览器下载",
        updateContents = "更新内容",
        feedback = "意见反馈",
        feedbackDescription = "报告问题或提出改进建议",
        problemDescription = "问题描述",
        problemDescriptionHint = "请描述您遇到的问题或建议...",
        contactInfo = "联系方式",
        contactInfoHint = "邮箱或电话（选填）",
        sendFeedback = "发送",
        feedbackSent = "反馈已发送",
        version = "版本 1.0.0",
        helpTutorial = "帮助教程",
        helpTutorialShort = "查看完整的使用指南和功能说明",
        helpTutorialDescription = "",
        github = "GitHub",
        githubDescription = "欢迎关注我们的开源项目",
        contactAuthor = "联系作者",
        contactAuthorDescription = "如有问题或建议，欢迎联系我们",
        poweredBy = "Powered by",
        authorName = "Ricky Miao",
        expenseRatio = "支出比例",
        incomeRatio = "收入比例",
        overallRatio = "综合比例",
        thirdPartyBillImport = "第三方账单导入",
        thirdPartyBillImportDescription = "支持微信和支付宝账单导入（Excel/CSV）",
        importWeChatBill = "导入微信账单",
        importAlipayBill = "导入支付宝账单",
        importWeChatAlipayBill = "导入微信/支付宝账单",
        manageImportedBills = "管理已导入的账单文件",
        selected = "已选择",
        customizeAppExperience = "自定义您的应用体验",
        darkThemeEnabled = "当前使用深色主题",
        lightThemeEnabled = "当前使用浅色主题",
        currentLanguage = "当前语言",
        currentCurrency = "当前货币符号",
        settingsInfo = "设置说明",
        settingsInfoDescription = "更改语言和货币符号后，重启应用即可生效",
        restartAppForChanges = "更改语言和货币符号后，重启应用即可生效",
        chinese = "中文",
        english = "English",
        categoryManagement = "分类配置",
        categoryManagementDescription = "统一管理收入与支出的分类信息",
        categoryAndTagManagement = "类别与标签管理",
        languageChineseLabel = "中文",
        toggleOn = "开",
        toggleOff = "关",
        appUpdateDownload = "应用更新下载",
        showUpdateDownloadProgress = "显示更新下载进度",
        downloadingVersion = "正在下载版本 %s",
        preparingDownload = "准备下载...",
        downloadFailedFileSave = "下载文件保存失败",
        downloadCancelled = "下载已取消",
        downloadFailedGeneric = "下载失败: %s - %s",
        downloadingUpdate = "正在下载更新...",
        clickToInstallUpdate = "点击安装更新"
    ),
    backup = BackupStrings(
        close = "关闭",
        enterBackupName = "为此备份输入名称：",
        backupNamePlaceholder = "我的备份",
        localBackupVault = "本地自动备份安全柜",
        enableAutoBackup = "开启本地自动备份",
        autoBackupDescription = "任何增删改时自动向本地沙盒存入留档",
        backupRetentionLimit = "新备份生成保留上限",
        backupRetentionDescription = "新备份生成保留上限",
        backupRetentionUnit = " 份",
        backupThresholdDescription = "超过此设定阈值时，自动销毁最远历史备份",
        currentBackupStatus = "当前存档状态",
        latestBackupFile = "最新备份文件: ",
        noBackupFound = "尚未发现可用备份文件",
        createManualBackup = "立即创建手动备份",
        manualBackupSuccess = "手动备份创建成功！",
        clearAutoBackups = "清空自动备份",
        clearManualBackups = "清空手动备份",
        backupsCleared = "自动备份池已被强制清空",
        manualBackupsCleared = "手动备份池已被强制清空",
        backupVault = "备份管理柜",
        autoBackup = "自动",
        manualBackup = "手动",
        restore = "恢复",
        delete = "删除",
        deleteBackupSuccess = "已成功删除选定的存档",
        noManualBackups = "您当前还没有生成任何手动备份。",
        latestAutoBackup = "最新自动: ",
        latestManualBackup = "最新手动: ",
        noAutoBackup = "无自动备份",
        noManualBackup = "无手动备份",
        openManualBackupVault = "打开手动备份私密柜",
        infoLimitation = "ZIP 备份包含交易记录、资产记录和附件。导入时会与现有数据智能合并。",
        swipeDeleteConfirm = "左滑删除确认",
        swipeDeleteConfirmEnabled = "左滑松开直接弹出确认",
        swipeDeleteConfirmDisabled = "左滑后点击删除再确认",
        defaultCategory = "默认分类",
        addCategory = "新增分类",
        categoryName = "分类名称",
        renameCategory = "重命名分类",
        newName = "新名称",
        deleteCategory = "删除分类",
        deleteCategoryConfirm = "确定要删除自定义分类 \"{name}\" 吗？此操作无法撤销。",
        appName = "财务管家",
        appSubtitle = "管理你的财务"
    ),
    feature = FeatureStrings(
        asset = "资产",
        assets = "资产",
        assetsDescription = "管理您的资产和负债",
        netAssets = "净资产",
        totalLiabilities = "总负债",
        addAsset = "添加资产",
        editAsset = "编辑资产记录",
        deleteAsset = "删除资产",
        assetStatus = "资产状态",
        none = "未选择",
        owned = "确定拥有",
        notOwned = "确定没有",
        lost = "已失去",
        inProgress = "进行中",
        temporarilyWithMe = "暂时在自己手里",
        temporarilyWithOthers = "暂时在别人手里",
        targetPerson = "目标对象",
        targetAccount = "目标账户",
        includeInTotal = "计入总资产",
        isCompleted = "已完成",
        noAssets = "暂无资产记录。",
        assetRecord = "资产记录",
        positiveAsset = "正资产",
        negativeAsset = "负资产",
        assetFlowCompleted = "已完成",
        assetFlowInProgress = "进行中",
        markComplete = "标识完成",
        markAsOwned = "获得正资产",
        markAsNotOwned = "去除负资产",
        markInProgress = "改变到进行中",
        confirmDelete = "确定删除此资产记录吗？",
        selectCategory = "选择分类",
        budget = "预算",
        showUnbudgetedSpend = "显示未预算支出",
        showBudgetedOnly = "仅显示已预算",
        budgetNotSet = "未设置预算",
        budgetSpent = "已支出：",
        budgetRemaining = "剩余：",
        budgetExceeded = "超出：",
        budgetInvalidAmount = "请输入有效金额",
        budgetZeroWarning = "预算金额不能为零",
        budgetSubtitle = "预算管理",
        financialArchive = "完整数据归档",
        financialArchiveDescription = "导入或导出包含业务数据、设置、附件和账单文件的完整 ZIP 归档。",
        openArchive = "打开归档",
        attachmentOverviewDescription = "统一查看交易、资产、审批和账单关联的文件。",
        itemName = "物品名称",
        specification = "规格型号",
        quantity = "数量",
        expenseCategory = "支出分类",
        assetCategory = "资产分类",
        budgetsDescription = "跟踪和管理您的预算",
        approvalCenter = "审批中心",
        approvalApplicant = "申请人",
        approvalApprover = "审批人",
        approvalTodo = "待办",
        approvalHistory = "历史",
        approvalNewRequest = "新建申请",
        approvalBudgetAdjustment = "预算调整申请",
        approvalPurchaseBudget = "采购预算申请",
        approvalPending = "待审批",
        approvalApproved = "已通过",
        approvalRejected = "已拒绝",
        approvalWithdrawn = "已撤回",
        approvalReason = "申请事由",
        approvalPurchaseDate = "采购日期",
        approvalSubmit = "提交申请",
        approvalWithdraw = "撤回",
        approvalResubmit = "重新提交",
        approvalApprove = "通过",
        approvalReject = "拒绝",
        approvalDecisionNote = "审批意见",
        approvalCurrentBudget = "当前预算",
        approvalRemaining = "预算剩余",
        approvalNoRequests = "暂无审批申请",
        approvalMissingCategory = "请选择分类",
        approvalInvalidAmount = "请输入大于零的金额",
        approvalSubmitted = "申请已提交",
        approvalView = "查看",
        approvalDetailTitle = "查看申请详情",
        approvalRequestId = "申请编号",
        approvalRequestType = "申请类型",
        approvalCreatedAt = "创建时间",
        approvalCurrentStatus = "当前审批状态",
        approvalDecidedAt = "决定时间",
        approvalNotApplicable = "不适用",
        approvalPurchaseProgress = "采购进度",
        approvalAwaitingPurchase = "已通过，待采购",
        approvalPurchaseInProgress = "采购进行中",
        approvalConfirmedOwned = "已确认拥有",
        approvalNotStarted = "未开始",
        approvalRelatedAsset = "关联资产",
        approvalRealTransaction = "真实支出交易",
        approvalNotSet = "未设置",
        approvalMissingAssetCategory = "请选择资产分类",
        approvalInvalidQuantity = "请输入大于零的数量",
        addAssetCategoryFirst = "请先添加资产分类",
        enterValidAmount = "请输入有效金额",
        mergeSuccess = "成功导入 %d 条新记录",
        mergeNoNewData = "没有新记录可导入",
        mergeParseFailed = "解析失败：%s",
        cannotReadFile = "无法读取所选文件",
        unrecognizedBillFormat = "无法识别的账单格式",
        noTransactionsToImport = "没有可导入的交易记录",
        wechat = "微信",
        alipay = "支付宝",
        excludedRefundInfo = "（已排除 %d 条退款交易）",
        billImportSuccess = "%s 账单导入：成功导入 %d 条交易%s",
        importFailed = "导入失败：%s",
        clearAllTransactionsButton = "删除所有交易记录",
        dangerWarning = "危险警告",
        clearAllDataWarningMessage = "此操作将永久删除所有交易记录和资产记录，且无法恢复。建议先导出备份再执行此操作。",
        allRecordsCleared = "所有记录已清除",
        clearAnyway = "仍然清除",
        clearAllData = "清除所有数据",
        clearAllDataDescription = "删除所有交易记录和资产记录",
        clearAllDataWarning = "此操作将永久删除所有交易记录和资产记录，且无法恢复。建议先导出备份再执行此操作。",
        clearAllDataConfirmTitle = "确认清除所有数据？",
        clearAllDataWillDelete = "此操作将永久删除：",
        clearAllDataTransactions = "所有交易记录",
        clearAllDataAssets = "所有资产记录",
        clearAllDataAttachments = "相关附件文件",
        clearAllDataCannotUndo = "此操作不可撤销！",
        clearAllDataConfirm = "确认清除",
        clearAllDataSuccess = "所有数据已清除",
        clearTransactions = "删除交易记录",
        clearAssets = "删除资产记录",
        clearTransactionsConfirmTitle = "删除所有交易记录？",
        clearTransactionsConfirm = "此操作将删除所有交易记录，且无法恢复！",
        clearTransactionsSuccess = "所有交易记录已清除",
        clearAssetsConfirmTitle = "删除所有资产记录？",
        clearAssetsConfirm = "此操作将删除所有资产记录和附件，且无法恢复！",
        clearAssetsSuccess = "所有资产记录已清除",
        reimportBill = "重新导入",
        reimportSuccess = "成功导入 %d 条新记录",
        noNewRecordsToImport = "没有新记录可导入",
        thirdPartyBillFileManagement = "第三方账单文件管理",
        noImportedBillFiles = "暂无已导入的账单文件",
        wechatBill = "微信账单",
        alipayBill = "支付宝账单",
        unknownBill = "未知账单",
        archiveExportSuccess = "归档导出成功",
        archiveExportFailed = "归档导出失败"
    ),
    help = HelpStrings(
        helpAboutProject = "关于项目",
        helpAboutDescription1 = "AccountKeeper 是一款开源的个人财务管理应用。",
        helpAboutDescription2 = "帮助您轻松记录和管理日常收支。",
        helpHomeFeatures = "首页功能",
        helpBalanceCard = "余额卡片",
        helpBalanceCardDetail1 = "总余额：当前净余额（收入 - 支出）",
        helpBalanceCardDetail2 = "总收入：当前时间范围内的总收入",
        helpBalanceCardDetail3 = "总支出：当前时间范围内的总支出",
        helpSearchFeature = "搜索功能",
        helpSearchDetail1 = "通过备注或分类名称搜索",
        helpSearchDetail2 = "输入时实时更新结果",
        helpSearchDetail3 = "搜索所有交易记录",
        helpTransactionListOps = "交易列表操作",
        helpTxTapDetail = "点击编辑交易",
        helpTxSwipeDetail = "向左滑动删除交易",
        helpTxLongPressDetail = "长按进入选择模式进行批量操作",
        helpTxBatchDelete = "选择多个交易批量删除",
        helpAddTransaction = "添加交易",
        helpAddTxDetail1 = "使用数字键盘输入金额",
        helpAddTxDetail2 = "选择收入或支出类型",
        helpAddTxDetail3 = "选择分类",
        helpAddTxDetail4 = "添加备注并保存",
        helpStatistics = "统计分析",
        helpTimeRangeSelection = "时间范围选择",
        helpTimeRangeDay = "日：查看单日数据",
        helpTimeRangeWeek = "周：查看本周数据（周一到周日）",
        helpTimeRangeMonth = "月：查看本月数据",
        helpTimeRangeYear = "年：查看全年数据",
        helpTimeRangeCustom = "自定义：选择开始和结束日期",
        helpChartDisplay = "图表展示",
        helpChartLine = "折线图显示趋势变化",
        helpChartPie = "饼图显示分类占比",
        helpChartRanking = "分类排行榜按金额排序",
        helpCategoryFilter = "分类筛选",
        helpCategoryFilterDetail1 = "按支出或收入分类筛选",
        helpCategoryFilterDetail2 = "查看特定分类的交易",
        helpCategoryFilterDetail3 = "结合时间范围进行详细分析",
        helpAssetManagement = "资产管理",
        helpAssetStatistics = "资产统计",
        helpAssetNetWorth = "净资产：总资产减去总负债",
        helpCurrentAssets = "当前资产：您当前拥有的资产",
        helpCurrentLiabilities = "当前负债：债务和义务",
        helpTransactionBalance = "交易余额：所有交易的净额",
        helpAddAssetRecord = "添加资产记录",
        helpAddAssetDetail1 = "选择资产类型和分类",
        helpAddAssetDetail2 = "输入资产名称和金额",
        helpAddAssetDetail3 = "设置资产状态（拥有、进行中等）",
        helpAddAssetDetail4 = "添加可选的目标对象或账户",
        helpStatusExplanation = "状态说明",
        helpStatusInProgress = "进行中：资产流转正在进行",
        helpStatusCompleted = "已完成：资产已获得或移除",
        helpStatusCancelled = "已取消：资产流转已取消",
        helpDataManagement = "数据管理",
        helpExportData = "导出数据",
        helpExportDetail1 = "导出所有数据为 ZIP 备份",
        helpExportDetail2 = "导出 CSV 用于电子表格分析",
        helpExportDetail3 = "备份包含交易、资产和附件",
        helpImportData = "导入数据",
        helpImportDetail1 = "导入 ZIP 备份恢复数据",
        helpImportDetail2 = "导入 CSV 与现有数据合并",
        helpImportDetail3 = "重复 ID 将在导入时跳过",
        helpBillImport = "账单导入",
        helpBillImportDetail1 = "导入微信支付账单",
        helpBillImportDetail2 = "导入支付宝账单",
        helpBillImportDetail3 = "自动识别账单类型并解析交易",
        helpBillImportDetail4 = "退款交易自动识别为收入",
        helpBillImportDetail5 = "相同 ID 的交易不会重复导入",
        helpHowToExportBill = "如何导出账单",
        helpExportBillWechat = "微信：我 -> 服务 -> 钱包 -> 账单 -> 导出",
        helpExportBillAlipay = "支付宝：我的 -> 右上角菜单 -> 账单 -> 导出",
        helpExportBillFormat = "导出为 CSV 格式",
        helpExportBillImport = "然后在本应用中导入 CSV 文件",
        helpAutoBackup = "自动备份",
        helpAutoBackupDetail1 = "每次增删改时自动创建备份",
        helpAutoBackupDetail2 = "无需手动操作",
        helpAutoBackupDetail3 = "设置保留上限（5-50 份）",
        helpAutoBackupDetail4 = "超过限制时自动删除最旧备份",
        helpCategoryManagement = "分类管理",
        helpCategoryTypes = "分类类型",
        helpExpenseCategories = "支出：餐饮、交通、购物等",
        helpIncomeCategories = "收入：工资、奖金、投资等",
        helpAssetCategories = "资产：正资产（拥有）和负资产（欠款）类型",
        helpManagementOps = "管理操作",
        helpAddCategory = "使用 + 按钮添加自定义分类",
        helpRenameCategory = "使用编辑图标重命名",
        helpDeleteCategory = "使用垃圾桶图标删除（仅自定义分类）",
        helpPresetCategoryNoDelete = "默认分类不可删除",
        helpPersonalization = "个性化",
        helpAppearanceSettings = "外观设置",
        helpDarkMode = "深色模式：适合夜间使用",
        helpLightMode = "浅色模式：适合白天使用",
        helpLanguage = "语言：支持中文和英文",
        helpCurrency = "货币：支持多种货币符号",
        helpNotes = "重要说明",
        helpRestartRequired = "语言和货币更改需要重启应用",
        helpOldSettingsBeforeRestart = "重启前使用旧设置",
        helpTips = "使用技巧",
        helpDailyTips = "日常记账习惯",
        helpDailyTip1 = "消费后立即记录，避免遗忘",
        helpDailyTip2 = "添加详细备注帮助回忆交易详情",
        helpDailyTip3 = "使用分类功能更好地管理财务",
        helpDataSafety = "数据安全",
        helpDataSafetyTip1 = "每周创建一次手动备份",
        helpDataSafetyTip2 = "开启自动备份确保每次操作都有备份",
        helpDataSafetyTip3 = "使用 CSV 导出在设备间同步数据",
        helpBillImportTips = "账单导入技巧",
        helpBillImportTip1 = "每月定期导入一次账单",
        helpBillImportTip2 = "导入后核对数据是否正确",
        helpBillImportTip3 = "为账单交易添加有用的备注",
        helpFAQ = "常见问题",
        helpFAQRecover = "如何找回删除的交易？",
        helpFAQRecoverDetail1 = "删除的交易无法直接恢复",
        helpFAQRecoverDetail2 = "如果有备份可以恢复备份文件",
        helpFAQPartialImport = "为什么部分交易未导入？",
        helpFAQPartialImportDetail1 = "可能是 ID 重复或金额为零",
        helpFAQPartialImportDetail2 = "系统会自动跳过这些交易",
        helpFAQPartialImportDetail3 = "请检查原始账单",
        helpFAQSync = "如何在多个设备间同步数据？",
        helpFAQSyncDetail1 = "在一个设备导出 CSV 文件",
        helpFAQSyncDetail2 = "在其他设备导入即可",
        helpFAQSyncDetail3 = "重复 ID 将被跳过",
        helpHomeTimeRange = "首页时间范围切换",
        helpHomeTimeRangeDetail1 = "月：查看本月收入和支出",
        helpHomeTimeRangeDetail2 = "年：查看本年收入和支出",
        helpHomeTimeRangeDetail3 = "全部：查看所有收入和支出",
        helpHomeFilter = "首页交易筛选",
        helpHomeFilterDetail1 = "按日期范围、分类或排序方式筛选",
        helpHomeFilterDetail2 = "筛选条件显示为列表上方的标签",
        helpHomeFilterDetail3 = "点击标签可清除单个筛选条件",
        helpBudgetPeriods = "预算周期选项",
        helpBudgetPeriodsDetail1 = "月度：标准月度预算",
        helpBudgetPeriodsDetail2 = "半年：6个月预算周期",
        helpBudgetPeriodsDetail3 = "年度：全年预算周期",
        helpBillPreview = "账单文件预览",
        helpBillPreviewDetail1 = "点击账单文件卡片可预览解析后的交易",
        helpBillPreviewDetail2 = "支持微信和支付宝账单格式",
        helpAssetAttachment = "资产附件",
        helpAssetAttachmentDetail1 = "可为资产附加图片、Excel或CSV文件",
        helpAssetAttachmentDetail2 = "点击附件图标可预览文件内容"
    )
)
