package com.foodcalories.app.domain.repository

import com.foodcalories.app.domain.model.NutritionGoal
import com.foodcalories.app.domain.model.UserProfile
import com.foodcalories.app.domain.model.WeightRecord

interface UserProfileRepository {
    fun getProfile(): UserProfile
    fun saveProfile(profile: UserProfile)
    fun getGoal(): NutritionGoal
    fun saveGoal(goal: NutritionGoal)
    suspend fun saveWeightRecord(record: WeightRecord)
    suspend fun getLatestWeight(): WeightRecord?
    suspend fun getWeightHistory(days: Int = 90): List<WeightRecord>
}
