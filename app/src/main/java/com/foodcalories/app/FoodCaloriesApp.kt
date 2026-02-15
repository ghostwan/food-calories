package com.foodcalories.app

import android.app.Application
import com.foodcalories.app.data.mapper.FoodAnalysisMapper
import com.foodcalories.app.data.remote.GeminiApiService
import com.foodcalories.app.data.repository.FoodAnalysisRepositoryImpl
import com.foodcalories.app.data.repository.SettingsRepositoryImpl
import com.foodcalories.app.domain.repository.SettingsRepository
import com.foodcalories.app.domain.usecase.AnalyzeFoodUseCase

class FoodCaloriesApp : Application() {

    lateinit var analyzeFoodUseCase: AnalyzeFoodUseCase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val apiService = GeminiApiService()
        val mapper = FoodAnalysisMapper()
        val settingsRepo = SettingsRepositoryImpl(this)
        settingsRepository = settingsRepo

        val foodAnalysisRepository = FoodAnalysisRepositoryImpl(apiService, settingsRepo, mapper)
        analyzeFoodUseCase = AnalyzeFoodUseCase(foodAnalysisRepository)
    }
}
