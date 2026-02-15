package com.foodcalories.app.domain.usecase

import com.foodcalories.app.domain.model.FoodAnalysis
import com.foodcalories.app.domain.repository.FoodAnalysisRepository

class AnalyzeFoodUseCase(
    private val repository: FoodAnalysisRepository
) {
    suspend operator fun invoke(imageData: ByteArray): FoodAnalysis {
        return repository.analyzeFood(imageData)
    }
}
