package com.foodcalories.app.domain.usecase

import com.foodcalories.app.domain.model.DailyNutrition
import com.foodcalories.app.domain.model.MealEntry
import com.foodcalories.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow

class GetDailyNutritionUseCase(private val mealRepository: MealRepository) {

    fun getNutrition(date: String): Flow<DailyNutrition?> {
        return mealRepository.getDailyNutrition(date)
    }

    fun getMeals(date: String): Flow<List<MealEntry>> {
        return mealRepository.getMealsForDate(date)
    }
}
