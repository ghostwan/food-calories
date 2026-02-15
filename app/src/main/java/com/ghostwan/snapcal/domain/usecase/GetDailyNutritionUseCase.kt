package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow

class GetDailyNutritionUseCase(private val mealRepository: MealRepository) {

    fun getNutrition(date: String): Flow<DailyNutrition?> {
        return mealRepository.getDailyNutrition(date)
    }

    fun getMeals(date: String): Flow<List<MealEntry>> {
        return mealRepository.getMealsForDate(date)
    }
}
