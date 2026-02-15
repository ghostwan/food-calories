package com.ghostwan.snapcal.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ghostwan.snapcal.SnapCalApp
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.presentation.FoodAnalysisViewModel
import com.ghostwan.snapcal.presentation.dashboard.DashboardScreen
import com.ghostwan.snapcal.presentation.dashboard.DashboardViewModel
import com.ghostwan.snapcal.presentation.history.HistoryScreen
import com.ghostwan.snapcal.presentation.history.HistoryViewModel
import com.ghostwan.snapcal.presentation.home.HomeScreen
import com.ghostwan.snapcal.presentation.profile.ProfileScreen
import com.ghostwan.snapcal.presentation.profile.ProfileViewModel
import com.ghostwan.snapcal.presentation.result.ResultScreen

private sealed class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Dashboard : BottomNavItem("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Scanner : BottomNavItem("home", R.string.nav_scanner, Icons.Default.Restaurant)
    data object Profile : BottomNavItem("profile", R.string.nav_profile, Icons.Default.Person)
}

private val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Scanner,
    BottomNavItem.Profile
)

// Routes that should NOT show the bottom bar
private val noBottomBarRoutes = setOf("result", "history")

@Composable
fun SnapCalNavGraph() {
    val context = LocalContext.current
    val app = context.applicationContext as SnapCalApp

    val navController = rememberNavController()

    val foodAnalysisViewModel: FoodAnalysisViewModel = viewModel(
        factory = FoodAnalysisViewModel.provideFactory(
            analyzeFoodUseCase = app.analyzeFoodUseCase,
            correctAnalysisUseCase = app.correctAnalysisUseCase,
            settingsRepository = app.settingsRepository,
            usageRepository = app.usageRepository,
            saveMealUseCase = app.saveMealUseCase
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in noBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.provideFactory(
                        getDailyNutritionUseCase = app.getDailyNutritionUseCase,
                        userProfileRepository = app.userProfileRepository,
                        mealRepository = app.mealRepository
                    )
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onScanMeal = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onHistory = {
                        navController.navigate("history")
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    viewModel = foodAnalysisViewModel,
                    onAnalysisStarted = {
                        navController.navigate("result")
                    }
                )
            }

            composable("result") {
                ResultScreen(
                    viewModel = foodAnalysisViewModel,
                    onBack = {
                        navController.popBackStack()
                    },
                    onMealSaved = {
                        foodAnalysisViewModel.resetState()
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("profile") {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.provideFactory(
                        userProfileRepository = app.userProfileRepository,
                        computeNutritionGoalUseCase = app.computeNutritionGoalUseCase,
                        healthConnectManager = app.healthConnectManager,
                        googleAuthManager = app.googleAuthManager,
                        driveBackupManager = app.driveBackupManager,
                        mealRepository = app.mealRepository
                    )
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    healthConnectManager = app.healthConnectManager
                )
            }

            composable("history") {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(
                        historyUseCase = app.getNutritionHistoryUseCase,
                        userProfileRepository = app.userProfileRepository,
                        mealRepository = app.mealRepository
                    )
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
