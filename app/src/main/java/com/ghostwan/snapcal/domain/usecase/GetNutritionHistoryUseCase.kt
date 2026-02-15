package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.repository.MealRepository

class GetNutritionHistoryUseCase(private val mealRepository: MealRepository) {

    suspend operator fun invoke(days: Int = 30): List<DailyNutrition> {
        return mealRepository.getDailyNutritionHistory(days)
    }
}
