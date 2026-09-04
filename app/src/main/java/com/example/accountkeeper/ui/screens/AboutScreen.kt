package com.example.accountkeeper.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.viewmodel.UpdateState
import com.example.accountkeeper.ui.viewmodel.UpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    // 更新状态
    val updateState by updateViewModel.updateState.collectAsState()
    val downloadProgress by updateViewModel.downloadProgress.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    // 获取应用版本名
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            strings.version
        }
    }
    
    // 监听更新状态变化
    LaunchedEffect(updateState) {
        if (updateState is UpdateState.UpdateAvailable) {
            showUpdateDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.about, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) DarkSurface else LightSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .background(if (isDark) DarkBackground else LightBackground)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon with Gradient
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(DarkGradientPrimary)
                        } else {
                            Brush.verticalGradient(LightGradientPrimary)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "AK",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Name
            Text(
                "AccountKeeper",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkOnBackground else LightOnBackground
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Version
            Text(
                versionName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) DarkOnBackground.copy(alpha = 0.7f) else LightOnBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) DarkOnBackground.copy(alpha = 0.1f) else LightOnBackground.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Check Update Card
            AboutCardWithStatus(
                icon = Icons.Default.SystemUpdate,
                title = strings.checkUpdate,
                description = strings.currentVersion + " " + versionName,
                statusContent = {
                    when (updateState) {
                        is UpdateState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is UpdateState.UpdateAvailable -> {
                            Icon(
                                Icons.Default.NewReleases,
                                contentDescription = "New version available",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        is UpdateState.Downloading -> {
                            Text(
                                "$downloadProgress%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        is UpdateState.NoUpdate -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Up to date",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> {}
                    }
                },
                onClick = { updateViewModel.checkUpdate(force = true) },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Help Tutorial Card
            AboutCard(
                icon = Icons.Default.Info,
                title = strings.helpTutorial,
                description = strings.helpTutorialShort,
                isClickable = true,
                onClick = { showHelpDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Feedback Card
            AboutCard(
                icon = Icons.Default.Feedback,
                title = strings.feedback,
                description = strings.feedbackDescription,
                isClickable = true,
                onClick = { showFeedbackDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // GitHub Card
            AboutCard(
                icon = Icons.Default.Star,
                title = strings.github,
                description = "https://github.com/miaotenone/AccountKeeper",
                link = "https://github.com/miaotenone/AccountKeeper",
                isLink = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contact Card
            AboutCard(
                icon = Icons.Default.Email,
                title = strings.contactAuthor,
                description = "rickymiao63@163.com",
                link = "mailto:rickymiao63@163.com",
                isLink = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Credits
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurface else LightSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        strings.poweredBy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) DarkOnBackground.copy(alpha = 0.7f) else LightOnBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        strings.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Help Tutorial Dialog
    if (showHelpDialog) {
        HelpTutorialDialog(
            onDismiss = { showHelpDialog = false },
            isDark = isDark
        )
    }
    
    // Feedback Dialog
    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            strings = strings,
            isDark = isDark
        )
    }
    
    // Update Dialog
    if (showUpdateDialog) {
        UpdateDialog(
            updateState = updateState,
            downloadProgress = downloadProgress,
            currentVersion = versionName,
            onDismiss = { 
                showUpdateDialog = false
                updateViewModel.resetState()
            },
            onDownload = { updateViewModel.downloadUpdate() },
            onInstall = { updateViewModel.installUpdate() },
            onCancelDownload = { updateViewModel.cancelDownload() },
            strings = strings,
            isDark = isDark
        )
    }
}

@Composable
fun AboutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    link: String? = null,
    isLink: Boolean = false,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    isLink -> Modifier.clickable {
                        link?.let { url ->
                            val intent = if (url.startsWith("mailto:")) {
                                Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                            } else {
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            }
                            context.startActivity(intent)
                        }
                    }
                    isClickable -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(DarkGradientPrimary)
                        } else {
                            Brush.verticalGradient(LightGradientPrimary)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AboutCardWithStatus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    statusContent: @Composable () -> Unit = {},
    onClick: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(DarkGradientPrimary)
                        } else {
                            Brush.verticalGradient(LightGradientPrimary)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f)
                )
            }
            
            statusContent()
        }
    }
}

@Composable
fun HelpTutorialDialog(
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val strings = LocalAppStrings.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 600.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkSurface else LightSurface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    strings.helpTutorial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 关于项目
                    HelpSection(title = strings.helpAboutProject, isDark = isDark) {
                        Text(
                            strings.helpAboutDescription1,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) DarkOnBackground else LightOnBackground
                        )
                        Text(
                            strings.helpAboutDescription2,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) DarkOnBackground.copy(alpha = 0.8f) else LightOnBackground.copy(alpha = 0.8f)
                        )
                    }
                    
                    // 首页功能
                    HelpSection(title = strings.helpHomeFeatures, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpBalanceCard,
                            details = listOf(
                                strings.helpBalanceCardDetail1,
                                strings.helpBalanceCardDetail2,
                                strings.helpBalanceCardDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpHomeTimeRange,
                            details = listOf(
                                strings.helpHomeTimeRangeDetail1,
                                strings.helpHomeTimeRangeDetail2,
                                strings.helpHomeTimeRangeDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpHomeFilter,
                            details = listOf(
                                strings.helpHomeFilterDetail1,
                                strings.helpHomeFilterDetail2,
                                strings.helpHomeFilterDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpSearchFeature,
                            details = listOf(
                                strings.helpSearchDetail1,
                                strings.helpSearchDetail2,
                                strings.helpSearchDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpTransactionListOps,
                            details = listOf(
                                strings.helpTxTapDetail,
                                strings.helpTxSwipeDetail,
                                strings.helpTxLongPressDetail,
                                strings.helpTxBatchDelete
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpAddTransaction,
                            details = listOf(
                                strings.helpAddTxDetail1,
                                strings.helpAddTxDetail2,
                                strings.helpAddTxDetail3,
                                strings.helpAddTxDetail4
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 统计分析
                    HelpSection(title = strings.helpStatistics, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpTimeRangeSelection,
                            details = listOf(
                                strings.helpTimeRangeDay,
                                strings.helpTimeRangeWeek,
                                strings.helpTimeRangeMonth,
                                strings.helpTimeRangeYear,
                                strings.helpTimeRangeCustom
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpChartDisplay,
                            details = listOf(
                                strings.helpChartLine,
                                strings.helpChartPie,
                                strings.helpChartRanking
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpCategoryFilter,
                            details = listOf(
                                strings.helpCategoryFilterDetail1,
                                strings.helpCategoryFilterDetail2,
                                strings.helpCategoryFilterDetail3
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 预算管理
                    HelpSection(title = strings.budget, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpBudgetPeriods,
                            details = listOf(
                                strings.helpBudgetPeriodsDetail1,
                                strings.helpBudgetPeriodsDetail2,
                                strings.helpBudgetPeriodsDetail3
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 资产管理
                    HelpSection(title = strings.helpAssetManagement, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpAssetStatistics,
                            details = listOf(
                                strings.helpAssetNetWorth,
                                strings.helpCurrentAssets,
                                strings.helpCurrentLiabilities,
                                strings.helpTransactionBalance
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpAddAssetRecord,
                            details = listOf(
                                strings.helpAddAssetDetail1,
                                strings.helpAddAssetDetail2,
                                strings.helpAddAssetDetail3,
                                strings.helpAddAssetDetail4
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpStatusExplanation,
                            details = listOf(
                                strings.helpStatusInProgress,
                                strings.helpStatusCompleted,
                                strings.helpStatusCancelled
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpAssetAttachment,
                            details = listOf(
                                strings.helpAssetAttachmentDetail1,
                                strings.helpAssetAttachmentDetail2
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 数据管理
                    HelpSection(title = strings.helpDataManagement, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpExportData,
                            details = listOf(
                                strings.helpExportDetail1,
                                strings.helpExportDetail2,
                                strings.helpExportDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpImportData,
                            details = listOf(
                                strings.helpImportDetail1,
                                strings.helpImportDetail2,
                                strings.helpImportDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpBillImport,
                            details = listOf(
                                strings.helpBillImportDetail1,
                                strings.helpBillImportDetail2,
                                strings.helpBillImportDetail3,
                                strings.helpBillImportDetail4,
                                strings.helpBillImportDetail5
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpHowToExportBill,
                            details = listOf(
                                strings.helpExportBillWechat,
                                strings.helpExportBillAlipay,
                                strings.helpExportBillFormat,
                                strings.helpExportBillImport
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpAutoBackup,
                            details = listOf(
                                strings.helpAutoBackupDetail1,
                                strings.helpAutoBackupDetail2,
                                strings.helpAutoBackupDetail3,
                                strings.helpAutoBackupDetail4
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpBillPreview,
                            details = listOf(
                                strings.helpBillPreviewDetail1,
                                strings.helpBillPreviewDetail2
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 分类管理
                    HelpSection(title = strings.helpCategoryManagement, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpCategoryTypes,
                            details = listOf(
                                strings.helpExpenseCategories,
                                strings.helpIncomeCategories,
                                strings.helpAssetCategories
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpManagementOps,
                            details = listOf(
                                strings.helpAddCategory,
                                strings.helpRenameCategory,
                                strings.helpDeleteCategory,
                                strings.helpPresetCategoryNoDelete
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 个性化设置
                    HelpSection(title = strings.helpPersonalization, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpAppearanceSettings,
                            details = listOf(
                                strings.helpDarkMode,
                                strings.helpLightMode,
                                strings.helpLanguage,
                                strings.helpCurrency
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpNotes,
                            details = listOf(
                                strings.helpRestartRequired,
                                strings.helpOldSettingsBeforeRestart
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 使用技巧
                    HelpSection(title = strings.helpTips, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpDailyTips,
                            details = listOf(
                                strings.helpDailyTip1,
                                strings.helpDailyTip2,
                                strings.helpDailyTip3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpDataSafety,
                            details = listOf(
                                strings.helpDataSafetyTip1,
                                strings.helpDataSafetyTip2,
                                strings.helpDataSafetyTip3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpBillImportTips,
                            details = listOf(
                                strings.helpBillImportTip1,
                                strings.helpBillImportTip2,
                                strings.helpBillImportTip3
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 常见问题
                    HelpSection(title = strings.helpFAQ, isDark = isDark) {
                        HelpDetailItem(
                            title = strings.helpFAQRecover,
                            details = listOf(
                                strings.helpFAQRecoverDetail1,
                                strings.helpFAQRecoverDetail2
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpFAQPartialImport,
                            details = listOf(
                                strings.helpFAQPartialImportDetail1,
                                strings.helpFAQPartialImportDetail2,
                                strings.helpFAQPartialImportDetail3
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = strings.helpFAQSync,
                            details = listOf(
                                strings.helpFAQSyncDetail1,
                                strings.helpFAQSyncDetail2,
                                strings.helpFAQSyncDetail3
                            ),
                            isDark = isDark
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) DarkPrimary else LightPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.close, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSection(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) DarkPrimary else LightPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(content = content)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun HelpDetailItem(
    title: String,
    details: List<String>,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) DarkOnBackground else LightOnBackground
        )
        details.forEach { detail ->
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) DarkOnBackground.copy(alpha = 0.7f) else LightOnBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    strings: AppStrings,
    isDark: Boolean
) {
    val context = LocalContext.current
    var problemText by remember { mutableStateOf("") }
    var contactText by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkSurface else LightSurface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Text(
                    strings.feedback,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Problem Description
                Text(
                    strings.problemDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = problemText,
                    onValueChange = { problemText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = {
                        Text(
                            strings.problemDescriptionHint,
                            color = if (isDark) DarkOnBackground.copy(alpha = 0.4f) else LightOnBackground.copy(alpha = 0.4f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) DarkPrimary else LightPrimary,
                        unfocusedBorderColor = if (isDark) DarkOnBackground.copy(alpha = 0.3f) else LightOnBackground.copy(alpha = 0.3f),
                        focusedTextColor = if (isDark) DarkOnBackground else LightOnBackground,
                        unfocusedTextColor = if (isDark) DarkOnBackground else LightOnBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Contact Info
                Text(
                    strings.contactInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = contactText,
                    onValueChange = { contactText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            strings.contactInfoHint,
                            color = if (isDark) DarkOnBackground.copy(alpha = 0.4f) else LightOnBackground.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) DarkPrimary else LightPrimary,
                        unfocusedBorderColor = if (isDark) DarkOnBackground.copy(alpha = 0.3f) else LightOnBackground.copy(alpha = 0.3f),
                        focusedTextColor = if (isDark) DarkOnBackground else LightOnBackground,
                        unfocusedTextColor = if (isDark) DarkOnBackground else LightOnBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) DarkOnBackground else LightOnBackground
                        )
                    ) {
                        Text(strings.cancel)
                    }
                    
                    // Send Button
                    Button(
                        onClick = {
                            if (problemText.isNotBlank()) {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:rickymiao63@163.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "[AccountKeeper Feedback]")
                                    putExtra(Intent.EXTRA_TEXT, buildString {
                                        append("问题描述：\n")
                                        append(problemText)
                                        append("\n\n")
                                        if (contactText.isNotBlank()) {
                                            append("联系方式：")
                                            append(contactText)
                                        }
                                    })
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, strings.sendFeedback))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) DarkPrimary else LightPrimary
                        ),
                        enabled = problemText.isNotBlank()
                    ) {
                        Text(strings.sendFeedback, color = Color.White)
                    }
                }
            }
        }
    }
}
