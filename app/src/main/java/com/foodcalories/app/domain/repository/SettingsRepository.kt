package com.foodcalories.app.domain.repository

interface SettingsRepository {
    fun getApiKey(): String
    fun setApiKey(key: String)
}
