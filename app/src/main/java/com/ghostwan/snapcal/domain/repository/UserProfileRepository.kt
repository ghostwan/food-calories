package com.ghostwan.snapcal.domain.repository

import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord

interface UserProfileRepository {
    fun getProfile(): UserProfile
    fun saveProfile(profile: UserProfile)
    fun getGoal(): NutritionGoal
    fun saveGoal(goal: NutritionGoal)
    suspend fun saveWeightRecord(record: WeightRecord)
    suspend fun getLatestWeight(): WeightRecord?
    suspend fun getWeightHistory(days: Int = 90): List<WeightRecord>
    fun getChartCaloriesOrigin(): Int
    fun setChartCaloriesOrigin(value: Int)
    fun getChartWeightOrigin(): Int
    fun setChartWeightOrigin(value: Int)
    fun getChartShowCalories(): Boolean
    fun setChartShowCalories(value: Boolean)
    fun getChartShowWeight(): Boolean
    fun setChartShowWeight(value: Boolean)
    fun getChartShowBurned(): Boolean
    fun setChartShowBurned(value: Boolean)
    fun getChartRange(): Int
    fun setChartRange(value: Int)
}
