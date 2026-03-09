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
import com.example.accountkeeper.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // 获取应用版本名
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            strings.version
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
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(if (isDark) DarkBackground else LightBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Icon with Gradient
            Box(
                modifier = Modifier
                    .size(100.dp)
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
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // App Name
            Text(
                "AccountKeeper",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkOnBackground else LightOnBackground
            )

            // Version
            Text(
                versionName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDark) DarkOnBackground.copy(alpha = 0.7f) else LightOnBackground.copy(alpha = 0.7f)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = if (isDark) DarkOnBackground.copy(alpha = 0.1f) else LightOnBackground.copy(alpha = 0.1f)
            )

            // Help Tutorial Card
            AboutCard(
                icon = Icons.Default.Info,
                title = strings.helpTutorial,
                description = strings.helpTutorialShort,
                isClickable = true,
                onClick = { showHelpDialog = true }
            )

            // GitHub Card
            AboutCard(
                icon = Icons.Default.Star,
                title = strings.github,
                description = "https://github.com/miaotenone/AccountKeeper",
                link = "https://github.com/miaotenone/AccountKeeper",
                isLink = true
            )

            // Contact Card
            AboutCard(
                icon = Icons.Default.Email,
                title = strings.contactAuthor,
                description = "rickymiao63@163.com",
                link = "mailto:rickymiao63@163.com",
                isLink = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                .fillMaxWidth(0.9f)
                .heightIn(max = 500.dp),
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
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "📖 关于项目",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    Text(
                        "AccountKeeper 是一款简洁易用的个人财务管理应用，帮助您轻松记录和管理日常收支。支持滑动删除、批量操作、账单导入等丰富功能。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) DarkOnBackground else LightOnBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "🏠 首页功能",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "💰 余额卡片",
                        "显示总余额、总收入、总支出，支持本月/总资产切换"
                    )
                    
                    HelpItem(
                        "📝 交易列表",
                        "按日期分组显示，支持点击编辑、滑动删除、长按批量选择"
                    )
                    
                    HelpItem(
                        "➕ 快速添加",
                        "点击右下角 + 按钮快速添加交易"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "📊 统计分析",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "⏰ 时间范围",
                        "支持日、周、月、年及自定义日期范围"
                    )
                    
                    HelpItem(
                        "📈 趋势图表",
                        "折线图显示收支趋势，饼图显示分类占比"
                    )
                    
                    HelpItem(
                        "🏆 分类排行",
                        "按金额排序显示各分类的支出/收入"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "💾 数据管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "📤 CSV 导出",
                        "导出全量账本数据到 CSV 文件"
                    )
                    
                    HelpItem(
                        "📥 CSV 导入",
                        "导入标准 CSV 备份文件，自动合并数据"
                    )
                    
                    HelpItem(
                        "🧾 账单导入",
                        "支持微信和支付宝账单 CSV 文件导入"
                    )
                    
                    HelpItem(
                        "🔐 自动备份",
                        "每次操作自动创建备份，可设置保留数量"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "🏷️ 分类管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "➕ 添加分类",
                        "创建自定义的收入和支出分类"
                    )
                    
                    HelpItem(
                        "✏️ 重命名",
                        "修改分类名称"
                    )
                    
                    HelpItem(
                        "🗑️ 删除",
                        "删除自定义分类（预设分类不可删除）"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "⚙️ 个性化设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "🌓 主题切换",
                        "支持深色/浅色主题，可跟随系统"
                    )
                    
                    HelpItem(
                        "🌐 语言切换",
                        "支持中文和英文"
                    )
                    
                    HelpItem(
                        "💱 货币符号",
                        "支持多种货币符号（¥、$、€等）"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "💡 使用技巧",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "1. 滑动删除",
                        "向左滑动交易卡片显示删除按钮"
                    )
                    
                    HelpItem(
                        "2. 批量操作",
                        "长按交易进入选择模式，批量删除或编辑"
                    )
                    
                    HelpItem(
                        "3. 定期备份",
                        "每周创建手动备份，确保数据安全"
                    )
                    
                    HelpItem(
                        "4. 账单导入",
                        "每月导入微信/支付宝账单，自动记录交易"
                    )
                    
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
fun HelpItem(
    title: String,
    description: String
) {
    val isDark = isSystemInDarkTheme()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) DarkOnBackground else LightOnBackground
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) DarkOnBackground.copy(alpha = 0.7f) else LightOnBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
