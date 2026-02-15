package com.foodcalories.app.data.repository

import com.foodcalories.app.data.local.MealDao
import com.foodcalories.app.data.local.MealEntity
import com.foodcalories.app.domain.model.DailyNutrition
import com.foodcalories.app.domain.model.MealEntry
import com.foodcalories.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MealRepositoryImpl(private val mealDao: MealDao) : MealRepository {

    override suspend fun saveMeal(meal: MealEntry) {
        mealDao.insert(
            MealEntity(
                dishName = meal.dishName,
                calories = meal.calories,
                proteins = meal.proteins,
                carbs = meal.carbs,
                fats = meal.fats,
                fiber = meal.fiber,
                date = meal.date,
                ingredientsJson = meal.ingredientsJson
            )
        )
    }

    override fun getMealsForDate(date: String): Flow<List<MealEntry>> {
        return mealDao.getMealsForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDailyNutrition(date: String): Flow<DailyNutrition?> {
        return mealDao.getDailyNutrition(date).map { tuple ->
            tuple?.let {
                DailyNutrition(
                    date = it.date,
                    totalCalories = it.totalCalories,
                    totalProteins = it.totalProteins,
                    totalCarbs = it.totalCarbs,
                    totalFats = it.totalFats,
                    totalFiber = it.totalFiber
                )
            }
        }
    }

    override suspend fun getDailyNutritionHistory(days: Int): List<DailyNutrition> {
        return mealDao.getDailyNutritionHistory(days).map {
            DailyNutrition(
                date = it.date,
                totalCalories = it.totalCalories,
                totalProteins = it.totalProteins,
                totalCarbs = it.totalCarbs,
                totalFats = it.totalFats,
                totalFiber = it.totalFiber
            )
        }
    }

    override suspend fun getAllMeals(): List<MealEntry> {
        return mealDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteMeal(id: Long) {
        mealDao.delete(id)
    }

    private fun MealEntity.toDomain() = MealEntry(
        id = id,
        dishName = dishName,
        calories = calories,
        proteins = proteins,
        carbs = carbs,
        fats = fats,
        fiber = fiber,
        date = date,
        ingredientsJson = ingredientsJson
    )
}
