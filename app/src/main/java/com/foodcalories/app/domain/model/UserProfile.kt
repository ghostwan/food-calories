package com.foodcalories.app.domain.model

data class UserProfile(
    val height: Int = 0, // cm
    val weight: Float = 0f, // kg
    val targetWeight: Float = 0f, // kg
    val age: Int = 0,
    val gender: Gender = Gender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE
)

enum class Gender { MALE, FEMALE }

enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }
