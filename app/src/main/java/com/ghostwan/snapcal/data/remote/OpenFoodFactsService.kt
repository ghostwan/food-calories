package com.ghostwan.snapcal.data.remote

import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.model.Ingredient
import com.ghostwan.snapcal.domain.model.Macros
import com.ghostwan.snapcal.domain.model.NutrientLevels
import com.ghostwan.snapcal.domain.model.ProductHealthInfo
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

        val healthInfo = parseHealthInfo(product)

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
            emoji = pickEmoji(product),
            healthInfo = healthInfo
        )
    }
    private fun pickEmoji(product: JSONObject): String {
        val tags = mutableListOf<String>()
        val categoriesTags = product.optJSONArray("categories_tags")
        if (categoriesTags != null) {
            for (i in 0 until categoriesTags.length()) {
                tags.add(categoriesTags.getString(i).lowercase())
            }
        }
        val allTags = tags.joinToString(" ")

        // Order matters: more specific categories first
        val mapping = listOf(
            "coffee" to "☕", "cafe" to "☕",
            "tea" to "🍵", "the" to "🍵",
            "juice" to "🧃", "jus" to "🧃",
            "beer" to "🍺", "biere" to "🍺",
            "wine" to "🍷", "vin" to "🍷",
            "water" to "💧", "eau" to "💧",
            "milk" to "🥛", "lait" to "🥛",
            "soda" to "🥤", "beverage" to "🥤", "boisson" to "🥤", "drink" to "🥤",
            "chocolate" to "🍫", "chocolat" to "🍫", "cacao" to "🍫",
            "candy" to "🍬", "bonbon" to "🍬", "confiserie" to "🍬",
            "ice-cream" to "🍦", "glace" to "🍦",
            "cake" to "🍰", "gateau" to "🍰", "patisserie" to "🍰",
            "biscuit" to "🍪", "cookie" to "🍪",
            "bread" to "🍞", "pain" to "🍞",
            "croissant" to "🥐", "viennoiserie" to "🥐",
            "pizza" to "🍕",
            "pasta" to "🍝", "pate" to "🍝", "noodle" to "🍜", "nouille" to "🍜",
            "rice" to "🍚", "riz" to "🍚",
            "cereal" to "🥣", "cereale" to "🥣",
            "soup" to "🥣", "soupe" to "🥣",
            "burger" to "🍔", "hamburger" to "🍔",
            "sandwich" to "🥪",
            "sushi" to "🍣",
            "salad" to "🥗", "salade" to "🥗",
            "egg" to "🥚", "oeuf" to "🥚",
            "cheese" to "🧀", "fromage" to "🧀",
            "yogurt" to "🍶", "yaourt" to "🍶",
            "butter" to "🧈", "beurre" to "🧈",
            "fish" to "🐟", "poisson" to "🐟", "tuna" to "🐟", "thon" to "🐟", "salmon" to "🐟", "saumon" to "🐟",
            "chicken" to "🍗", "poulet" to "🍗", "poultry" to "🍗", "volaille" to "🍗",
            "meat" to "🥩", "viande" to "🥩", "beef" to "🥩", "boeuf" to "🥩", "pork" to "🥩", "porc" to "🥩",
            "sausage" to "🌭", "saucisse" to "🌭", "hot-dog" to "🌭",
            "ham" to "🥓", "jambon" to "🥓",
            "fruit" to "🍎",
            "vegetable" to "🥬", "legume" to "🥬",
            "nut" to "🥜", "noix" to "🥜", "arachide" to "🥜", "peanut" to "🥜",
            "chip" to "🍿", "crisp" to "🍿", "snack" to "🍿",
            "sauce" to "🫙", "condiment" to "🫙", "ketchup" to "🫙", "mayonnaise" to "🫙",
            "oil" to "🫒", "huile" to "🫒",
            "honey" to "🍯", "miel" to "🍯",
            "jam" to "🫙", "confiture" to "🫙",
            "spice" to "🧂", "epice" to "🧂", "sel" to "🧂", "salt" to "🧂",
            "baby-food" to "🍼", "bebe" to "🍼",
            "frozen" to "🧊", "surgele" to "🧊",
            "canned" to "🥫", "conserve" to "🥫",
            "meal" to "🍽️", "plat" to "🍽️", "prepared" to "🍽️",
            "dairy" to "🧀", "laitier" to "🧀",
        )

        for ((keyword, emoji) in mapping) {
            if (keyword in allTags) return emoji
        }

        return "🍽️"
    }

    private fun parseHealthInfo(product: JSONObject): ProductHealthInfo? {
        val nutriScore = product.optString("nutriscore_grade", "").ifBlank {
            product.optString("nutrition_grades", "").ifBlank { null }
        }?.lowercase()?.takeIf { it in listOf("a", "b", "c", "d", "e") }

        val novaGroup = product.optInt("nova_group", 0).takeIf { it in 1..4 }

        val nutrientLevelsJson = product.optJSONObject("nutrient_levels")
        val nutrientLevels = if (nutrientLevelsJson != null) {
            NutrientLevels(
                fat = nutrientLevelsJson.optString("fat", "").ifBlank { null },
                saturatedFat = nutrientLevelsJson.optString("saturated-fat", "").ifBlank { null },
                sugars = nutrientLevelsJson.optString("sugars", "").ifBlank { null },
                salt = nutrientLevelsJson.optString("salt", "").ifBlank { null }
            ).takeIf { it.fat != null || it.saturatedFat != null || it.sugars != null || it.salt != null }
        } else null

        return if (nutriScore != null || novaGroup != null || nutrientLevels != null) {
            ProductHealthInfo(nutriScore, novaGroup, nutrientLevels)
        } else null
    }
}

class OpenFoodFactsException(message: String) : Exception(message)
class ProductNotFoundException(barcode: String) : Exception("Product not found: $barcode")
