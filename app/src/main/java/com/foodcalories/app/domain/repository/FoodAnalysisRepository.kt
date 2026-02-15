package com.foodcalories.app.domain.repository

import com.foodcalories.app.domain.model.FoodAnalysis

interface FoodAnalysisRepository {
    suspend fun analyzeFood(imageData: ByteArray, language: String): FoodAnalysis
    suspend fun correctAnalysis(
        originalAnalysis: FoodAnalysis,
        userFeedback: String,
        imageData: ByteArray?,
        language: String
    ): FoodAnalysis
}
