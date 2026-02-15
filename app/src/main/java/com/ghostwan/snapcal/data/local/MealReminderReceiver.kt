package com.ghostwan.snapcal.data.local

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ghostwan.snapcal.MainActivity
import com.ghostwan.snapcal.R

class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mealKey = intent.getStringExtra(MealReminderManager.EXTRA_MEAL_TYPE) ?: return

        val mealType = MealReminderManager.MealType.entries.find { it.key == mealKey } ?: return

        val messageRes = when (mealType) {
            MealReminderManager.MealType.BREAKFAST -> R.string.reminder_breakfast_message
            MealReminderManager.MealType.LUNCH -> R.string.reminder_lunch_message
            MealReminderManager.MealType.DINNER -> R.string.reminder_dinner_message
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, mealType.requestCode, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MealReminderManager.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(messageRes))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(mealType.requestCode, notification)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS permission — ignore
        }

        // Re-schedule for tomorrow
        val manager = MealReminderManager(context)
        if (manager.enabled) {
            manager.setTime(mealType, manager.getHour(mealType), manager.getMinute(mealType))
        }
    }
}
