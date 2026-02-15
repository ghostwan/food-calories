package com.foodcalories.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.foodcalories.app.FoodCaloriesApp
import com.foodcalories.app.presentation.FoodAnalysisViewModel
import com.foodcalories.app.presentation.home.HomeScreen
import com.foodcalories.app.presentation.result.ResultScreen

@Composable
fun FoodCaloriesNavGraph() {
    val context = LocalContext.current
    val app = context.applicationContext as FoodCaloriesApp

    val navController = rememberNavController()
    val viewModel: FoodAnalysisViewModel = viewModel(
        factory = FoodAnalysisViewModel.provideFactory(
            analyzeFoodUseCase = app.analyzeFoodUseCase,
            settingsRepository = app.settingsRepository,
            usageRepository = app.usageRepository
        )
    )

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onAnalysisStarted = {
                    navController.navigate("result")
                }
            )
        }
        composable("result") {
            ResultScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
