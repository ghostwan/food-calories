package com.ghostwan.snapcal.domain.repository

interface SettingsRepository {
    fun getApiKey(): String
    fun setApiKey(key: String)
    fun isShoppingListEnabled(): Boolean
    fun setShoppingListEnabled(enabled: Boolean)
    fun getBackupFrequencyDays(): Int
    fun setBackupFrequencyDays(days: Int)
    fun isDynamicCalorieGoalEnabled(): Boolean
    fun setDynamicCalorieGoalEnabled(enabled: Boolean)
    fun getDailyCalorieDeficit(): Int
    fun setDailyCalorieDeficit(deficit: Int)
}
