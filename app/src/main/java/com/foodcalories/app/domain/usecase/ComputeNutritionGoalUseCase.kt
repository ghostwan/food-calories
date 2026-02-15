package com.foodcalories.app.domain.usecase

import com.foodcalories.app.data.remote.GeminiApiService
import com.foodcalories.app.domain.model.NutritionGoal
import com.foodcalories.app.domain.model.UserProfile
import com.foodcalories.app.domain.repository.SettingsRepository
import org.json.JSONObject

class ComputeNutritionGoalUseCase(
    private val apiService: GeminiApiService,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(profile: UserProfile, language: String): NutritionGoal {
        val apiKey = settingsRepository.getApiKey()
        val rawResponse = apiService.computeNutritionGoal(profile, apiKey, language)

        val json = JSONObject(rawResponse)
        val textContent = json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val cleanJson = textContent
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val result = JSONObject(cleanJson)

        return NutritionGoal(
            calories = result.getInt("calories"),
            proteins = result.getDouble("proteins_g").toFloat(),
            carbs = result.getDouble("carbs_g").toFloat(),
            fats = result.getDouble("fats_g").toFloat(),
            fiber = if (result.has("fiber_g")) result.getDouble("fiber_g").toFloat() else 25f,
            explanation = if (result.has("explanation")) result.getString("explanation") else null
        )
    }
}
