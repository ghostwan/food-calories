package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.repository.FoodAnalysisRepository

class AnalyzeFoodUseCase(
    private val repository: FoodAnalysisRepository
) {
    suspend operator fun invoke(imageData: ByteArray, language: String): FoodAnalysis {
        return repository.analyzeFood(imageData, language)
    }

    suspend fun fromText(description: String, language: String): FoodAnalysis {
        return repository.analyzeFoodFromText(description, language)
    }

    suspend fun fromBarcode(barcode: String): FoodAnalysis {
        return repository.analyzeFoodFromBarcode(barcode)
    }
}
