package com.ghostwan.snapcal.data.repository

import com.ghostwan.snapcal.data.local.MealDao
import com.ghostwan.snapcal.data.local.MealEntity
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.repository.MealRepository
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
                ingredientsJson = meal.ingredientsJson,
                emoji = meal.emoji
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

    override fun getDailyNutritionHistory(days: Int): Flow<List<DailyNutrition>> {
        return mealDao.getDailyNutritionHistory(days).map { list ->
            list.map {
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

    override suspend fun getAllMeals(): List<MealEntry> {
        return mealDao.getAll().map { it.toDomain() }
    }

    override suspend fun deleteMeal(id: Long) {
        mealDao.delete(id)
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        mealDao.setFavorite(id, isFavorite)
    }

    override fun getFavorites(): Flow<List<MealEntry>> {
        return mealDao.getFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
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
        ingredientsJson = ingredientsJson,
        emoji = emoji,
        isFavorite = isFavorite
    )
}
