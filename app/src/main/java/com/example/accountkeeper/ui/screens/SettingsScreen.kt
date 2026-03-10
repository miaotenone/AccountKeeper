package com.example.accountkeeper.ui.screens

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import com.example.accountkeeper.ui.viewmodel.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToAppSettings: () -> Unit = {},
    onNavigateToCategorySettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val strings = LocalAppStrings.current
    val appSettings by settingsViewModel.appSettings.collectAsState()
    val isDark = isSystemInDarkTheme()

    Scaffold(
        topBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                strings.settings,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                strings.customizeExperience,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick Settings Card
            PremiumQuickSettingsCard(
                isDarkMode = appSettings.isDarkMode,
                language = appSettings.language,
                currency = appSettings.currencySymbol,
                onToggleDarkMode = { settingsViewModel.updateTheme(!appSettings.isDarkMode) },
                onToggleLanguage = {
                    val newLang = if (appSettings.language == "zh") "en" else "zh"
                    settingsViewModel.updateLanguage(newLang)
                },
                onToggleCurrency = {
                    val currencies = listOf("¥", "$", "€", "£", "₩", "₹", "₽", "฿")
                    val currentIndex = currencies.indexOf(appSettings.currencySymbol)
                    val nextIndex = (currentIndex + 1) % currencies.size
                    settingsViewModel.updateCurrency(currencies[nextIndex])
                },
                strings = strings
            )

            // Main Settings Groups
            SettingsSection(
                title = strings.dataManagement,
                icon = Icons.Default.CloudUpload,
                color = if (isDark) Color(0xFF5BD9CA) else Color(0xFF00B5A4)
            ) {
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    title = strings.dataManagement,
                    description = strings.dataManagementDescription,
                    onClick = onNavigateToDataManagement
                )
            }

            SettingsSection(
                title = strings.generalSettings,
                icon = Icons.Default.Settings,
                color = if (isDark) Color(0xFFFF6B6B) else Color(0xFFE63946)
            ) {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = strings.generalSettings,
                    description = strings.generalSettingsDescription,
                    onClick = onNavigateToAppSettings
                )
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = strings.categoryManagement,
                    description = strings.categoryManagementDescription,
                    onClick = onNavigateToCategorySettings
                )
            }

            SettingsSection(
                title = strings.about,
                icon = Icons.Default.Info,
                color = if (isDark) Color(0xFF9D4EDD) else Color(0xFF7209B7)
            ) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = strings.about,
                    description = strings.aboutDescription,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    downloadProgress: Int,
    currentVersion: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onCancelDownload: () -> Unit,
    strings: AppStrings,
    isDark: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = updateState !is UpdateState.Downloading,
            dismissOnClickOutside = updateState !is UpdateState.Downloading
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkSurface else LightSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    strings.checkUpdate,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkOnBackground else LightOnBackground
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                when (updateState) {
                    is UpdateState.UpdateAvailable -> {
                        // 有新版本
                        UpdateAvailableContent(
                            updateInfo = updateState.info,
                            currentVersion = currentVersion,
                            isDark = isDark
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(strings.cancel)
                            }
                            Button(
                                onClick = onDownload,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) DarkPrimary else LightPrimary
                                )
                            ) {
                                Text(strings.downloadNow)
                            }
                        }
                    }
                    
                    is UpdateState.Downloading -> {
                        // 下载中
                        DownloadingContent(
                            progress = downloadProgress,
                            isDark = isDark
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(strings.cancelDownload)
                        }
                    }
                    
                    is UpdateState.DownloadComplete -> {
                        // 下载完成
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF4CAF50) else Color(0xFF4CAF50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Download complete",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            strings.downloadComplete,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkOnBackground else LightOnBackground
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = onInstall,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) DarkPrimary else LightPrimary
                            )
                        ) {
                            Text(strings.installNow)
                        }
                    }
                    
                    is UpdateState.NoUpdate -> {
                        // 已是最新版本
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF4CAF50) else Color(0xFF4CAF50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Up to date",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            strings.alreadyLatest,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkOnBackground else LightOnBackground
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) DarkPrimary else LightPrimary
                            )
                        ) {
                            Text(strings.close)
                        }
                    }
                    
                    is UpdateState.Error -> {
                        // 错误
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            strings.checkUpdateFailed,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkOnBackground else LightOnBackground
                        )
                        
                        Text(
                            updateState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) DarkOnBackground.copy(alpha = 0.7f) else LightOnBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) DarkPrimary else LightPrimary
                            )
                        ) {
                            Text(strings.close)
                        }
                    }
                    
                    else -> {
                        // 默认状态
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateAvailableContent(
    updateInfo: com.example.accountkeeper.utils.UpdateInfo,
    currentVersion: String,
    isDark: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) Color(0xFF2196F3) else Color(0xFF2196F3)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.NewReleases,
                contentDescription = "New version",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                currentVersion,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f)
            )
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "To",
                modifier = Modifier.size(16.dp),
                tint = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f)
            )
            Text(
                "v${updateInfo.versionName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkPrimary else LightPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "${updateInfo.fileSizeFormatted}",
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 更新日志
        if (updateInfo.releaseNotes.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "更新内容",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkOnBackground else LightOnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        updateInfo.releaseNotes.take(500),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) DarkOnBackground.copy(alpha = 0.8f) else LightOnBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadingContent(
    progress: Int,
    isDark: Boolean
) {
    val strings = LocalAppStrings.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = if (isDark) DarkPrimary else LightPrimary,
                trackColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
            )
            Text(
                "$progress%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDark) DarkOnBackground else LightOnBackground
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            strings.downloading,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDark) DarkOnBackground else LightOnBackground
        )
        
        Text(
            strings.downloadHint,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDark) DarkOnBackground.copy(alpha = 0.6f) else LightOnBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PremiumQuickSettingsCard(
    isDarkMode: Boolean,
    language: String,
    currency: String,
    onToggleDarkMode: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleCurrency: () -> Unit,
    strings: AppStrings
) {
    val isDark = isSystemInDarkTheme()
    val gradient = if (isDark) {
        Brush.linearGradient(DarkGradientPrimary)
    } else {
        Brush.linearGradient(LightGradientPrimary)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            // Decorative circles
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-50).dp, y = (-50).dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 60.dp, y = 40.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    strings.quickSettings,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickSettingItem(
                        icon = Icons.Default.DarkMode,
                        label = strings.darkMode,
                        value = if (strings.language == "界面语言") {
                            if (isDarkMode) "开" else "关"
                        } else {
                            if (isDarkMode) "On" else "Off"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        onClick = onToggleDarkMode
                    )
                    QuickSettingItem(
                        icon = Icons.Default.Language,
                        label = strings.language,
                        value = if (language == "zh") "中文" else "English",
                        color = Color.White.copy(alpha = 0.85f),
                        onClick = onToggleLanguage
                    )
                    QuickSettingItem(
                        icon = Icons.Default.AttachMoney,
                        label = strings.currencySymbol,
                        value = currency,
                        color = Color.White.copy(alpha = 0.85f),
                        onClick = onToggleCurrency
                    )
                }
            }
        }
    }
}

@Composable
fun QuickSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDark) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}