package com.example.accountkeeper.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.accountkeeper.ui.screens.AboutScreen
import com.example.accountkeeper.ui.screens.AttachmentOverviewScreen
import com.example.accountkeeper.ui.screens.AddEditAssetScreen
import com.example.accountkeeper.ui.screens.AddEditTransactionScreen
import com.example.accountkeeper.ui.screens.AppSettingsScreen
import com.example.accountkeeper.ui.screens.AssetsScreen
import com.example.accountkeeper.ui.screens.BudgetApprovalScreen
import com.example.accountkeeper.ui.screens.BudgetScreen
import com.example.accountkeeper.ui.screens.CategorySettingsScreen
import com.example.accountkeeper.ui.screens.CategoryTransactionsScreen
import com.example.accountkeeper.ui.screens.DataManagementHubScreen
import com.example.accountkeeper.ui.screens.DataManagementScreen
import com.example.accountkeeper.ui.screens.FinancialArchiveScreen
import com.example.accountkeeper.ui.screens.HomeScreen
import com.example.accountkeeper.ui.screens.SearchResultScreen
import com.example.accountkeeper.ui.screens.SettingsScreen
import com.example.accountkeeper.ui.screens.StatisticsScreen
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object StatisticsRoute
@Serializable object AssetsRoute
@Serializable object BudgetRoute
@Serializable object BudgetApprovalRoute
@Serializable data class AddEditTransactionRoute(val transactionId: Long = -1L)
@Serializable data class AddEditAssetRoute(val assetId: Long = -1L)
@Serializable object SettingsRoute
@Serializable object DataManagementRoute
@Serializable object FinancialArchiveRoute
@Serializable object AttachmentOverviewRoute
@Serializable object LegacyDataManagementRoute
@Serializable object AppSettingsRoute
@Serializable object CategorySettingsRoute
@Serializable object AboutRoute
@Serializable data class CategoryTransactionsRoute(val categoryId: Long, val categoryName: String, val startTime: Long, val endTime: Long)
@Serializable data class SearchResultRoute(val query: String)

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    homeVisibilityEventId: Long = 0L,
    homeVisibilityVisible: Boolean = false,
    onTransactionSaved: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToAddTransaction = { navController.navigate(AddEditTransactionRoute()) },
                onNavigateToEditTransaction = { navController.navigate(AddEditTransactionRoute(it)) },
                onNavigateToSearchResult = { navController.navigate(SearchResultRoute(it)) },
                homeVisibilityEventId = homeVisibilityEventId,
                homeVisibilityVisible = homeVisibilityVisible
            )
        }
        composable<StatisticsRoute> { StatisticsScreen(onNavigateToCategoryTransactions = { id, name, start, end -> navController.navigate(CategoryTransactionsRoute(id, name, start, end)) }) }
        composable<AssetsRoute> { AssetsScreen(onNavigateToAddAsset = { navController.navigate(AddEditAssetRoute()) }, onNavigateToEditAsset = { navController.navigate(AddEditAssetRoute(it)) }) }
        composable<BudgetRoute> { BudgetScreen(onNavigateToCategoryTransactions = { id, name, start, end -> navController.navigate(CategoryTransactionsRoute(id, name, start, end)) }, onNavigateToBudgetApproval = { navController.navigate(BudgetApprovalRoute) }) }
        composable<BudgetApprovalRoute> { BudgetApprovalScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<SettingsRoute> { SettingsScreen(onNavigateToDataManagement = { navController.navigate(DataManagementRoute) }, onNavigateToAppSettings = { navController.navigate(AppSettingsRoute) }, onNavigateToCategorySettings = { navController.navigate(CategorySettingsRoute) }, onNavigateToAbout = { navController.navigate(AboutRoute) }) }
        composable<DataManagementRoute> { DataManagementHubScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToLegacyManagement = { navController.navigate(LegacyDataManagementRoute) }, onNavigateToFinancialArchive = { navController.navigate(FinancialArchiveRoute) }, onNavigateToAttachments = { navController.navigate(AttachmentOverviewRoute) }) }
        composable<AttachmentOverviewRoute> { AttachmentOverviewScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<FinancialArchiveRoute> { FinancialArchiveScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<LegacyDataManagementRoute> { DataManagementScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<AppSettingsRoute> { AppSettingsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<AddEditTransactionRoute> { entry ->
            val args = entry.toRoute<AddEditTransactionRoute>()
            AddEditTransactionScreen(
                transactionId = args.transactionId,
                onNavigateBack = { navController.popBackStack() },
                onTransactionSaved = { onTransactionSaved(); navController.popBackStack() }
            )
        }
        composable<AddEditAssetRoute> { entry ->
            val args = entry.toRoute<AddEditAssetRoute>()
            AddEditAssetScreen(assetId = args.assetId, onNavigateBack = { navController.popBackStack() })
        }
        composable<CategorySettingsRoute> { CategorySettingsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<AboutRoute> { AboutScreen(onNavigateBack = { navController.popBackStack() }) }
        composable<CategoryTransactionsRoute> { entry ->
            val args = entry.toRoute<CategoryTransactionsRoute>()
            CategoryTransactionsScreen(categoryId = args.categoryId, categoryName = args.categoryName, startTime = args.startTime, endTime = args.endTime, onNavigateBack = { navController.popBackStack() })
        }
        composable<SearchResultRoute> { entry ->
            val args = entry.toRoute<SearchResultRoute>()
            SearchResultScreen(query = args.query, onNavigateBack = { navController.popBackStack() }, onNavigateToEditTransaction = { navController.navigate(AddEditTransactionRoute(it)) })
        }
    }
}
