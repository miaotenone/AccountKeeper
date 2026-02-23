package com.example.accountkeeper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.accountkeeper.ui.screens.AddEditTransactionScreen
import com.example.accountkeeper.ui.screens.HomeScreen
import com.example.accountkeeper.ui.screens.ImportExportScreen
import com.example.accountkeeper.ui.screens.StatisticsScreen
import com.example.accountkeeper.ui.screens.CategorySettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class AddEditTransactionRoute(val transactionId: Long = -1L)

@Serializable
object StatisticsRoute

@Serializable
object ImportExportRoute

@Serializable
object CategorySettingsRoute

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToAddTransaction = { navController.navigate(AddEditTransactionRoute()) },
                onNavigateToEditTransaction = { transactionId -> navController.navigate(AddEditTransactionRoute(transactionId)) }
            )
        }
        composable<AddEditTransactionRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<AddEditTransactionRoute>()
            AddEditTransactionScreen(
                transactionId = args.transactionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<StatisticsRoute> {
            StatisticsScreen()
        }
        composable<ImportExportRoute> {
            ImportExportScreen(
                onNavigateToCategorySettings = {
                    navController.navigate(CategorySettingsRoute)
                }
            )
        }
        composable<CategorySettingsRoute> {
            CategorySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
