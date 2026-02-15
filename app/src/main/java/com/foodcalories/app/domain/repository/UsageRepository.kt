package com.foodcalories.app.domain.repository

interface UsageRepository {
    fun recordRequest()
    fun getDailyRequestCount(): Int
}
