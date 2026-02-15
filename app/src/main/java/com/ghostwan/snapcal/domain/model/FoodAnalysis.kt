package com.ghostwan.snapcal.domain.model

data class FoodAnalysis(
    val dishName: String,
    val totalCalories: Int,
    val ingredients: List<Ingredient>,
    val macros: Macros?,
    val notes: String?,
    val emoji: String? = null
)

data class Ingredient(
    val name: String,
    val quantity: String,
    val calories: Int
)

data class Macros(
    val proteins: String,
    val carbs: String,
    val fats: String,
    val fiber: String?
)
