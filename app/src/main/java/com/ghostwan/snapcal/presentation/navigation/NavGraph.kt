package com.ghostwan.snapcal.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ghostwan.snapcal.MainActivity
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
import com.ghostwan.snapcal.presentation.shopping.ShoppingListScreen
import com.ghostwan.snapcal.presentation.shopping.ShoppingListViewModel
import com.ghostwan.snapcal.domain.model.ShoppingItem
import com.ghostwan.snapcal.widget.ShoppingWidgetProvider

private data class BottomNavItem(val route: String, val labelRes: Int, val icon: ImageVector)

// Routes that should NOT show the bottom bar
private val noBottomBarRoutes = setOf("result", "profile")

@Composable
fun SnapCalNavGraph(startRoute: String = "dashboard") {
    val context = LocalContext.current
    val app = context.applicationContext as SnapCalApp

    val navController = rememberNavController()

    // Handle navigation from widgets when app is already open
    val activity = context as? MainActivity
    val pendingRoute = activity?.pendingRoute?.collectAsState()?.value
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navController.navigate(pendingRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            activity.consumePendingRoute()
        }
    }

    val foodAnalysisViewModel: FoodAnalysisViewModel = viewModel(
        factory = FoodAnalysisViewModel.provideFactory(
            analyzeFoodUseCase = app.analyzeFoodUseCase,
            correctAnalysisUseCase = app.correctAnalysisUseCase,
            settingsRepository = app.settingsRepository,
            usageRepository = app.usageRepository,
            saveMealUseCase = app.saveMealUseCase,
            mealRepository = app.mealRepository
        )
    )

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(
            getDailyNutritionUseCase = app.getDailyNutritionUseCase,
            userProfileRepository = app.userProfileRepository,
            mealRepository = app.mealRepository,
            healthConnectManager = app.healthConnectManager,
            dailyNoteRepository = app.dailyNoteRepository
        )
    )

    val shoppingListEnabled = app.settingsRepository.isShoppingListEnabled()

    val bottomNavItems = buildList {
        add(BottomNavItem("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard))
        add(BottomNavItem("home", R.string.nav_scanner, Icons.Default.Restaurant))
        add(BottomNavItem("history", R.string.nav_history, Icons.Default.History))
        if (shoppingListEnabled) {
            add(BottomNavItem("shopping", R.string.nav_shopping, Icons.Default.ShoppingCart))
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in noBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val showLabels = bottomNavItems.size <= 3
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = if (showLabels) {{ Text(stringResource(item.labelRes)) }} else null,
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
            startDestination = startRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onScanMeal = {
                        val selectedDate = dashboardViewModel.selectedDate.value
                        val today = java.time.LocalDate.now()
                        foodAnalysisViewModel.setTargetDate(
                            if (selectedDate == today) null else selectedDate.toString()
                        )
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onProfile = {
                        navController.navigate("profile")
                    },
                    onMealClick = { meal ->
                        foodAnalysisViewModel.viewMealDetail(meal)
                        navController.navigate("result")
                    },
                    onMergeMeals = { meals ->
                        foodAnalysisViewModel.viewMergedMeals(meals)
                        navController.navigate("result")
                    }
                )
            }

            composable("home") {
                val favorites by app.mealRepository.getFavorites().collectAsState(initial = emptyList())
                HomeScreen(
                    viewModel = foodAnalysisViewModel,
                    onAnalysisStarted = {
                        navController.navigate("result")
                    },
                    favorites = favorites,
                    onQuickAddFavorite = { meal ->
                        kotlinx.coroutines.MainScope().launch {
                            val date = dashboardViewModel.selectedDate.value.toString()
                            app.mealRepository.saveMeal(
                                meal.copy(id = 0, date = date, isFavorite = false)
                            )
                        }
                    },
                    onMealClick = { meal ->
                        foodAnalysisViewModel.viewMealDetail(meal)
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
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onAddToShoppingList = if (shoppingListEnabled) { ingredient ->
                        val today = java.time.LocalDate.now().toString()
                        kotlinx.coroutines.MainScope().launch {
                            app.shoppingRepository.addItem(
                                ShoppingItem(
                                    name = ingredient.name,
                                    quantity = ingredient.quantity,
                                    addedDate = today
                                )
                            )
                            ShoppingWidgetProvider.updateAllWidgets(context.applicationContext)
                        }
                    } else null
                )
            }

            composable("profile") {
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.provideFactory(
                        application = app,
                        userProfileRepository = app.userProfileRepository,
                        computeNutritionGoalUseCase = app.computeNutritionGoalUseCase,
                        healthConnectManager = app.healthConnectManager,
                        googleAuthManager = app.googleAuthManager,
                        driveBackupManager = app.driveBackupManager,
                        mealRepository = app.mealRepository,
                        mealReminderManager = app.mealReminderManager,
                        settingsRepository = app.settingsRepository,
                        dailyNoteRepository = app.dailyNoteRepository
                    )
                )
                ProfileScreen(
                    viewModel = profileViewModel,
                    healthConnectManager = app.healthConnectManager,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("history") {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.provideFactory(
                        historyUseCase = app.getNutritionHistoryUseCase,
                        userProfileRepository = app.userProfileRepository,
                        mealRepository = app.mealRepository,
                        healthConnectManager = app.healthConnectManager
                    )
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onMealClick = { meal ->
                        foodAnalysisViewModel.viewMealDetail(meal)
                        navController.navigate("result")
                    }
                )
            }

            composable("shopping") {
                val shoppingViewModel: ShoppingListViewModel = viewModel(
                    factory = ShoppingListViewModel.provideFactory(
                        shoppingRepository = app.shoppingRepository,
                        appContext = context.applicationContext
                    )
                )
                ShoppingListScreen(
                    viewModel = shoppingViewModel
                )
            }
        }
    }
}
