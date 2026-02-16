package com.ghostwan.snapcal.domain.usecase

import com.ghostwan.snapcal.data.remote.GeminiApiService
import com.ghostwan.snapcal.data.remote.GeminiAuth
import com.ghostwan.snapcal.data.remote.GoogleAuthManager
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import org.json.JSONObject

class ComputeNutritionGoalUseCase(
    private val apiService: GeminiApiService,
    private val settingsRepository: SettingsRepository,
    private val googleAuthManager: GoogleAuthManager
) {
    private suspend fun resolveAuth(): GeminiAuth {
        if (settingsRepository.isGoogleAuthForGemini() && googleAuthManager.isSignedIn()) {
            val token = googleAuthManager.getGeminiAccessToken()
            if (token != null) return GeminiAuth.OAuth(token)
        }
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) {
            throw IllegalStateException("Clé API Gemini non configurée")
        }
        return GeminiAuth.ApiKey(apiKey)
    }

    suspend operator fun invoke(profile: UserProfile, language: String): NutritionGoal {
        val auth = resolveAuth()
        val rawResponse = apiService.computeNutritionGoal(profile, auth, language)

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
