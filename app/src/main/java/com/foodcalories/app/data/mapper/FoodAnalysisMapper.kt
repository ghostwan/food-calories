package com.foodcalories.app.data.mapper

import com.foodcalories.app.domain.model.FoodAnalysis
import com.foodcalories.app.domain.model.Ingredient
import com.foodcalories.app.domain.model.Macros
import org.json.JSONObject

class FoodAnalysisMapper {

    fun mapFromApiResponse(rawResponse: String): FoodAnalysis {
        val json = JSONObject(rawResponse)
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() == 0) {
            throw IllegalStateException("Aucune réponse de l'API")
        }

        val textContent = candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")

        val cleanJson = textContent
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val result = JSONObject(cleanJson)

        val ingredients = buildList {
            val array = result.getJSONArray("ingredients")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                add(
                    Ingredient(
                        name = obj.getString("name"),
                        quantity = obj.getString("quantity"),
                        calories = obj.getInt("calories")
                    )
                )
            }
        }

        val macros = if (result.isNull("macros")) {
            null
        } else {
            val m = result.getJSONObject("macros")
            Macros(
                proteins = m.getString("proteins"),
                carbs = m.getString("carbs"),
                fats = m.getString("fats"),
                fiber = if (m.has("fiber") && !m.isNull("fiber")) m.getString("fiber") else null
            )
        }

        return FoodAnalysis(
            dishName = result.getString("dishName"),
            totalCalories = result.getInt("totalCalories"),
            ingredients = ingredients,
            macros = macros,
            notes = if (result.has("notes") && !result.isNull("notes")) result.getString("notes") else null
        )
    }
}
