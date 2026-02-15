package com.foodcalories.app.data.repository

import com.foodcalories.app.data.mapper.FoodAnalysisMapper
import com.foodcalories.app.data.remote.GeminiApiService
import com.foodcalories.app.domain.model.FoodAnalysis
import com.foodcalories.app.domain.repository.FoodAnalysisRepository
import com.foodcalories.app.domain.repository.SettingsRepository

class FoodAnalysisRepositoryImpl(
    private val apiService: GeminiApiService,
    private val settingsRepository: SettingsRepository,
    private val mapper: FoodAnalysisMapper
) : FoodAnalysisRepository {

    override suspend fun analyzeFood(imageData: ByteArray): FoodAnalysis {
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Clé API Gemini non configurée")
        }
        val rawResponse = apiService.analyzeImage(imageData, apiKey)
        return mapper.mapFromApiResponse(rawResponse)
    }
}
