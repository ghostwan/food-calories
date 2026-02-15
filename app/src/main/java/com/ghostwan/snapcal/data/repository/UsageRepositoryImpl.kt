package com.ghostwan.snapcal.data.repository

import android.content.Context
import com.ghostwan.snapcal.domain.repository.UsageRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageRepositoryImpl(context: Context) : UsageRepository {

    private val prefs = context.getSharedPreferences("usage_tracking", Context.MODE_PRIVATE)

    override fun recordRequest() {
        val today = today()
        val storedDate = prefs.getString(KEY_DATE, "")

        if (storedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_DAILY_COUNT, 1)
                .apply()
        } else {
            val count = prefs.getInt(KEY_DAILY_COUNT, 0)
            prefs.edit()
                .putInt(KEY_DAILY_COUNT, count + 1)
                .apply()
        }
    }

    override fun getDailyRequestCount(): Int {
        val today = today()
        val storedDate = prefs.getString(KEY_DATE, "")
        return if (storedDate == today) prefs.getInt(KEY_DAILY_COUNT, 0) else 0
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private companion object {
        const val KEY_DATE = "last_request_date"
        const val KEY_DAILY_COUNT = "daily_request_count"
    }
}
