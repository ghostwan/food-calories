package com.foodcalories.app.data.remote

import android.util.Base64
import com.foodcalories.app.domain.model.UserProfile
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
        return withContext(Dispatchers.IO) {
            val base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP)
            val requestBody = buildRequestBody(base64Image, language)

            var lastException: Exception? = null

            for (attempt in 0..MAX_RETRIES) {
                if (attempt > 0) {
                    Thread.sleep(RETRY_DELAYS_MS[attempt - 1])
                }

                val url = URL("$BASE_URL/$MODEL:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000

                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray())
                }

                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    return@withContext connection.inputStream.bufferedReader().readText()
                }

                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Erreur inconnue"

                if (responseCode == 429 && attempt < MAX_RETRIES) {
                    lastException = GeminiApiException("Quota dépassé, nouvelle tentative (${attempt + 1}/$MAX_RETRIES)...")
                    continue
                }

                throw GeminiApiException("Erreur API (code $responseCode): $error")
            }

            throw lastException ?: GeminiApiException("Échec après $MAX_RETRIES tentatives")
        }
    }

    suspend fun computeNutritionGoal(profile: UserProfile, apiKey: String, language: String): String {
        return withContext(Dispatchers.IO) {
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

            val requestBody = buildTextRequestBody(prompt)

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
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }.toString()
    }

    private fun buildRequestBody(base64Image: String, language: String): String {
        val prompt = """
            Analyze this food photo and provide a detailed estimation.
            IMPORTANT: All text values in your response (dish name, ingredient names, notes, quantities) MUST be written in $language.

            Estimate the quantity of each ingredient as precisely as possible (weight in grams, volume in ml/cl, or count for countable items like eggs).

            Respond ONLY with valid JSON (no markdown, no backticks) in this exact format:
            {
                "dishName": "Name of the dish",
                "totalCalories": 500,
                "ingredients": [
                    {"name": "Ingredient 1", "quantity": "150g", "calories": 200},
                    {"name": "Ingredient 2", "quantity": "2 cuillères à soupe (30ml)", "calories": 100}
                ],
                "macros": {
                    "proteins": "25g",
                    "carbs": "60g",
                    "fats": "15g",
                    "fiber": "5g"
                },
                "notes": "Additional remarks about the dish"
            }

            If the image does not contain food, respond with:
            {"dishName": "Not recognized", "totalCalories": 0, "ingredients": [], "macros": null, "notes": "The image does not appear to contain food."}
        """.trimIndent()

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
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
}

class GeminiApiException(message: String) : Exception(message)
