package com.ghostwan.snapcal.domain.model

data class DailyNutrition(
    val date: String, // yyyy-MM-dd
    val totalCalories: Int,
    val totalProteins: Float,
    val totalCarbs: Float,
    val totalFats: Float,
    val totalFiber: Float
)
