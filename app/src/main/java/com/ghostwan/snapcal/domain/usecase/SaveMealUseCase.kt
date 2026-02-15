package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.model.MealEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveMealUseCase(
    private val mealRepository: com.ghostwan.snapcal.domain.repository.MealRepository
) {
    suspend operator fun invoke(analysis: FoodAnalysis) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val ingredientsJson = buildIngredientsJson(analysis)
        val meal = MealEntry(
            dishName = analysis.dishName,
            calories = analysis.totalCalories,
            proteins = parseGrams(analysis.macros?.proteins),
            carbs = parseGrams(analysis.macros?.carbs),
            fats = parseGrams(analysis.macros?.fats),
            fiber = parseGrams(analysis.macros?.fiber),
            date = date,
            ingredientsJson = ingredientsJson
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
            sb.append("\"calories\":${ingredient.calories}}")
        }
        sb.append("]")
        return sb.toString()
    }
}
