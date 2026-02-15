package com.ghostwan.snapcal.domain.repository

import com.ghostwan.snapcal.domain.model.FoodAnalysis

interface FoodAnalysisRepository {
    suspend fun analyzeFood(imageData: ByteArray, language: String): FoodAnalysis
    suspend fun analyzeFoodFromText(description: String, language: String): FoodAnalysis
    suspend fun correctAnalysis(
        originalAnalysis: FoodAnalysis,
        userFeedback: String,
        imageData: ByteArray?,
        language: String
    ): FoodAnalysis
}
