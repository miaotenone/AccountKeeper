package com.example.accountkeeper.ui.screens

import android.content.Intent
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
    var showHelpDialog by remember { mutableStateOf(false) }
    
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
                strings.version,
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
                description = strings.helpTutorialDescription,
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
                    "AccountKeeper 使用教程",
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
                        "AccountKeeper 是一款简洁易用的个人财务管理应用，帮助您轻松记录和管理日常收支。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) DarkOnBackground else LightOnBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "✨ 主要功能",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "📊 首页统计",
                        "查看总资产、本月收支统计和最近交易记录"
                    )
                    
                    HelpItem(
                        "📈 数据统计",
                        "按日、周、月、年查看收支趋势和分类排行"
                    )
                    
                    HelpItem(
                        "💰 记录交易",
                        "快速添加收入和支出，支持自定义分类和备注"
                    )
                    
                    HelpItem(
                        "💾 数据备份",
                        "支持本地自动备份和手动备份，CSV 导入导出"
                    )
                    
                    HelpItem(
                        "🏷️ 分类管理",
                        "自定义收入和支出分类，灵活管理"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "🚀 快速上手",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkPrimary else LightPrimary
                    )
                    
                    HelpItem(
                        "1. 首次使用",
                        "点击首页的 + 号按钮开始记录第一笔交易"
                    )
                    
                    HelpItem(
                        "2. 添加分类",
                        "在设置中管理分类，创建适合您的收支类别"
                    )
                    
                    HelpItem(
                        "3. 查看统计",
                        "切换到统计页面，了解您的消费习惯"
                    )
                    
                    HelpItem(
                        "4. 备份数据",
                        "定期创建手动备份，确保数据安全"
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
                        Text("知道了", color = Color.White)
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
