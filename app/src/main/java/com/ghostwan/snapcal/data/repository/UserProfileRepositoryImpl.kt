package com.ghostwan.snapcal.data.repository

import android.content.Context
import com.ghostwan.snapcal.data.local.WeightDao
import com.ghostwan.snapcal.data.local.WeightEntity
import com.ghostwan.snapcal.domain.model.ActivityLevel
import com.ghostwan.snapcal.domain.model.Gender
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UserProfileRepositoryImpl(
    context: Context,
    private val weightDao: WeightDao
) : UserProfileRepository {

    private val prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    override fun getProfile(): UserProfile {
        return UserProfile(
            height = prefs.getInt("height", 0),
            weight = prefs.getFloat("weight", 0f),
            targetWeight = prefs.getFloat("target_weight", 0f),
            age = prefs.getInt("age", 0),
            gender = Gender.valueOf(prefs.getString("gender", Gender.MALE.name)!!),
            activityLevel = ActivityLevel.valueOf(
                prefs.getString("activity_level", ActivityLevel.MODERATE.name)!!
            )
        )
    }

    override fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putInt("height", profile.height)
            .putFloat("weight", profile.weight)
            .putFloat("target_weight", profile.targetWeight)
            .putInt("age", profile.age)
            .putString("gender", profile.gender.name)
            .putString("activity_level", profile.activityLevel.name)
            .apply()
    }

    override fun getGoal(): NutritionGoal {
        return NutritionGoal(
            calories = prefs.getInt("goal_calories", 2000),
            proteins = prefs.getFloat("goal_proteins", 50f),
            carbs = prefs.getFloat("goal_carbs", 250f),
            fats = prefs.getFloat("goal_fats", 65f),
            fiber = prefs.getFloat("goal_fiber", 25f),
            explanation = prefs.getString("goal_explanation", null)
        )
    }

    override fun saveGoal(goal: NutritionGoal) {
        prefs.edit()
            .putInt("goal_calories", goal.calories)
            .putFloat("goal_proteins", goal.proteins)
            .putFloat("goal_carbs", goal.carbs)
            .putFloat("goal_fats", goal.fats)
            .putFloat("goal_fiber", goal.fiber)
            .putString("goal_explanation", goal.explanation)
            .apply()
    }

    override suspend fun saveWeightRecord(record: WeightRecord) {
        val existing = weightDao.getByDate(record.date)
        weightDao.insert(
            WeightEntity(
                id = existing?.id ?: 0,
                weight = record.weight,
                date = record.date
            )
        )
    }

    override suspend fun getLatestWeight(): WeightRecord? {
        return weightDao.getLatest()?.let {
            WeightRecord(id = it.id, weight = it.weight, date = it.date)
        }
    }

    override suspend fun getWeightHistory(days: Int): List<WeightRecord> {
        val startDate = daysAgoDate(days)
        return weightDao.getHistory(startDate).map {
            WeightRecord(id = it.id, weight = it.weight, date = it.date)
        }
    }

    private fun daysAgoDate(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
