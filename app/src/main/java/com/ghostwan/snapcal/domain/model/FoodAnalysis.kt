package com.ghostwan.snapcal.domain.model

data class FoodAnalysis(
    val dishName: String,
    val totalCalories: Int,
    val ingredients: List<Ingredient>,
    val macros: Macros?,
    val notes: String?,
    val emoji: String? = null,
    val healthInfo: ProductHealthInfo? = null
)

data class Ingredient(
    val name: String,
    val quantity: String,
    val calories: Int,
    val healthRating: String? = null
)

data class Macros(
    val proteins: String,
    val carbs: String,
    val fats: String,
    val fiber: String?
)

data class ProductHealthInfo(
    val nutriScore: String?,
    val novaGroup: Int?,
    val nutrientLevels: NutrientLevels?
)

data class NutrientLevels(
    val fat: String?,
    val saturatedFat: String?,
    val sugars: String?,
    val salt: String?
)
