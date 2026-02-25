package com.ghostwan.snapcal.domain.usecase

import android.content.Context
import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.widget.CaloriesWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveMealUseCase(
    private val mealRepository: com.ghostwan.snapcal.domain.repository.MealRepository,
    private val appContext: Context
) {
    suspend operator fun invoke(analysis: FoodAnalysis, quantity: Int = 1, date: String? = null) {
        val mealDate = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        saveMeal(analysis, mealDate, quantity)
        refreshWidget()
    }

    suspend fun replaceAndSave(oldMealId: Long, analysis: FoodAnalysis, date: String, quantity: Int = 1) {
        mealRepository.deleteMeal(oldMealId)
        saveMeal(analysis, date, quantity)
        refreshWidget()
    }

    suspend fun replaceMultipleAndSave(oldMealIds: List<Long>, analysis: FoodAnalysis, date: String, quantity: Int = 1) {
        oldMealIds.forEach { mealRepository.deleteMeal(it) }
        saveMeal(analysis, date, quantity)
        refreshWidget()
    }

    suspend fun splitAndSave(
        originalMealId: Long?,
        analysis1: FoodAnalysis,
        analysis2: FoodAnalysis,
        date: String,
        quantity: Int = 1
    ) {
        if (originalMealId != null) {
            mealRepository.deleteMeal(originalMealId)
        }
        saveMeal(analysis1, date, quantity)
        saveMeal(analysis2, date, quantity)
        refreshWidget()
    }

    suspend fun splitAndSaveMultiple(
        originalMealIds: List<Long>,
        analysis1: FoodAnalysis,
        analysis2: FoodAnalysis,
        date: String,
        quantity: Int = 1
    ) {
        originalMealIds.forEach { mealRepository.deleteMeal(it) }
        saveMeal(analysis1, date, quantity)
        saveMeal(analysis2, date, quantity)
        refreshWidget()
    }

    private fun refreshWidget() {
        CaloriesWidgetProvider.refreshAll(appContext)
    }

    private suspend fun saveMeal(analysis: FoodAnalysis, date: String, quantity: Int = 1) {
        val ingredientsJson = buildIngredientsJson(analysis)
        val meal = MealEntry(
            dishName = analysis.dishName,
            calories = analysis.totalCalories,
            proteins = parseGrams(analysis.macros?.proteins),
            carbs = parseGrams(analysis.macros?.carbs),
            fats = parseGrams(analysis.macros?.fats),
            fiber = parseGrams(analysis.macros?.fiber),
            date = date,
            ingredientsJson = ingredientsJson,
            emoji = analysis.emoji,
            quantity = quantity
        )
        mealRepository.saveMeal(meal)
    }

    private fun parseGrams(value: String?): Float {
        if (value == null) return 0f
        return value.replace(Regex("[^0-9.,]"), "")
            .replace(",", ".")
            .toFloatOrNull() ?: 0f
    }

    private fun buildIngredientsJson(analysis: FoodAnalysis): String {
        val sb = StringBuilder("[")
        analysis.ingredients.forEachIndexed { index, ingredient ->
            if (index > 0) sb.append(",")
            sb.append("{\"name\":\"${ingredient.name.replace("\"", "\\\"")}\",")
            sb.append("\"quantity\":\"${ingredient.quantity.replace("\"", "\\\"")}\",")
            sb.append("\"calories\":${ingredient.calories}")
            if (ingredient.healthRating != null) {
                sb.append(",\"healthRating\":\"${ingredient.healthRating}\"")
            }
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }
}
