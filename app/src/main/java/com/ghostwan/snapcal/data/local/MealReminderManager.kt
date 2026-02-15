package com.ghostwan.snapcal.data.local

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class MealReminderManager(private val context: Context) {

    enum class MealType(val key: String, val requestCode: Int) {
        BREAKFAST("breakfast", 0),
        LUNCH("lunch", 1),
        DINNER("dinner", 2)
    }

    private val prefs = context.getSharedPreferences("meal_reminders", Context.MODE_PRIVATE)

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Meal Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to log your meals"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) {
            prefs.edit().putBoolean("enabled", value).apply()
            if (value) scheduleAll() else cancelAll()
        }

    fun getHour(meal: MealType): Int = prefs.getInt("${meal.key}_hour", defaultHour(meal))
    fun getMinute(meal: MealType): Int = prefs.getInt("${meal.key}_minute", defaultMinute(meal))

    fun setTime(meal: MealType, hour: Int, minute: Int) {
        prefs.edit()
            .putInt("${meal.key}_hour", hour)
            .putInt("${meal.key}_minute", minute)
            .apply()
        if (enabled) schedule(meal)
    }

    fun scheduleAll() {
        MealType.entries.forEach { schedule(it) }
    }

    fun cancelAll() {
        MealType.entries.forEach { cancel(it) }
    }

    private fun schedule(meal: MealType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra(EXTRA_MEAL_TYPE, meal.key)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, meal.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, getHour(meal))
            set(Calendar.MINUTE, getMinute(meal))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }
    }

    private fun cancel(meal: MealType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, meal.requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun defaultHour(meal: MealType): Int = when (meal) {
        MealType.BREAKFAST -> 8
        MealType.LUNCH -> 12
        MealType.DINNER -> 20
    }

    private fun defaultMinute(meal: MealType): Int = when (meal) {
        MealType.BREAKFAST -> 0
        MealType.LUNCH -> 30
        MealType.DINNER -> 30
    }

    companion object {
        const val CHANNEL_ID = "meal_reminders"
        const val EXTRA_MEAL_TYPE = "meal_type"
    }
}
