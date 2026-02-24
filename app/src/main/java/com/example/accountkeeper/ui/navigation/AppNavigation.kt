package com.example.accountkeeper.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.accountkeeper.ui.screens.AddEditTransactionScreen
import com.example.accountkeeper.ui.screens.HomeScreen
import com.example.accountkeeper.ui.screens.ImportExportScreen
import com.example.accountkeeper.ui.screens.StatisticsScreen
import com.example.accountkeeper.ui.screens.CategorySettingsScreen
import com.example.accountkeeper.ui.screens.AboutScreen
import com.example.accountkeeper.ui.screens.SettingsScreen
import com.example.accountkeeper.ui.screens.DataManagementScreen
import com.example.accountkeeper.ui.screens.AppSettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object StatisticsRoute

@Serializable
data class AddEditTransactionRoute(val transactionId: Long = -1L)

@Serializable
object SettingsRoute

@Serializable
object DataManagementRoute

@Serializable
object AppSettingsRoute

@Serializable
object CategorySettingsRoute

@Serializable
object AboutRoute

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Statistics : BottomNavItem("statistics", "Statistics", Icons.Default.Settings)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Statistics,
    BottomNavItem.Settings
)

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToAddTransaction = {
                    navController.navigate(AddEditTransactionRoute())
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(AddEditTransactionRoute(transactionId))
                }
            )
        }
        composable<StatisticsRoute> {
            StatisticsScreen()
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateToDataManagement = {
                    navController.navigate(DataManagementRoute)
                },
                onNavigateToAppSettings = {
                    navController.navigate(AppSettingsRoute)
                },
                onNavigateToCategorySettings = {
                    navController.navigate(CategorySettingsRoute)
                },
                onNavigateToAbout = {
                    navController.navigate(AboutRoute)
                }
            )
        }
        composable<DataManagementRoute> {
            DataManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<AppSettingsRoute> {
            AppSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<AddEditTransactionRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<AddEditTransactionRoute>()
            AddEditTransactionScreen(
                transactionId = args.transactionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<CategorySettingsRoute> {
            CategorySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<AboutRoute> {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
