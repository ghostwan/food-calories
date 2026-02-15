package com.foodcalories.app.domain.usecase

import com.foodcalories.app.domain.model.FoodAnalysis
import com.foodcalories.app.domain.repository.FoodAnalysisRepository

class CorrectAnalysisUseCase(
    private val repository: FoodAnalysisRepository
) {
    suspend operator fun invoke(
        originalAnalysis: FoodAnalysis,
        userFeedback: String,
        imageData: ByteArray?,
        language: String
    ): FoodAnalysis {
        return repository.correctAnalysis(originalAnalysis, userFeedback, imageData, language)
    }
}
