package com.foodcalories.app.domain.repository

import com.foodcalories.app.domain.model.NutritionGoal
import com.foodcalories.app.domain.model.UserProfile

interface UserProfileRepository {
    fun getProfile(): UserProfile
    fun saveProfile(profile: UserProfile)
    fun getGoal(): NutritionGoal
    fun saveGoal(goal: NutritionGoal)
}
