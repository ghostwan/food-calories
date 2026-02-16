package com.ghostwan.snapcal.data.repository

import com.ghostwan.snapcal.data.mapper.FoodAnalysisMapper
import com.ghostwan.snapcal.data.remote.GeminiApiService
import com.ghostwan.snapcal.data.remote.GeminiAuth
import com.ghostwan.snapcal.data.remote.GoogleAuthManager
import com.ghostwan.snapcal.data.remote.OpenFoodFactsService
import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.repository.FoodAnalysisRepository
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import org.json.JSONArray
import org.json.JSONObject

class FoodAnalysisRepositoryImpl(
    private val apiService: GeminiApiService,
    private val settingsRepository: SettingsRepository,
    private val mapper: FoodAnalysisMapper,
    private val openFoodFactsService: OpenFoodFactsService,
    private val googleAuthManager: GoogleAuthManager
) : FoodAnalysisRepository {

    private suspend fun resolveAuth(): GeminiAuth {
        if (settingsRepository.isGoogleAuthForGemini() && googleAuthManager.isSignedIn()) {
            val token = googleAuthManager.getGeminiAccessToken()
            if (token != null) return GeminiAuth.OAuth(token)
        }
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Clé API Gemini non configurée")
        }
        return GeminiAuth.ApiKey(apiKey)
    }

    override suspend fun analyzeFood(imageData: ByteArray, language: String): FoodAnalysis {
        val auth = resolveAuth()
        val rawResponse = apiService.analyzeImage(imageData, auth, language)
        return mapper.mapFromApiResponse(rawResponse)
    }

    override suspend fun analyzeFoodFromText(description: String, language: String): FoodAnalysis {
        val auth = resolveAuth()
        val rawResponse = apiService.analyzeText(description, auth, language)
        return mapper.mapFromApiResponse(rawResponse)
    }

    override suspend fun correctAnalysis(
        originalAnalysis: FoodAnalysis,
        userFeedback: String,
        imageData: ByteArray?,
        language: String
    ): FoodAnalysis {
        val auth = resolveAuth()
        val originalJson = serializeAnalysis(originalAnalysis)
        val rawResponse = apiService.correctAnalysis(originalJson, userFeedback, imageData, auth, language)
        return mapper.mapFromApiResponse(rawResponse)
    }

    override suspend fun analyzeFoodFromBarcode(barcode: String): FoodAnalysis {
        return openFoodFactsService.lookupProduct(barcode)
    }

    private fun serializeAnalysis(analysis: FoodAnalysis): String {
        return JSONObject().apply {
            if (analysis.emoji != null) put("emoji", analysis.emoji)
            put("dishName", analysis.dishName)
            put("totalCalories", analysis.totalCalories)
            put("ingredients", JSONArray().apply {
                analysis.ingredients.forEach { ing ->
                    put(JSONObject().apply {
                        put("name", ing.name)
                        put("quantity", ing.quantity)
                        put("calories", ing.calories)
                    })
                }
            })
            if (analysis.macros != null) {
                put("macros", JSONObject().apply {
                    put("proteins", analysis.macros.proteins)
                    put("carbs", analysis.macros.carbs)
                    put("fats", analysis.macros.fats)
                    if (analysis.macros.fiber != null) put("fiber", analysis.macros.fiber)
                })
            }
            if (analysis.notes != null) put("notes", analysis.notes)
        }.toString()
    }
}
