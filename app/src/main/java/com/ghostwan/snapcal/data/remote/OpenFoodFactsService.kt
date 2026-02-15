package com.ghostwan.snapcal.data.remote

import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.model.Ingredient
import com.ghostwan.snapcal.domain.model.Macros
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class OpenFoodFactsService {

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/api/v2/product"
    }

    suspend fun lookupProduct(barcode: String): FoodAnalysis {
        return withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/$barcode.json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SnapCal Android App")
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw OpenFoodFactsException("API error (code $responseCode): $error")
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(responseBody)

            if (json.optInt("status", 0) != 1) {
                throw ProductNotFoundException(barcode)
            }

            val product = json.getJSONObject("product")
            parseProduct(product)
        }
    }

    private fun parseProduct(product: JSONObject): FoodAnalysis {
        val langCode = Locale.getDefault().language
        val productName = product.optString("product_name_$langCode", "").ifBlank {
            product.optString("product_name", "").ifBlank { "?" }
        }
        val brands = product.optString("brands", "").ifBlank { null }
        val dishName = if (brands != null) "$productName ($brands)" else productName

        val nutriments = product.optJSONObject("nutriments")
        val servingSize = product.optString("serving_size", "").ifBlank { null }

        val hasServingData = nutriments?.has("energy-kcal_serving") == true
                && (nutriments.optDouble("energy-kcal_serving", 0.0) > 0)

        val calories: Int
        val proteins: Float
        val carbs: Float
        val fats: Float
        val fiber: Float
        val quantityLabel: String

        if (hasServingData && servingSize != null) {
            calories = nutriments!!.optDouble("energy-kcal_serving", 0.0).toInt()
            proteins = nutriments.optDouble("proteins_serving", 0.0).toFloat()
            carbs = nutriments.optDouble("carbohydrates_serving", 0.0).toFloat()
            fats = nutriments.optDouble("fat_serving", 0.0).toFloat()
            fiber = nutriments.optDouble("fiber_serving", 0.0).toFloat()
            quantityLabel = servingSize
        } else {
            calories = nutriments?.optDouble("energy-kcal_100g", 0.0)?.toInt() ?: 0
            proteins = nutriments?.optDouble("proteins_100g", 0.0)?.toFloat() ?: 0f
            carbs = nutriments?.optDouble("carbohydrates_100g", 0.0)?.toFloat() ?: 0f
            fats = nutriments?.optDouble("fat_100g", 0.0)?.toFloat() ?: 0f
            fiber = nutriments?.optDouble("fiber_100g", 0.0)?.toFloat() ?: 0f
            quantityLabel = "100g"
        }

        val note = if (hasServingData && servingSize != null) {
            "Open Food Facts · $servingSize"
        } else {
            "Open Food Facts · 100g"
        }

        return FoodAnalysis(
            dishName = dishName,
            totalCalories = calories,
            ingredients = listOf(
                Ingredient(
                    name = productName,
                    quantity = quantityLabel,
                    calories = calories
                )
            ),
            macros = Macros(
                proteins = String.format("%.1fg", proteins),
                carbs = String.format("%.1fg", carbs),
                fats = String.format("%.1fg", fats),
                fiber = if (fiber > 0) String.format("%.1fg", fiber) else null
            ),
            notes = note,
            emoji = "\uD83D\uDCE6"
        )
    }
}

class OpenFoodFactsException(message: String) : Exception(message)
class ProductNotFoundException(barcode: String) : Exception("Product not found: $barcode")
