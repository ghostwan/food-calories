package com.foodcalories.app.domain.usecase

import com.foodcalories.app.domain.model.DailyNutrition
import com.foodcalories.app.domain.repository.MealRepository

class GetNutritionHistoryUseCase(private val mealRepository: MealRepository) {

    suspend operator fun invoke(days: Int = 30): List<DailyNutrition> {
        return mealRepository.getDailyNutritionHistory(days)
    }
}
