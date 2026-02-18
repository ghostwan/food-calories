package com.ghostwan.snapcal.data.remote

import android.util.Base64
import com.ghostwan.snapcal.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiApiService {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-2.0-flash"
        private const val MAX_RETRIES = 3
        private val RETRY_DELAYS_MS = longArrayOf(2_000, 5_000, 10_000)
    }

    suspend fun analyzeImage(imageData: ByteArray, apiKey: String, language: String): String {
        val base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP)
        val requestBody = buildImageRequestBody(buildAnalysisPrompt(language), base64Image)
        return executeRequest(requestBody, apiKey)
    }

    suspend fun analyzeText(description: String, apiKey: String, language: String): String {
        val prompt = """
            A user describes what they ate:
            "$description"

            Based on this description, provide a detailed nutritional estimation.
            IMPORTANT: All text values in your response (dish name, ingredient names, notes, quantities) MUST be written in $language.

            Estimate the quantity of each ingredient as precisely as possible (weight in grams, volume in ml/cl, or count for countable items like eggs).

            Respond ONLY with valid JSON (no markdown, no backticks) in this exact format:
            {
                "emoji": "🍝",
                "dishName": "Name of the dish",
                "totalCalories": 500,
                "ingredients": [
                    {"name": "Ingredient 1", "quantity": "150g", "calories": 200, "healthRating": "healthy"},
                    {"name": "Ingredient 2", "quantity": "2 cuillères à soupe (30ml)", "calories": 100, "healthRating": "moderate"}
                ],
                "macros": {
                    "proteins": "25g",
                    "carbs": "60g",
                    "fats": "15g",
                    "fiber": "5g"
                },
                "notes": "Additional remarks about the dish"
            }

            The "emoji" field must be a single emoji that best represents the dish (e.g. 🍕 for pizza, 🥗 for salad, 🍣 for sushi).
            The "healthRating" field for each ingredient must be "healthy" (nutritious, whole foods), "moderate" (acceptable in moderation), or "unhealthy" (highly processed, high sugar/fat).
        """.trimIndent()

        return executeRequest(buildTextRequestBody(prompt), apiKey)
    }

    suspend fun correctAnalysis(
        originalAnalysisJson: String,
        userFeedback: String,
        imageData: ByteArray?,
        apiKey: String,
        language: String
    ): String {
        val prompt = """
            You previously analyzed a food photo and gave this result:
            $originalAnalysisJson

            The user says this is incorrect and provides this feedback:
            "$userFeedback"

            Please re-analyze and provide a corrected estimation based on the user's feedback.
            IMPORTANT: All text values in your response MUST be written in $language.

            Respond ONLY with valid JSON (no markdown, no backticks) in this exact format:
            {
                "emoji": "🍝",
                "dishName": "Name of the dish",
                "totalCalories": 500,
                "ingredients": [
                    {"name": "Ingredient 1", "quantity": "150g", "calories": 200, "healthRating": "healthy"}
                ],
                "macros": {
                    "proteins": "25g",
                    "carbs": "60g",
                    "fats": "15g",
                    "fiber": "5g"
                },
                "notes": "Additional remarks"
            }

            The "emoji" field must be a single emoji that best represents the dish.
            The "healthRating" field for each ingredient must be "healthy" (nutritious, whole foods), "moderate" (acceptable in moderation), or "unhealthy" (highly processed, high sugar/fat).
        """.trimIndent()

        val requestBody = if (imageData != null) {
            val base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP)
            buildImageRequestBody(prompt, base64Image)
        } else {
            buildTextRequestBody(prompt)
        }
        return executeRequest(requestBody, apiKey)
    }

    suspend fun computeNutritionGoal(profile: UserProfile, apiKey: String, language: String): String {
        val prompt = """
            Given a person with the following characteristics:
            - Height: ${profile.height} cm
            - Weight: ${profile.weight} kg
            - Target weight: ${profile.targetWeight} kg
            - Age: ${profile.age}
            - Gender: ${profile.gender.name.lowercase()}
            - Activity level: ${profile.activityLevel.name.lowercase().replace("_", " ")}

            Calculate recommended daily nutritional goals to help them reach their target weight in a healthy way.
            IMPORTANT: Write the explanation in $language.

            Respond ONLY with valid JSON (no markdown, no backticks) in this exact format:
            {
                "calories": 2000,
                "proteins_g": 120.0,
                "carbs_g": 200.0,
                "fats_g": 65.0,
                "fiber_g": 30.0,
                "explanation": "Brief explanation of the recommendation"
            }
        """.trimIndent()

        return executeRequest(buildTextRequestBody(prompt), apiKey)
    }

    private suspend fun executeRequest(requestBody: String, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            for (attempt in 0..MAX_RETRIES) {
                if (attempt > 0) Thread.sleep(RETRY_DELAYS_MS[attempt - 1])

                val url = URL("$BASE_URL/$MODEL:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000

                connection.outputStream.use { os -> os.write(requestBody.toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    return@withContext connection.inputStream.bufferedReader().readText()
                }

                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                if (responseCode == 429 && attempt < MAX_RETRIES) {
                    lastException = GeminiApiException("Quota exceeded, retrying (${attempt + 1}/$MAX_RETRIES)...")
                    continue
                }
                throw GeminiApiException("API error (code $responseCode): $error")
            }
            throw lastException ?: GeminiApiException("Failed after $MAX_RETRIES retries")
        }
    }

    private fun buildTextRequestBody(prompt: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }.toString()
    }

    private fun buildImageRequestBody(prompt: String, base64Image: String): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }.toString()
    }

    private fun buildAnalysisPrompt(language: String): String {
        return """
            Analyze this food photo and provide a detailed estimation.
            IMPORTANT: All text values in your response (dish name, ingredient names, notes, quantities) MUST be written in $language.

            Estimate the quantity of each ingredient as precisely as possible (weight in grams, volume in ml/cl, or count for countable items like eggs).

            Respond ONLY with valid JSON (no markdown, no backticks) in this exact format:
            {
                "emoji": "🍝",
                "dishName": "Name of the dish",
                "totalCalories": 500,
                "ingredients": [
                    {"name": "Ingredient 1", "quantity": "150g", "calories": 200, "healthRating": "healthy"},
                    {"name": "Ingredient 2", "quantity": "2 cuillères à soupe (30ml)", "calories": 100, "healthRating": "moderate"}
                ],
                "macros": {
                    "proteins": "25g",
                    "carbs": "60g",
                    "fats": "15g",
                    "fiber": "5g"
                },
                "notes": "Additional remarks about the dish"
            }

            The "emoji" field must be a single emoji that best represents the dish (e.g. 🍕 for pizza, 🥗 for salad, 🍣 for sushi).
            The "healthRating" field for each ingredient must be "healthy" (nutritious, whole foods), "moderate" (acceptable in moderation), or "unhealthy" (highly processed, high sugar/fat).

            If the image does not contain food, respond with:
            {"emoji": "❓", "dishName": "Not recognized", "totalCalories": 0, "ingredients": [], "macros": null, "notes": "The image does not appear to contain food."}
        """.trimIndent()
    }
}

class GeminiApiException(message: String) : Exception(message)
