package com.ghostwan.snapcal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Insert
    suspend fun insert(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE date = :date ORDER BY id DESC")
    fun getMealsForDate(date: String): Flow<List<MealEntity>>

    @Query(
        "SELECT date, SUM(calories * quantity) as totalCalories, SUM(proteins * quantity) as totalProteins, " +
        "SUM(carbs * quantity) as totalCarbs, SUM(fats * quantity) as totalFats, SUM(fiber * quantity) as totalFiber " +
        "FROM meals WHERE date = :date GROUP BY date"
    )
    fun getDailyNutrition(date: String): Flow<DailyNutritionTuple?>

    @Query(
        "SELECT date, SUM(calories * quantity) as totalCalories, SUM(proteins * quantity) as totalProteins, " +
        "SUM(carbs * quantity) as totalCarbs, SUM(fats * quantity) as totalFats, SUM(fiber * quantity) as totalFiber " +
        "FROM meals WHERE date >= :startDate GROUP BY date ORDER BY date DESC"
    )
    fun getDailyNutritionHistory(startDate: String): Flow<List<DailyNutritionTuple>>

    @Query("SELECT * FROM meals ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<MealEntity>

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE meals SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM meals WHERE isFavorite = 1 ORDER BY id DESC")
    fun getFavorites(): Flow<List<MealEntity>>

    @Query("SELECT SUM(calories * quantity) FROM meals WHERE date = :date")
    suspend fun getDailyCalories(date: String): Int?

    @Query("UPDATE meals SET emoji = :emoji WHERE id = :id")
    suspend fun updateEmoji(id: Long, emoji: String)

    @Query("UPDATE meals SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Int)

    @Query("UPDATE meals SET calories = :calories, proteins = :proteins, carbs = :carbs, fats = :fats, fiber = :fiber, ingredientsJson = :ingredientsJson WHERE id = :id")
    suspend fun updateMealNutrition(id: Long, calories: Int, proteins: Float, carbs: Float, fats: Float, fiber: Float, ingredientsJson: String)
}

data class DailyNutritionTuple(
    val date: String,
    val totalCalories: Int,
    val totalProteins: Float,
    val totalCarbs: Float,
    val totalFats: Float,
    val totalFiber: Float
)
