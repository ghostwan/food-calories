package com.ghostwan.snapcal.domain.model

data class MealEntry(
    val id: Long = 0,
    val dishName: String,
    val calories: Int,
    val proteins: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float,
    val date: String, // yyyy-MM-dd
    val ingredientsJson: String,
    val emoji: String? = null
)
