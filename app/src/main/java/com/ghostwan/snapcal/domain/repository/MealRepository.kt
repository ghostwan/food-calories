package com.ghostwan.snapcal.domain.repository

import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    suspend fun saveMeal(meal: MealEntry)
    fun getMealsForDate(date: String): Flow<List<MealEntry>>
    fun getDailyNutrition(date: String): Flow<DailyNutrition?>
    fun getDailyNutritionHistory(days: Int): Flow<List<DailyNutrition>>
    suspend fun getAllMeals(): List<MealEntry>
    suspend fun deleteMeal(id: Long)
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    fun getFavorites(): Flow<List<MealEntry>>
    suspend fun updateEmoji(id: Long, emoji: String)
    suspend fun updateQuantity(id: Long, quantity: Int)
    suspend fun updateMealNutrition(id: Long, calories: Int, proteins: Float, carbs: Float, fats: Float, fiber: Float, ingredientsJson: String)
}
