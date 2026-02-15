package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow

class GetNutritionHistoryUseCase(private val mealRepository: MealRepository) {

    operator fun invoke(days: Int = 30): Flow<List<DailyNutrition>> {
        return mealRepository.getDailyNutritionHistory(days)
    }
}
