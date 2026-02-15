package com.ghostwan.snapcal.domain.repository

interface UsageRepository {
    fun recordRequest()
    fun getDailyRequestCount(): Int
}
