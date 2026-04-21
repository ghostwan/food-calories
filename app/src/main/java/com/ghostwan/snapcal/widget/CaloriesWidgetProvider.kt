package com.ghostwan.snapcal.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.widget.RemoteViews
import com.ghostwan.snapcal.MainActivity
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.data.local.AppDatabase
import com.ghostwan.snapcal.data.local.HealthConnectManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class CaloriesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, CaloriesWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val dao = AppDatabase.getInstance(context).mealDao()
            val consumed = runBlocking { dao.getDailyCalories(today) } ?: 0

            val prefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            var goal = prefs.getInt("goal_calories", 2000)

            val settingsPrefs = context.getSharedPreferences("food_calories_settings", Context.MODE_PRIVATE)
            val dynamicGoalEnabled = settingsPrefs.getBoolean("dynamic_calorie_goal", false)
            if (dynamicGoalEnabled) {
                try {
                    val hcManager = HealthConnectManager(context)
                    if (hcManager.isAvailable()) {
                        val burned = runBlocking {
                            if (hcManager.hasPermissions()) {
                                val todayDate = LocalDate.now()
                                val map = hcManager.readCaloriesBurnedForDateRange(todayDate, todayDate)
                                map.values.firstOrNull()?.toInt() ?: 0
                            } else 0
                        }
                        if (burned > 0) {
                            val deficit = settingsPrefs.getInt("daily_calorie_deficit", 500)
                            goal = (burned - deficit).coerceAtLeast(0)
                        }
                    }
                } catch (_: Exception) { }
            }

            val remaining = (goal - consumed).coerceAtLeast(0)
            val progress = if (goal > 0) ((consumed * 100) / goal).coerceIn(0, 100) else 0

            val progressFraction = if (goal > 0) consumed.toFloat() / goal else 0f
            val progressColor = when {
                progressFraction <= 0.9f -> 0xFF4CAF50.toInt() // green
                progressFraction <= 1.0f  -> 0xFFFF9800.toInt() // orange
                else                      -> 0xFFF44336.toInt() // red
            }

            val views = RemoteViews(context.packageName, R.layout.widget_calories)
            views.setTextViewText(R.id.widget_calories_text, "$consumed / $goal kcal")
            views.setProgressBar(R.id.widget_progress, 100, progress, false)
            views.setColorStateList(R.id.widget_progress, "setProgressTintList", ColorStateList.valueOf(progressColor))
            views.setTextViewText(
                R.id.widget_remaining_text,
                context.getString(R.string.widget_remaining, remaining)
            )

            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra("navigate_to", "home")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                data = Uri.parse("snapcal://widget/calories")
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
