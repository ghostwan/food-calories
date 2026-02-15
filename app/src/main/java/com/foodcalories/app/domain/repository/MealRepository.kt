package com.foodcalories.app.domain.repository

import com.foodcalories.app.domain.model.DailyNutrition
import com.foodcalories.app.domain.model.MealEntry
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    suspend fun saveMeal(meal: MealEntry)
    fun getMealsForDate(date: String): Flow<List<MealEntry>>
    fun getDailyNutrition(date: String): Flow<DailyNutrition?>
    suspend fun getDailyNutritionHistory(days: Int): List<DailyNutrition>
    suspend fun getAllMeals(): List<MealEntry>
    suspend fun deleteMeal(id: Long)
}
