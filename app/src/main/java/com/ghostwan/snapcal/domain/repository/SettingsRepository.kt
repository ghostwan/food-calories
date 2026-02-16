package com.ghostwan.snapcal.domain.repository

interface SettingsRepository {
    fun getApiKey(): String
    fun setApiKey(key: String)
    fun isShoppingListEnabled(): Boolean
    fun setShoppingListEnabled(enabled: Boolean)
}
