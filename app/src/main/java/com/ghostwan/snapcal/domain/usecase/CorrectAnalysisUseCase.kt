package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.repository.FoodAnalysisRepository

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
