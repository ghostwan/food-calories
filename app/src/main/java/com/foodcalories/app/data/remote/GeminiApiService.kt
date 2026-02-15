package com.foodcalories.app.data.remote

import android.util.Base64
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

    suspend fun analyzeImage(imageData: ByteArray, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            val base64Image = Base64.encodeToString(imageData, Base64.NO_WRAP)
            val requestBody = buildRequestBody(base64Image)

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

    private fun buildRequestBody(base64Image: String): String {
        val prompt = """
            Analyse cette photo de nourriture/plat et fournis une estimation détaillée.

            Réponds UNIQUEMENT avec un JSON valide (sans markdown, sans backticks) au format suivant:
            {
                "dishName": "Nom du plat",
                "totalCalories": 500,
                "ingredients": [
                    {"name": "Ingrédient 1", "quantity": "150g", "calories": 200},
                    {"name": "Ingrédient 2", "quantity": "50g", "calories": 100}
                ],
                "macros": {
                    "proteins": "25g",
                    "carbs": "60g",
                    "fats": "15g",
                    "fiber": "5g"
                },
                "notes": "Remarques supplémentaires sur le plat"
            }

            Si l'image ne contient pas de nourriture, réponds avec:
            {"dishName": "Non reconnu", "totalCalories": 0, "ingredients": [], "macros": null, "notes": "L'image ne semble pas contenir de nourriture."}
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
