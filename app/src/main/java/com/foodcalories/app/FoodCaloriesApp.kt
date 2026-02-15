package com.foodcalories.app

import android.app.Application
import com.foodcalories.app.data.local.AppDatabase
import com.foodcalories.app.data.mapper.FoodAnalysisMapper
import com.foodcalories.app.data.remote.GeminiApiService
import com.foodcalories.app.data.repository.FoodAnalysisRepositoryImpl
import com.foodcalories.app.data.repository.MealRepositoryImpl
import com.foodcalories.app.data.repository.SettingsRepositoryImpl
import com.foodcalories.app.data.repository.UsageRepositoryImpl
import com.foodcalories.app.data.repository.UserProfileRepositoryImpl
import com.foodcalories.app.domain.repository.MealRepository
import com.foodcalories.app.domain.repository.SettingsRepository
import com.foodcalories.app.domain.repository.UsageRepository
import com.foodcalories.app.domain.repository.UserProfileRepository
import com.foodcalories.app.domain.usecase.AnalyzeFoodUseCase
import com.foodcalories.app.domain.usecase.ComputeNutritionGoalUseCase
import com.foodcalories.app.domain.usecase.GetDailyNutritionUseCase
import com.foodcalories.app.domain.usecase.GetNutritionHistoryUseCase
import com.foodcalories.app.domain.usecase.SaveMealUseCase

class FoodCaloriesApp : Application() {

    lateinit var analyzeFoodUseCase: AnalyzeFoodUseCase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var usageRepository: UsageRepository
        private set
    lateinit var saveMealUseCase: SaveMealUseCase
        private set
    lateinit var getDailyNutritionUseCase: GetDailyNutritionUseCase
        private set
    lateinit var getNutritionHistoryUseCase: GetNutritionHistoryUseCase
        private set
    lateinit var computeNutritionGoalUseCase: ComputeNutritionGoalUseCase
        private set
    lateinit var userProfileRepository: UserProfileRepository
        private set
    lateinit var mealRepository: MealRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val apiService = GeminiApiService()
        val mapper = FoodAnalysisMapper()
        val settingsRepo = SettingsRepositoryImpl(this)
        settingsRepository = settingsRepo
        usageRepository = UsageRepositoryImpl(this)

        val database = AppDatabase.getInstance(this)
        val mealDao = database.mealDao()
        val mealRepo = MealRepositoryImpl(mealDao)
        mealRepository = mealRepo

        val userProfileRepo = UserProfileRepositoryImpl(this)
        userProfileRepository = userProfileRepo

        val foodAnalysisRepository = FoodAnalysisRepositoryImpl(apiService, settingsRepo, mapper)
        analyzeFoodUseCase = AnalyzeFoodUseCase(foodAnalysisRepository)
        saveMealUseCase = SaveMealUseCase(mealRepo)
        getDailyNutritionUseCase = GetDailyNutritionUseCase(mealRepo)
        getNutritionHistoryUseCase = GetNutritionHistoryUseCase(mealRepo)
        computeNutritionGoalUseCase = ComputeNutritionGoalUseCase(apiService, settingsRepo)
    }
}
