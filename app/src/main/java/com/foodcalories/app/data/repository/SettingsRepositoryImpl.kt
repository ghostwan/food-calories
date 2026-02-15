package com.foodcalories.app.data.repository

import android.content.Context
import com.foodcalories.app.BuildConfig
import com.foodcalories.app.domain.repository.SettingsRepository

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

    private companion object {
        const val KEY_API = "gemini_api_key"
    }
}
