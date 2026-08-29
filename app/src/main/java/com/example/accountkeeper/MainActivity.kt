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
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.accountkeeper.ui.navigation.AppNavigation
import com.example.accountkeeper.ui.navigation.AssetsRoute
import com.example.accountkeeper.ui.navigation.BudgetRoute
import com.example.accountkeeper.ui.navigation.HomeRoute
import com.example.accountkeeper.ui.navigation.SettingsRoute
import com.example.accountkeeper.ui.navigation.StatisticsRoute
import com.example.accountkeeper.ui.theme.AccountKeeperTheme
import com.example.accountkeeper.ui.theme.EnStrings
import com.example.accountkeeper.ui.theme.LocalAppStrings
import com.example.accountkeeper.ui.theme.ZhStrings
import com.example.accountkeeper.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

val LocalCurrencySymbol = compositionLocalOf { "¥" }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appSettings by settingsViewModel.appSettings.collectAsState()
            val strings = if (appSettings.language == "zh") ZhStrings else EnStrings
            CompositionLocalProvider(LocalCurrencySymbol provides appSettings.currencySymbol, LocalAppStrings provides strings) {
                AccountKeeperTheme(darkTheme = appSettings.isDarkMode) { AccountKeeperMainApp() }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector, val route: Any) {
    HOME("Home", Icons.Default.Home, HomeRoute),
    STATISTICS("Statistics", Icons.Default.List, StatisticsRoute),
    ASSETS("Assets", Icons.Default.Wallet, AssetsRoute),
    BUDGET("Budget", Icons.Default.Savings, BudgetRoute),
    SETTINGS("Settings", Icons.Default.Settings, SettingsRoute),
}

@Composable
fun AccountKeeperMainApp() {
    val navController = rememberNavController()
    var homeVisibilityEventId by remember { mutableStateOf(0L) }
    var homeVisibilityVisible by remember { mutableStateOf(false) }
    val emitHomeVisibility: (Boolean) -> Unit = { visible ->
        homeVisibilityVisible = visible
        homeVisibilityEventId += 1L
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) emitHomeVisibility(false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val simpleRouteName = currentDestination?.route?.substringBefore("?")?.substringBefore("/")?.substringAfterLast(".")
    val routeParentMap = mapOf(
        "AddEditAssetRoute" to AppDestinations.ASSETS,
        "AddEditTransactionRoute" to AppDestinations.HOME,
        "SearchResultRoute" to AppDestinations.HOME,
        "CategoryTransactionsRoute" to AppDestinations.STATISTICS,
        "BudgetApprovalRoute" to AppDestinations.BUDGET,
        "DataManagementRoute" to AppDestinations.SETTINGS,
        "LegacyDataManagementRoute" to AppDestinations.SETTINGS,
        "AppSettingsRoute" to AppDestinations.SETTINGS,
        "CategorySettingsRoute" to AppDestinations.SETTINGS,
        "AboutRoute" to AppDestinations.SETTINGS
    )
    val currentSelected = routeParentMap[simpleRouteName] ?: AppDestinations.entries.find { it.route::class.simpleName == simpleRouteName } ?: AppDestinations.HOME
    var previousMainDestination by remember { mutableStateOf<AppDestinations?>(null) }
    LaunchedEffect(currentSelected) {
        if (previousMainDestination != null && previousMainDestination != AppDestinations.HOME && currentSelected == AppDestinations.HOME) {
            emitHomeVisibility(true)
        }
        previousMainDestination = currentSelected
    }
    val strings = LocalAppStrings.current

    NavigationSuiteScaffold(navigationSuiteItems = {
        AppDestinations.entries.forEach { destination ->
            val label = when (destination) {
                AppDestinations.HOME -> strings.home
                AppDestinations.STATISTICS -> strings.statistics
                AppDestinations.ASSETS -> strings.assets
                AppDestinations.BUDGET -> strings.budget
                AppDestinations.SETTINGS -> strings.settings
            }
            item(icon = { Icon(destination.icon, contentDescription = label) }, label = { Text(label) }, selected = destination == currentSelected, onClick = {
                val currentBackStack = navController.currentBackStack.value
                var poppedSubScreens = false
                for (entry in currentBackStack.reversed()) {
                    val route = entry.destination.route?.substringBefore("?")?.substringBefore("/")?.substringAfterLast(".")
                    if (routeParentMap[route] == destination) {
                        navController.popBackStack()
                        poppedSubScreens = true
                    } else if (poppedSubScreens) break
                }
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                    launchSingleTop = true

                }
            })
        }
    }) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AppNavigation(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                homeVisibilityEventId = homeVisibilityEventId,
                homeVisibilityVisible = homeVisibilityVisible,
                onTransactionSaved = { emitHomeVisibility(true) }
            )
        }
    }
}
