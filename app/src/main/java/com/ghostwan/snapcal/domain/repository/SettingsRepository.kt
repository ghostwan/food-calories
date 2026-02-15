package com.ghostwan.snapcal.domain.repository

interface SettingsRepository {
    fun getApiKey(): String
    fun setApiKey(key: String)
}
