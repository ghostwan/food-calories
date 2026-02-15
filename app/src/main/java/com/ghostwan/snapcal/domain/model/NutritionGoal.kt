package com.ghostwan.snapcal.domain.model

data class NutritionGoal(
    val calories: Int = 2000,
    val proteins: Float = 50f,
    val carbs: Float = 250f,
    val fats: Float = 65f,
    val fiber: Float = 25f,
    val explanation: String? = null
)
