package com.example.accountkeeper.ui.theme

data class AppStrings(
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
    val amount: String,
    val date: String,
    val category: String,
    val note: String,
    val save: String,
    val daily: String,
    val weekly: String,
    val monthly: String,
    val yearly: String,
    val custom: String,
    val totalIncome: String,
    val totalExpense: String,
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
    val infoLimitation: String,
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
    val openManualBackupVault: String,
    val categoryManagement: String,
    val categoryManagementDescription: String,
    val categoryAndTagManagement: String,
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
    val close: String,
    val enterBackupName: String,
    val backupNamePlaceholder: String,
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
    val importWeChatAlipayBill: String, // 兼容旧界面
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
    val defaultCategory: String,
    val addCategory: String,
    val categoryName: String,
    val renameCategory: String,
    val newName: String,
    val deleteCategory: String,
    val deleteCategoryConfirm: String,
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
    // Asset related
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
    val inProgress: String,
    val temporarilyWithMe: String,
    val temporarilyWithOthers: String,
    val targetPerson: String,
    val targetAccount: String,
    val includeInTotal: String,
    val isCompleted: String,
    val noAssets: String,
    val assetRecord: String,
    // Asset type in category
    val positiveAsset: String,
    val negativeAsset: String,
    // Asset flow status
    val assetFlowCompleted: String,
    val assetFlowInProgress: String,
    val markComplete: String,
    val markAsOwned: String,
    val markAsNotOwned: String,
    val markInProgress: String,
    val confirmDelete: String,
    val selectCategory: String,
    // Swipe delete confirm
    val swipeDeleteConfirm: String,
    val swipeDeleteConfirmEnabled: String,
    val swipeDeleteConfirmDisabled: String,
    // Filter and sort (additional)
    val sort: String,
    val categoryFilter: String,
    val allCategories: String,
    val timeDescending: String,
    val timeAscending: String,
    val amountDescending: String,
    val amountAscending: String,
    // Attachments
    val attachments: String,
    val addAttachment: String,
    val removeAttachment: String,
    val noAttachments: String,
    val selectFile: String,
    val fileSizeTooLarge: String,
    // Update related
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
    val openInBrowser: String
)

val EnStrings = AppStrings(
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
    clearFilter = "Clear Filter",
    thisMonth = "This Month",
    income = "Income",
    expense = "Expense",
    balanceOverall = "Overall",
    startDate = "Start",
    endDate = "End",
    recentTransactions = "Recent Transactions",
    addTransaction = "Add Transaction",
    editTransaction = "Edit Transaction",
    deleteTransaction = "Delete Transaction",
    deleteConfirm = "Are you sure you want to delete this transaction?",
    amount = "Amount",
    date = "Date",
    category = "Category",
    note = "Note",
    save = "Save",
    daily = "Daily",
    weekly = "Weekly",
    monthly = "Monthly",
    yearly = "Yearly",
    custom = "Custom",
    totalIncome = "Total Income",
    totalExpense = "Total Expense",
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
    infoLimitation = "ZIP backup contains transactions, assets, and attachments. Import to merge data with existing records.",
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
    openManualBackupVault = "Open Private Manual Backup Vault",
    categoryManagement = "Category Management",
    categoryManagementDescription = "Unified management of income and expense category information",
    categoryAndTagManagement = "Category & Tag Management",
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
    close = "Close",
    enterBackupName = "Enter a name for this backup:",
    backupNamePlaceholder = "My Backup",
    version = "Version 1.0.0",
    helpTutorial = "Help & Tutorial",
    helpTutorialShort = "View complete usage guide and feature descriptions",
    helpTutorialDescription = """Learn how to use AccountKeeper to manage your finances.

# 📖 AccountKeeper Complete User Guide

Welcome to AccountKeeper! This is a simple yet powerful personal finance management app designed to help you easily record and manage your daily expenses and income. Here is a comprehensive usage guide.

## 🏠 Home Screen Guide

### Balance Card

**Displayed Information**
- **Total Balance**: Current net balance (Income - Expenses)
- **Total Income**: Total income within the selected time range
- **Total Expenses**: Total expenses within the selected time range

**Interaction Features**
- **Tap "This Month/Total Assets"**: Toggle view range
  - This Month: Shows only current month's financial data
  - Total Assets: Shows all historical data
- **Tap Right Arrow**: Expand/Collapse detailed information

### Transaction List

**Display Method**
- Sorted by date in descending order (newest first)
- Each date shows the day's income/expense summary
- Income displayed in green, expenses displayed in red

**Transaction Card Information**
- **Category Icon**: Circular icon displaying first letter of category name
- **Category Name**: Transaction's category
- **Note**: Optional transaction description (if available)
- **Time**: Transaction record time (Format: HH:mm)
- **Amount**: Amount with sign (+ indicates income, - indicates expense)

### Quick Operations

**1. Single Transaction Operations**
- **Tap Transaction Card**: Enter edit page
  - Modify amount, date, category, note
  - Changes take effect immediately after saving

- **Swipe Left on Transaction Card**: Show delete option
  - Card moves left approximately 30% to reveal red background
  - Red background displays delete icon (X)
  - Tap red background area to delete the transaction
  - Tap other areas to restore card position

- **Long Press Transaction Card**: Enter selection mode
  - That transaction becomes selected
  - You can continue tapping other transactions to multi-select

**2. Batch Operations**

**After Entering Selection Mode**
- Top bar displays count of selected transactions (e.g., "2 selected")
- All selected transactions show highlighted border

**Top Bar Buttons**
- **Left Down Arrow**: Exit selection mode
- **Edit Icon (Pencil)**: Edit selected single transaction (only available when exactly 1 transaction selected)
- **Delete Icon (Red X)**: Batch delete all selected transactions
  - Shows confirmation dialog
  - Confirm to delete

**Ways to Exit Selection Mode**
- Tap the down arrow in top left corner
- Or tap blank area elsewhere

**Add New Transaction**
- Tap the **+** button in bottom right corner
- Enter add transaction page

## ➕ Add/Edit Transaction

### Interface Layout

**Top Bar**
- Left: Back button
- Center: Title ("Add Transaction" or "Edit Transaction")
- Right: Save button

**Amount Input Area**
- Large number keypad
- Real-time display of entered amount
- Supports decimal point

**Income/Expense Type Toggle**
- Two large buttons: "Expense" and "Income"
- Tap to switch type
- Category list changes accordingly after switching

**Date Selection**
- Shows currently selected date
- Tap to open date picker
- Can select any historical date

**Category Selection**
- Grid layout displays all categories
- Each category shows icon and name
- Tap to select category
- Default categories cannot be deleted

**Note Input**
- Optional text input field
- Used to add transaction description
- Can be left empty

### Operation Steps

**Add New Transaction**
1. Tap + button to enter page
2. Enter amount (e.g., 100)
3. Select income/expense type
4. Select or modify date (defaults to today)
5. Select category
6. Optional: Enter note
7. Tap save button in top right corner

**Edit Existing Transaction**
1. Tap the transaction you want to edit on home screen
2. Page automatically populates with all transaction information
3. Modify content you want to change
4. Tap save button

### Important Notes

- Amount cannot be empty or zero
- Must select a category
- Modified transaction will overwrite original data

## 📊 Statistics Analysis

### Time Range Selection

**Preset Ranges**
- **Day**: View single day data (defaults to current day)
- **Week**: View this week's data (Monday to Sunday)
- **Month**: View this month's data (1st to end of month)
- **Year**: View full year data (January to December)

**Custom Range**
- Tap "Custom" to enter date picker
- Select start date and end date
- Confirm to display data within that range

### Statistics Type

**Three Statistics Types**
- **Expense**: Only statistics for expense data
- **Income**: Only statistics for income data
- **Overall**: Display comparison of income and expenses

### Data Display

**Total Amount Statistics**
- Displays total amount within selected time range
- Shows total income or total expenses or net balance based on statistics type

**Trend Chart**
- Line chart showing trends over time
- X-axis: Time (automatically adjusted based on selected time range)
- Y-axis: Amount
- Different colors distinguish different data

**Category Proportion Chart**
- Pie chart showing proportion of each category
- Hover to view specific percentage
- Sorted by amount from large to small

**Category Leaderboard**
- List showing each category's amount
- Sorted by amount from high to low
- Displays category icon, name and amount

### Usage Tips

- Switching different time ranges helps understand spending trends
- Select "Overall" type to compare income and expenses
- Category proportion chart helps identify major spending areas
- Use date range to export reports for specific periods

## ⚙️ Personalization Settings

### Theme Settings

**Dark Mode**
- Protect eyes, suitable for nighttime use
- Dark background, light text

**Light Mode**
- Suitable for daytime use
- Bright background, dark text

**Auto Switch**
- Automatically switches based on system theme
- Need to enable in system settings

### Language Settings

**Supported Languages**
- Chinese (Simplified)
- English

**Switching Steps**
1. Enter Personalization Settings
2. Tap "Interface Language"
3. Select target language
4. Restart app for changes to take effect

### Currency Symbol

**Supported Currencies**
- ¥ (Chinese Yuan)
- $ (US Dollar)
- € (Euro)
- £ (British Pound)
- ₩ (South Korean Won)
- ₹ (Indian Rupee)
- ₽ (Russian Ruble)
- ฿ (Thai Baht)

**Switching Steps**
1. Enter Personalization Settings
2. Tap "Currency Symbol"
3. Select target currency
4. Restart app for changes to take effect

### Important Notes

- Language and currency symbol changes require app restart
- Can continue using old settings before restart
- Changes affect all amount displays

## 💾 Data Management

### CSV Data Import/Export

**Export Function**

**Purpose**
- Backup all transaction data
- View on other devices or applications
- Data analysis and report generation

**Export Steps**
1. Enter Data Management page
2. Find "Local Data Archive" section
3. Tap "Export Full Ledger to CSV"
4. Select save location
5. Confirm export

**Export File Format**
- CSV format (Comma Separated Values)
- Contains fields: ID, Date, Type, Amount, Category, Note
- File name format: AccountKeeper_Export_YYYYMMDD.csv

**Import Function**

**Purpose**
- Restore data from backup
- Merge data from multiple devices
- Migrate data to new device

**Import Steps**
1. Enter Data Management page
2. Find "Local Data Archive" section
3. Tap "Import Standard CSV Backup"
4. Select previously exported CSV file
5. System automatically recognizes and imports

**Data Merge Rules**
- Transactions with duplicate IDs will be skipped
- Missing categories will be automatically created
- Existing categories will not be created duplicate
- Shows number of successfully imported transactions

### Third-party Bill Import

**Supported Bills**
- WeChat Pay bills
- Alipay bills

**Import Steps**
1. Enter Data Management page
2. Find "Third-party Bill Import" section
3. Tap "Import WeChat/Alipay Bill"
4. Select bill CSV file
5. System automatically recognizes bill type
6. Wait for import to complete

**Bill Recognition**
- Automatically detects bill type (WeChat/Alipay)
- Parses transaction time, amount, type, name, note
- Automatically matches or creates categories
- Intelligently recognizes income and expenses

**Smart Processing**
- Refund transactions automatically recognized as income
- Transactions with same ID will not be imported duplicate
- Supports multiple date formats

**Important Notes**
- Only supports standard WeChat/Alipay bill CSV format
- Need to manually export bills from WeChat/Alipay
- After import, can view in "Manage Imported Bill Files"

### Local Auto Backup

**Function Description**
- Automatically creates backup on every add, delete or modify operation
- No manual operation required, automatically protects data

**Enable Steps**
1. Enter Data Management page
2. Find "Local Auto Backup Safe" section
3. Toggle "Enable Local Auto Backup" switch

**Retention Settings**
- Can set number of backups to keep (5-50 backups)
- Automatically deletes oldest backups when limit exceeded
- Recommend setting appropriate number based on usage frequency

**View Backups**
- Shows time of latest auto backup
- Shows time of latest manual backup
- Shows "No auto backup" or "No manual backup" if none exist

### Manual Backup

**Create Backup**
1. Enter Data Management page
2. Tap "Create Manual Backup Now"
3. Enter backup name (optional)
4. Confirm creation

**Backup Purpose**
- Manual backup before important operations
- Regular data backup
- Create snapshot at specific point in time

**Manage Backups**
- Tap "Backup Management Cabinet" to view all backups
- Tap "Manual" tab to view manual backups
- Tap "Auto" tab to view auto backups
- Tap delete icon to delete unwanted backups

### Restore Backup

**Restore Steps**
1. Enter Data Management page
2. Tap "Backup Management Cabinet"
3. Select "Manual" or "Auto" tab
4. Find the backup you want to restore
5. Tap "Restore" button
6. Confirm restore

**Restore Effect**
- Overwrites all current data
- Cannot be undone
- Recommend creating manual backup before restore

## 🏷️ Category Management

### Category Types

**Expense Categories**
- Records all expense transactions
- Preset categories: Dining, Transportation, Shopping, Entertainment, Medical, Education, etc.

**Income Categories**
- Records all income transactions
- Preset categories: Salary, Bonus, Investment Income, Part-time Income, etc.

### View Categories

**Interface Layout**
- Two tabs at top: Expense and Income
- Tap tabs to switch between viewing different category types
- Each category shows name
- Default categories have special marker

### Add Category

**Add Steps**
1. Tap the + button in bottom right corner
2. Enter category name
3. Tap "OK" to save

**Naming Suggestions**
- Use simple and clear names
- Avoid using special characters
- Can use Chinese or English
- Examples: Breakfast, Lunch, Dinner, Transportation

### Rename Category

**Rename Steps**
1. Find the category you want to modify
2. Tap the edit icon (pencil) on the right
3. Modify category name
4. Tap "OK" to save

**Important Notes**
- Modifying name affects all transactions using that category
- Does not affect transaction data itself

### Delete Category

**Delete Steps**
1. Find the custom category you want to delete
2. Tap the delete icon (red trash can) on the right
3. Confirm deletion

**Restrictions**
- Default categories cannot be deleted
- After deleting category, transactions using that category are not affected
- But that category will disappear from category list

### Usage Tips

- Create categories that fit your personal habits
- Don't make categories too detailed to avoid difficulty in selection
- Regularly clean up unused categories
- Use meaningful category names

## 💡 Usage Tips and Best Practices

### Daily Bookkeeping Habits

1. **Record Immediately**: Record immediately after spending to avoid forgetting
2. **Detailed Notes**: Add notes to help recall transaction details
3. **Reasonable Categorization**: Use categorization to better manage finances

### Data Security

1. **Regular Backup**: Create manual backup once a week
2. **Enable Auto Backup**: Ensure every operation has a backup
3. **Multi-device Sync**: Use CSV import/export to sync data between devices

### Financial Analysis

1. **Regular Statistics**: View statistics page once a month
2. **Focus on Trends**: Observe income/expense trends to adjust spending habits
3. **Identify Problem Areas**: Find major expenses through category proportion

### Bill Management

1. **Regular Import**: Import WeChat/Alipay bills once a month
2. **Verify Data**: Verify transactions are correct after import
3. **Add Notes**: Add useful notes for bill transactions

### Advanced Tips

1. **Batch Operations**: Use long press to enter selection mode for batch delete or edit
2. **Swipe Delete**: Use swipe function to quickly delete unwanted transactions
3. **Custom Categories**: Create personalized category system

## ⚠️ Important Notes

### Data Security

- **Deletion Cannot Be Recovered**: Deleted transactions cannot be undone, please operate with caution
- **Regular Backup**: Recommend creating manual backup weekly
- **Device Replacement**: Export data before replacing device

### System Requirements

- **Language Switching**: Need to restart app after switching language or currency symbol
- **Storage Space**: Ensure device has enough storage space for backups
- **Network Requirement**: Third-party bill import does not require network

### Data Limitations

- **CSV Format**: Only supports standard CSV format exported from the app
- **Bill Format**: Only supports standard bill CSV format from WeChat/Alipay
- **Backup Retention**: Auto backup automatically deletes old backups, please set appropriate retention number

### Compatibility

- **Data Merge**: When importing CSV, transactions with duplicate IDs will be skipped
- **Category Auto-creation**: Missing categories will be automatically created during import
- **Default Categories**: Preset categories cannot be deleted but can be renamed

## 🆘 Frequently Asked Questions

**Q: How to recover deleted transactions?**
A: Deleted transactions cannot be directly recovered. If you have a backup, you can restore the backup file.

**Q: Some transactions were not imported when importing bills?**
A: This may be because IDs are duplicate or amounts are zero. System automatically skips these transactions.

**Q: How to sync data between multiple devices?**
A: Export CSV file on one device, then import on other devices.

**Q: Why interface doesn't change after switching language?**
A: Need to restart app for language changes to take effect.

**Q: Can I modify preset categories?**
A: You can rename preset categories but cannot delete them.

**Q: How much storage space does auto backup occupy?**
A: Depends on data volume. You can check backup size in settings.

## 📞 Technical Support

If you encounter problems or have suggestions, please feel free to contact us:
- Email: rickymiao63@163.com
- GitHub: https://github.com/miaotenone/AccountKeeper

Thank you for using AccountKeeper! Wish you smooth financial management!""",
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
    restartAppForChanges = "Restart the app for changes to take effect",
    chinese = "Chinese",
    english = "English",
    defaultCategory = "Default category",
    addCategory = "Add Category",
    categoryName = "Category Name",
    renameCategory = "Rename Category",
    newName = "New Name",
    deleteCategory = "Delete Category",
    deleteCategoryConfirm = "Are you sure you want to delete custom category \"{name}\"? This action cannot be undone.",
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
    // Asset related
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
    swipeDeleteConfirm = "Swipe Delete Confirmation",
    swipeDeleteConfirmEnabled = "Swipe to delete directly triggers confirmation",
    swipeDeleteConfirmDisabled = "Click delete button after swipe to confirm",
    // Filter and sort (additional)
    sort = "Sort",
    categoryFilter = "Category",
    allCategories = "All Categories",
    timeDescending = "Time (Newest First)",
    timeAscending = "Time (Oldest First)",
    amountDescending = "Amount (High to Low)",
    amountAscending = "Amount (Low to High)",
    // Attachments
    attachments = "Attachments",
    addAttachment = "Add Attachment",
    removeAttachment = "Remove",
    noAttachments = "No attachments",
    selectFile = "Select File",
    fileSizeTooLarge = "File size exceeds limit",
    // Update related
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
    openInBrowser = "Open in Browser"
)

val ZhStrings = AppStrings(
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
    clearFilter = "清除筛选",
    thisMonth = "本月",
    income = "收入",
    expense = "支出",
    balanceOverall = "综合",
    startDate = "开始",
    endDate = "结束",
    recentTransactions = "近期交易",
    addTransaction = "记录交易",
    editTransaction = "修改交易",
    deleteTransaction = "删除交易",
    deleteConfirm = "确定要删除这条交易吗？",
    amount = "金额",
    date = "日期",
    category = "分类",
    note = "备注",
    save = "保存",
    daily = "日",
    weekly = "周",
    monthly = "月",
    yearly = "年",
    custom = "自定义",
    totalIncome = "总收入",
    totalExpense = "总支出",
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
    infoLimitation = "ZIP 备份包含交易记录、资产记录和附件。导入时会与现有数据智能合并。",
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
    openManualBackupVault = "打开手动备份私密柜",
    categoryManagement = "分类配置",
    categoryManagementDescription = "统一管理收入与支出的分类信息",
    categoryAndTagManagement = "类别与标签管理",
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
    close = "关闭",
    enterBackupName = "为此备份输入名称：",
    backupNamePlaceholder = "我的备份",
    version = "版本 1.0.0",
    helpTutorial = "帮助教程",
    helpTutorialShort = "查看完整的使用指南和功能说明",
    helpTutorialDescription = """学习如何使用 AccountKeeper 管理您的财务。

# 📖 AccountKeeper 完整使用指南

欢迎使用 AccountKeeper！这是一款简洁易用的个人财务管理应用，帮助您轻松记录和管理日常收支。以下是详细的使用指南。

## 🏠 首页功能详解

### 余额卡片

**显示内容**
- **总余额**：当前账户的净余额（收入 - 支出）
- **总收入**：当前时间范围内的总收入
- **总支出**：当前时间范围内的总支出

**交互功能**
- **点击"本月/总资产"**：切换查看范围
  - 本月：仅显示当前月份的收支数据
  - 总资产：显示所有历史数据
- **点击右侧箭头**：展开/收起详细信息

### 交易列表

**显示方式**
- 按日期倒序排列（最近的在最上面）
- 每个日期显示当天的收支汇总
- 收入用绿色显示，支出用红色显示

**交易卡片信息**
- **分类图标**：显示分类首字母的圆形图标
- **分类名称**：交易的分类
- **备注**：可选的交易说明（如果有）
- **时间**：交易的记录时间（格式：HH:mm）
- **金额**：带符号的金额（+表示收入，-表示支出）

### 快捷操作

**1. 单个交易操作**
- **点击交易卡片**：进入编辑页面
  - 修改金额、日期、分类、备注
  - 保存后立即生效

- **向左滑动交易卡片**：显示删除选项
  - 卡片向左移动约 30% 时显示红色背景
  - 背景显示删除图标（X）
  - 点击红色背景区域即可删除该交易
  - 点击其他区域卡片恢复原位

- **长按交易卡片**：进入选择模式
  - 该交易会被选中
  - 可以继续点击其他交易进行多选

**2. 批量操作**

**进入选择模式后**
- 顶部栏显示已选中的交易数量（如"2 已选择"）
- 所有选中的交易会显示高亮边框

**顶部栏按钮**
- **左侧向下箭头**：退出选择模式
- **编辑图标（铅笔）**：编辑选中的单个交易（仅选中 1 个时可用）
- **删除图标（红色 X）**：批量删除选中的所有交易
  - 弹出确认对话框
  - 确认后删除

**退出选择模式的方法**
- 点击左上角的向下箭头
- 或点击其他区域的空白处

**添加新交易**
- 点击右下角的 **+** 按钮
- 进入添加交易页面

## ➕ 添加/编辑交易

### 界面布局

**顶部栏**
- 左侧：返回按钮
- 中间：标题（"添加交易"或"编辑交易"）
- 右侧：保存按钮

**金额输入区**
- 大号数字键盘
- 实时显示输入的金额
- 支持小数点

**收支类型切换**
- 两个大按钮："支出"和"收入"
- 点击切换类型
- 切换后分类列表也会相应变化

**日期选择**
- 显示当前选择的日期
- 点击打开日期选择器
- 可选择任意历史日期

**分类选择**
- 网格布局显示所有分类
- 每个分类显示图标和名称
- 点击选择分类
- 默认分类不可删除

**备注输入**
- 可选的文本输入框
- 用于添加交易说明
- 可以为空

### 操作步骤

**添加新交易**
1. 点击 + 按钮进入页面
2. 输入金额（如：100）
3. 选择收支类型（支出/收入）
4. 选择或修改日期（默认为今天）
5. 选择分类
6. 可选：输入备注
7. 点击右上角保存按钮

**编辑现有交易**
1. 在首页点击要编辑的交易
2. 页面会自动填充该交易的所有信息
3. 修改需要更改的内容
4. 点击保存按钮

### 注意事项

- 金额不能为空或零
- 必须选择分类
- 修改后的交易会覆盖原有数据

## 📊 统计分析

### 时间范围选择

**预设范围**
- **日**：查看单日数据（默认为当天）
- **周**：查看本周数据（周一到周日）
- **月**：查看本月数据（1日到月末）
- **年**：查看全年数据（1月到12月）

**自定义范围**
- 点击"自定义"进入日期选择器
- 选择开始日期和结束日期
- 确认后显示该范围内的数据

### 统计类型

**三种统计类型**
- **支出**：仅统计支出数据
- **收入**：仅统计收入数据
- **综合**：同时显示收入和支出的对比

### 数据展示

**总金额统计**
- 显示该时间范围内的总金额
- 根据统计类型显示总收入或总支出或净余额

**趋势图**
- 折线图显示随时间的变化趋势
- 横轴：时间（根据选择的时间范围自动调整）
- 纵轴：金额
- 不同颜色区分不同数据

**分类占比图**
- 饼图显示各分类的占比
- 鼠标悬停可查看具体百分比
- 按金额从大到小排序

**分类排行榜**
- 列表显示各分类的金额
- 按金额从高到低排序
- 显示分类图标、名称和金额

### 使用技巧

- 切换不同时间范围可以了解消费趋势
- 选择"综合"类型可以对比收入和支出
- 分类占比图帮助识别主要支出领域
- 使用日期范围导出特定时期的报表

## ⚙️ 个性化设置

### 主题设置

**深色模式**
- 保护眼睛，适合夜间使用
- 暗色背景，浅色文字

**浅色模式**
- 适合白天使用
- 明亮背景，深色文字

**自动切换**
- 根据系统主题自动切换
- 需要在系统设置中启用

### 语言设置

**支持语言**
- 中文（简体）
- English（英语）

**切换步骤**
1. 进入个性化设置
2. 点击"界面语言"
3. 选择目标语言
4. 重启应用使更改生效

### 货币符号

**支持的货币**
- ¥（人民币）
- $（美元）
- €（欧元）
- £（英镑）
- ₩（韩元）
- ₹（印度卢比）
- ₽（俄罗斯卢布）
- ฿（泰铢）

**切换步骤**
1. 进入个性化设置
2. 点击"货币符号"
3. 选择目标货币
4. 重启应用使更改生效

### 注意事项

- 语言和货币符号更改后需要重启应用
- 重启前可以继续使用旧设置
- 更改会影响所有金额的显示

## 💾 数据管理

### CSV 数据导入导出

**导出功能**

**用途**
- 备份所有交易数据
- 在其他设备或应用中查看
- 数据分析和报表生成

**导出步骤**
1. 进入数据管理页面
2. 找到"本地数据归档"部分
3. 点击"导出全量账本至 CSV"
4. 选择保存位置
5. 确认导出

**导出文件格式**
- CSV 格式（逗号分隔值）
- 包含字段：ID、日期、类型、金额、分类、备注
- 文件名格式：AccountKeeper_Export_YYYYMMDD.csv

**导入功能**

**用途**
- 从备份恢复数据
- 合并多个设备的数据
- 迁移数据到新设备

**导入步骤**
1. 进入数据管理页面
2. 找到"本地数据归档"部分
3. 点击"导入标准 CSV 备份"
4. 选择之前导出的 CSV 文件
5. 系统自动识别并导入

**数据合并规则**
- ID 重复的交易会被跳过
- 不存在的分类会自动创建
- 已存在的分类不会重复创建
- 显示导入成功的交易数量

### 第三方账单导入

**支持的账单**
- 微信支付账单
- 支付宝账单

**导入步骤**
1. 进入数据管理页面
2. 找到"第三方账单导入"部分
3. 点击"导入微信/支付宝账单"
4. 选择账单 CSV 文件
5. 系统自动识别账单类型
6. 等待导入完成

**账单识别**
- 自动检测账单类型（微信/支付宝）
- 解析交易时间、金额、类型、名称、备注
- 自动匹配或创建分类
- 智能识别收入和支出

**智能处理**
- 退款交易自动识别为收入
- 相同 ID 的交易不会重复导入
- 支持多种日期格式

**注意事项**
- 仅支持标准格式的微信/支付宝账单 CSV
- 需要手动从微信/支付宝导出账单
- 导入后可在"管理已导入的账单文件"中查看

### 本地自动备份

**功能说明**
- 每次添加、删除或修改交易时自动创建备份
- 无需手动操作，自动保护数据

**开启步骤**
1. 进入数据管理页面
2. 找到"本地自动备份安全柜"部分
3. 开启"开启本地自动备份"开关

**保留设置**
- 可设置保留的备份数量（5-50 份）
- 超过限制时自动删除最旧的备份
- 建议根据使用频率设置合适的数量

**查看备份**
- 显示最新自动备份的时间
- 显示最新手动备份的时间
- 如无备份则显示"无自动备份"或"无手动备份"

### 手动备份

**创建备份**
1. 进入数据管理页面
2. 点击"立即创建手动备份"
3. 输入备份名称（可选）
4. 确认创建

**备份用途**
- 重要操作前的手动备份
- 定期数据备份
- 创建特定时间点的快照

**管理备份**
- 点击"备份管理柜"查看所有备份
- 点击"手动"标签查看手动备份
- 点击"自动"标签查看自动备份
- 点击删除图标删除不需要的备份

### 恢复备份

**恢复步骤**
1. 进入数据管理页面
2. 点击"备份管理柜"
3. 选择"手动"或"自动"标签
4. 找到要恢复的备份
5. 点击"恢复"按钮
6. 确认恢复

**恢复效果**
- 覆盖当前所有数据
- 不可撤销
- 建议恢复前先创建手动备份

## 🏷️ 分类管理

### 分类类型

**支出分类**
- 记录所有支出交易
- 预设分类：餐饮、交通、购物、娱乐、医疗、教育等

**收入分类**
- 记录所有收入交易
- 预设分类：工资、奖金、投资收益、兼职收入等

### 查看分类

**界面布局**
- 顶部有两个标签：支出和收入
- 点击标签切换查看不同类型的分类
- 每个分类显示名称
- 默认分类有特殊标记

### 添加分类

**添加步骤**
1. 点击右下角的 + 按钮
2. 输入分类名称
3. 点击"确定"保存

**命名建议**
- 使用简洁明了的名称
- 避免使用特殊字符
- 可以使用中英文
- 示例：早餐、午餐、晚餐、交通费

### 重命名分类

**重命名步骤**
1. 找到要修改的分类
2. 点击右侧的编辑图标（铅笔）
3. 修改分类名称
4. 点击"确定"保存

**注意事项**
- 修改名称会影响所有使用该分类的交易
- 不会影响交易数据本身

### 删除分类

**删除步骤**
1. 找到要删除的自定义分类
2. 点击右侧的删除图标（红色垃圾桶）
3. 确认删除

**限制**
- 默认分类不可删除
- 删除分类后，使用该分类的交易不会受影响
- 但该分类会从分类列表中消失

### 使用技巧

- 创建符合个人习惯的分类
- 分类不要太细，避免难以选择
- 定期清理不常用的分类
- 使用有意义的分类名称

## 💡 使用技巧和最佳实践

### 日常记账习惯

1. **及时记录**：消费后立即记录，避免遗忘
2. **详细备注**：添加备注帮助回忆交易详情
3. **合理分类**：使用分类功能，更好地管理财务

### 数据安全

1. **定期备份**：每周创建一次手动备份
2. **开启自动备份**：确保每次操作都有备份
3. **多设备同步**：使用 CSV 导入导出在设备间同步数据

### 财务分析

1. **定期查看统计**：每月查看一次统计页面
2. **关注趋势**：观察收支趋势，调整消费习惯
3. **识别问题领域**：通过分类占比找出主要支出

### 账单管理

1. **定期导入**：每月导入一次微信/支付宝账单
2. **核对数据**：导入后核对交易是否正确
3. **补充备注**：为账单交易添加有用的备注

### 高级技巧

1. **批量操作**：使用长按进入选择模式，批量删除或编辑
2. **滑动删除**：使用滑动功能快速删除不需要的交易
3. **自定义分类**：创建个性化的分类体系

## ⚠️ 重要注意事项

### 数据安全

- **删除不可恢复**：删除交易后无法撤销，请谨慎操作
- **定期备份**：建议每周创建手动备份
- **设备更换**：更换设备前先导出数据

### 系统要求

- **语言切换**：切换语言或货币符号后需要重启应用
- **存储空间**：确保设备有足够的存储空间用于备份
- **网络要求**：第三方账单导入不需要网络

### 数据限制

- **CSV 格式**：仅支持应用导出的标准 CSV 格式
- **账单格式**：仅支持微信/支付宝的标准账单 CSV 格式
- **备份保留**：自动备份会自动删除旧备份，请设置合适的保留数量

### 兼容性

- **数据合并**：导入 CSV 时，ID 重复的交易会被跳过
- **分类自动创建**：导入时会自动创建缺失的分类
- **默认分类**：预设的分类不可删除，但可以重命名

## 🆘 常见问题

**Q: 如何找回删除的交易？**
A: 删除的交易无法直接恢复，如果有备份可以恢复备份文件。

**Q: 导入账单时部分交易未导入？**
A: 可能是 ID 重复或金额为零，系统会自动跳过这些交易。

**Q: 如何在多个设备间同步数据？**
A: 在一个设备导出 CSV 文件，然后在其他设备导入即可。

**Q: 更改语言后为什么界面没有变化？**
A: 需要重启应用才能使语言更改生效。

**Q: 可以修改预设的分类吗？**
A: 可以重命名预设分类，但不能删除它们。

**Q: 自动备份会占用多少存储空间？**
A: 取决于数据量，可以在设置中查看备份大小。

## 📞 技术支持

如遇到问题或有建议，欢迎联系我们：
- 邮箱：rickymiao63@163.com
- GitHub：https://github.com/miaotenone/AccountKeeper

感谢使用 AccountKeeper！祝您财务管理顺利！""",
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
    defaultCategory = "默认分类",
    addCategory = "新增分类",
    categoryName = "分类名称",
    renameCategory = "重命名分类",
    newName = "新名称",
    deleteCategory = "删除分类",
    deleteCategoryConfirm = "确定要删除自定义分类 \"{name}\" 吗？此操作无法撤销。",
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
    // Asset related
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
    swipeDeleteConfirm = "左滑删除确认",
    swipeDeleteConfirmEnabled = "左滑松开直接弹出确认",
    swipeDeleteConfirmDisabled = "左滑后点击删除再确认",
    // Filter and sort (additional)
    sort = "排序",
    categoryFilter = "分类",
    allCategories = "全部分类",
    timeDescending = "时间（最新优先）",
    timeAscending = "时间（最早优先）",
    amountDescending = "金额（从高到低）",
    amountAscending = "金额（从低到高）",
    // Attachments
    attachments = "附件",
    addAttachment = "添加附件",
    removeAttachment = "移除",
    noAttachments = "暂无附件",
    selectFile = "选择文件",
    fileSizeTooLarge = "文件大小超出限制",
    // Update related
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
    openInBrowser = "浏览器下载"
)

val LocalAppStrings = androidx.compose.runtime.compositionLocalOf { EnStrings }