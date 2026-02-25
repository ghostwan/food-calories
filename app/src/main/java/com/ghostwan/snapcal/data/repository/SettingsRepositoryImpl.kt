package com.ghostwan.snapcal.data.repository

import android.content.Context
import com.ghostwan.snapcal.BuildConfig
import com.ghostwan.snapcal.domain.repository.SettingsRepository

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences("food_calories_settings", Context.MODE_PRIVATE)

    override fun getApiKey(): String {
        val stored = prefs.getString(KEY_API, null)
        if (!stored.isNullOrBlank()) return stored
        return BuildConfig.GEMINI_API_KEY
    }

    override fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API, key).apply()
    }

    override fun isShoppingListEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOPPING_LIST, false)
    }

    override fun setShoppingListEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOPPING_LIST, enabled).apply()
    }

    override fun getBackupFrequencyDays(): Int {
        return prefs.getInt(KEY_BACKUP_FREQUENCY, 1)
    }

    override fun setBackupFrequencyDays(days: Int) {
        prefs.edit().putInt(KEY_BACKUP_FREQUENCY, days).apply()
    }

    override fun isDynamicCalorieGoalEnabled(): Boolean {
        return prefs.getBoolean(KEY_DYNAMIC_CALORIE_GOAL, false)
    }

    override fun setDynamicCalorieGoalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_CALORIE_GOAL, enabled).apply()
    }

    private companion object {
        const val KEY_API = "gemini_api_key_v2"
        const val KEY_SHOPPING_LIST = "shopping_list_enabled"
        const val KEY_BACKUP_FREQUENCY = "backup_frequency_days"
        const val KEY_DYNAMIC_CALORIE_GOAL = "dynamic_calorie_goal"
    }
}
