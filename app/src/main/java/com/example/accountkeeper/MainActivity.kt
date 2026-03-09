package com.example.accountkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.accountkeeper.ui.navigation.AppNavigation
import com.example.accountkeeper.ui.navigation.AssetsRoute
import com.example.accountkeeper.ui.navigation.HomeRoute
import com.example.accountkeeper.ui.navigation.SettingsRoute
import com.example.accountkeeper.ui.navigation.StatisticsRoute
import com.example.accountkeeper.ui.theme.AccountKeeperTheme
import com.example.accountkeeper.ui.theme.EnStrings
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.theme.ZhStrings
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.compositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel

val LocalCurrencySymbol = compositionLocalOf { "¥" }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appSettings by settingsViewModel.appSettings.collectAsState()

            // TODO: In a real app, you can use appSettings.isDarkMode to override system theme
            // and appSettings.language to override Locale
            val strings = if (appSettings.language == "zh") ZhStrings else EnStrings
            
            CompositionLocalProvider(
                LocalCurrencySymbol provides appSettings.currencySymbol,
                LocalAppStrings provides strings
            ) {
                AccountKeeperTheme(darkTheme = appSettings.isDarkMode) {
                    AccountKeeperMainApp()
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Any
) {
    HOME("Home", Icons.Default.Home, HomeRoute),
    STATISTICS("Statistics", Icons.Default.List, StatisticsRoute),
    ASSETS("Assets", Icons.Default.Wallet, AssetsRoute),
    SETTINGS("Settings", Icons.Default.Settings, SettingsRoute),
}

@Composable
fun AccountKeeperMainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 获取当前路由名称（去掉参数部分）
    val currentRouteName = currentDestination?.route?.substringBefore("?")?.substringBefore("/")
    
    // 提取简单类名（去掉包名）
    val simpleRouteName = currentRouteName?.substringAfterLast(".")

    // 定义子路由归属关系：子路由名称 -> 父页面
    val routeParentMap = mapOf(
        "AddEditAssetRoute" to AppDestinations.ASSETS,
        "AddEditTransactionRoute" to AppDestinations.HOME,
        "SearchResultRoute" to AppDestinations.HOME,
        "CategoryTransactionsRoute" to AppDestinations.STATISTICS,
        "DataManagementRoute" to AppDestinations.SETTINGS,
        "AppSettingsRoute" to AppDestinations.SETTINGS,
        "CategorySettingsRoute" to AppDestinations.SETTINGS,
        "AboutRoute" to AppDestinations.SETTINGS,
    )

    // 首先检查是否是子路由，然后检查是否是主路由
    val currentSelected = routeParentMap[simpleRouteName] 
        ?: AppDestinations.entries.find { 
            it.route::class.simpleName == simpleRouteName 
        } 
        ?: AppDestinations.HOME

    val strings = LocalAppStrings.current

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                val localizedLabel = when (destination) {
                    AppDestinations.HOME -> strings.home
                    AppDestinations.STATISTICS -> strings.statistics
                    AppDestinations.ASSETS -> strings.assets
                    AppDestinations.SETTINGS -> strings.settings
                }
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = localizedLabel
                        )
                    },
                    label = { Text(localizedLabel) },
                    selected = destination == currentSelected,
                    onClick = {
                        // Always pop back to the start destination to clear all sub-pages
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AppNavigation(
                modifier = Modifier.padding(innerPadding),
                navController = navController
            )
        }
    }
}
