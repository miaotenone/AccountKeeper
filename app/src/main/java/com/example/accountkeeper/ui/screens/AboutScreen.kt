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
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }
    
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
    val isDark = isSystemInDarkTheme()
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
                    HelpSection(title = "📖 关于项目", isDark = isDark) {
                        Text(
                            "AccountKeeper 是一款简洁易用的个人财务管理应用，帮助您轻松记录和管理日常收支。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) DarkOnBackground else LightOnBackground
                        )
                        Text(
                            "主要功能：收支记账、资产管理、统计分析、账单导入、数据备份等。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) DarkOnBackground.copy(alpha = 0.8f) else LightOnBackground.copy(alpha = 0.8f)
                        )
                    }
                    
                    // 首页功能
                    HelpSection(title = "🏠 首页功能", isDark = isDark) {
                        HelpDetailItem(
                            title = "💰 余额卡片",
                            details = listOf(
                                "显示总余额 = 总收入 - 总支出",
                                "点击「本月/总资产」切换查看范围",
                                "本月：仅显示当月数据",
                                "总资产：显示所有历史数据"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "🔍 搜索功能",
                            details = listOf(
                                "点击顶部搜索图标展开搜索栏",
                                "输入关键词搜索备注或分类名",
                                "支持模糊匹配，快速定位交易"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "📝 交易列表操作",
                            details = listOf(
                                "点击卡片：进入编辑页面",
                                "左滑卡片：显示删除按钮",
                                "长按卡片：进入多选模式",
                                "多选模式可批量删除"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "➕ 添加交易",
                            details = listOf(
                                "点击右下角 + 按钮",
                                "输入金额 → 选择类型 → 选择分类",
                                "可设置日期和备注",
                                "点击保存完成添加"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 统计分析
                    HelpSection(title = "📊 统计分析", isDark = isDark) {
                        HelpDetailItem(
                            title = "⏰ 时间范围选择",
                            details = listOf(
                                "日：查看当天数据",
                                "周：本周（周一至周日）",
                                "月：本月（1日至月末）",
                                "年：本年度数据",
                                "自定义：选择起止日期"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "📈 图表展示",
                            details = listOf(
                                "折线图：收支趋势变化",
                                "饼图：各分类占比",
                                "排行榜：分类金额排序"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "🏆 分类筛选",
                            details = listOf(
                                "点击分类排行榜中的分类卡片",
                                "查看该分类下所有交易明细",
                                "支持按时间范围筛选"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 资产管理
                    HelpSection(title = "💵 资产管理", isDark = isDark) {
                        HelpDetailItem(
                            title = "📊 资产统计",
                            details = listOf(
                                "净资产 = 总资产 - 总负债",
                                "总资产：借出款项（他人欠我）",
                                "总负债：借入款项（我欠他人）",
                                "交易余额：记账余额"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "📝 添加资产记录",
                            details = listOf(
                                "点击 + 添加借贷记录",
                                "填写金额、对方、日期",
                                "选择分类（借出/借入）",
                                "可添加附件作为凭证"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "✅ 状态说明",
                            details = listOf(
                                "进行中：借贷尚未结清",
                                "已完成：已还清结账",
                                "已取消：作废的记录"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 数据管理
                    HelpSection(title = "💾 数据管理", isDark = isDark) {
                        HelpDetailItem(
                            title = "📤 导出数据",
                            details = listOf(
                                "导出 ZIP 格式备份文件",
                                "包含交易、资产、附件",
                                "可用于迁移或备份"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "📥 导入数据",
                            details = listOf(
                                "导入 ZIP 备份文件",
                                "自动合并，重复 ID 跳过",
                                "缺失分类自动创建"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "🧾 微信/支付宝账单导入",
                            details = listOf(
                                "从微信/支付宝导出账单",
                                "支持 Excel(.xlsx) 和 CSV 格式",
                                "自动识别交易类型和分类",
                                "退款交易智能处理（已退款项不导入）",
                                "可在「管理已导入账单」中复原"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "🔐 自动备份",
                            details = listOf(
                                "每次增删改自动创建备份",
                                "可设置保留数量（5-50）",
                                "超出自动删除最旧备份",
                                "建议同时定期手动备份"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 分类管理
                    HelpSection(title = "🏷️ 分类管理", isDark = isDark) {
                        HelpDetailItem(
                            title = "分类类型",
                            details = listOf(
                                "支出分类：餐饮、交通、购物等",
                                "收入分类：工资、奖金、理财等",
                                "资产分类：借出、借入"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "管理操作",
                            details = listOf(
                                "添加：创建自定义分类",
                                "重命名：修改分类名称",
                                "删除：仅可删除自定义分类",
                                "预设分类不可删除"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 个性化设置
                    HelpSection(title = "⚙️ 个性化设置", isDark = isDark) {
                        HelpDetailItem(
                            title = "外观设置",
                            details = listOf(
                                "深色模式：护眼，适合夜间",
                                "浅色模式：清爽，适合白天",
                                "语言：中文/English",
                                "货币：¥ $ € £ ₩ ₹ ₽ ฿"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "注意事项",
                            details = listOf(
                                "语言和货币修改后需重启生效",
                                "重启前仍使用旧设置"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 使用技巧
                    HelpSection(title = "💡 使用技巧", isDark = isDark) {
                        HelpDetailItem(
                            title = "日常记账建议",
                            details = listOf(
                                "消费后立即记录，避免遗忘",
                                "善用备注记录详细信息",
                                "定期查看统计，了解消费习惯"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "数据安全建议",
                            details = listOf(
                                "开启自动备份功能",
                                "每周创建一次手动备份",
                                "更换手机前先导出数据"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "账单导入技巧",
                            details = listOf(
                                "每月导入一次微信/支付宝账单",
                                "导入后检查分类是否正确",
                                "可手动调整错误的分类"
                            ),
                            isDark = isDark
                        )
                    }
                    
                    // 常见问题
                    HelpSection(title = "❓ 常见问题", isDark = isDark) {
                        HelpDetailItem(
                            title = "交易删除后能恢复吗？",
                            details = listOf(
                                "删除操作不可撤销",
                                "如有备份可从备份恢复"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "账单导入部分交易未导入？",
                            details = listOf(
                                "ID 重复的交易会跳过",
                                "已退款的原交易会被排除",
                                "金额为0的交易不导入"
                            ),
                            isDark = isDark
                        )
                        HelpDetailItem(
                            title = "如何同步到其他设备？",
                            details = listOf(
                                "导出 ZIP 备份文件",
                                "在新设备导入该文件",
                                "数据自动合并"
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
