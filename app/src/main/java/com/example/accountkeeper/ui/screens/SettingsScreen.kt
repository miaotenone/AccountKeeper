package com.example.accountkeeper.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.ui.theme.*
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel

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
                                "Customize your experience",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Quick Settings Card
            PremiumQuickSettingsCard(
                isDarkMode = appSettings.isDarkMode,
                language = appSettings.language,
                currency = appSettings.currencySymbol,
                onToggleDarkMode = { settingsViewModel.updateTheme(!appSettings.isDarkMode) },
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
                    description = "Import, export and backup your data",
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
                    description = "Theme, language and currency preferences",
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
                    description = "Version info and help tutorial",
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PremiumQuickSettingsCard(
    isDarkMode: Boolean,
    language: String,
    currency: String,
    onToggleDarkMode: () -> Unit,
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
                    "Quick Settings",
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
                        value = if (isDarkMode) "On" else "Off",
                        color = Color.White.copy(alpha = 0.85f),
                        onClick = onToggleDarkMode
                    )
                    QuickSettingItem(
                        icon = Icons.Default.Language,
                        label = strings.language,
                        value = language.uppercase(),
                        color = Color.White.copy(alpha = 0.85f),
                        onClick = {}
                    )
                    QuickSettingItem(
                        icon = Icons.Default.AttachMoney,
                        label = strings.currencySymbol,
                        value = currency,
                        color = Color.White.copy(alpha = 0.85f),
                        onClick = {}
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